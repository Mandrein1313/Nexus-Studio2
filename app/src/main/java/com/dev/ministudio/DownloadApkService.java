package com.dev.ministudio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadApkService extends Service {

    public static final String ACTION_DOWNLOAD_PROGRESS = "com.dev.ministudio.ACTION_DOWNLOAD_PROGRESS";
    public static final String ACTION_DOWNLOAD_COMPLETE = "com.dev.ministudio.ACTION_DOWNLOAD_COMPLETE";
    
    public static final String EXTRA_LOG_TEXT = "log_text";
    public static final String EXTRA_SUCCESS = "success";
    public static final String EXTRA_APK_PATH = "apk_path";

    private static final String CHANNEL_ID = "DownloadApkChannel";
    private static final int NOTIFICATION_ID = 2002;

    private volatile boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && !isRunning) {
            isRunning = true;
            String projectName = intent.getStringExtra("project_name");

            // เริ่มรัน Foreground Service พร้อมแสดง Notification ตั้งต้น
            startForeground(NOTIFICATION_ID, createNotification("กำลังเตรียมดาวน์โหลด APK...", 0));

            // ทำงานบน Background Thread
            new Thread(() -> startFetchAndInstall(projectName)).start();
        }
        return START_NOT_STICKY;
    }

    private void startFetchAndInstall(String projectName) {
        HttpURLConnection conn = null;
        HttpURLConnection dlConn = null;
        try {
            SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
            String username = prefs.getString("username", "").trim();
            String token = prefs.getString("token", "").trim();

            if (username.isEmpty() || token.isEmpty()) {
                sendProgress("❌ กรุณาตั้งค่า GitHub Username + Token ก่อน");
                endDownload(false, null);
                return;
            }

            sendProgress("🔍 กำลังดึงข้อมูลบิวด์ล่าสุดจากคลาวด์...");

            String releasesUrl = "https://api.github.com/repos/" + username + "/" + projectName + "/releases?per_page=5";
            
            conn = (HttpURLConnection) new URL(releasesUrl).openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("Cache-Control", "no-cache, no-store");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            if (conn.getResponseCode() != 200) {
                sendProgress("❌ ไม่สามารถเข้าถึงข้อมูลคลาวด์บิวด์ได้ (HTTP " + conn.getResponseCode() + ")");
                endDownload(false, null);
                return;
            }

            JSONArray releases = new JSONArray(readStream(conn.getInputStream()));
            if (releases.length() == 0) {
                sendProgress("⏳ ไม่พบประวัติการบิวด์บนระบบคลาวด์");
                endDownload(false, null);
                return;
            }

            JSONObject latestRelease = releases.getJSONObject(0);
            JSONArray assets = latestRelease.getJSONArray("assets");

            String downloadUrl = null;
            String apkName = "app-debug.apk";

            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                String name = asset.getString("name");
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url");
                    apkName = name;
                    break;
                }
            }

            if (downloadUrl == null) {
                sendProgress("⏳ พบสถานะบิวด์สำเร็จ แต่ไม่พบไฟล์ APK สำหรับติดตั้ง");
                endDownload(false, null);
                return;
            }

            sendProgress("📥 ตรวจพบไฟล์: " + apkName + " (กำลังเตรียมดาวน์โหลด...)");

            dlConn = (HttpURLConnection) new URL(downloadUrl).openConnection();
            dlConn.setRequestProperty("Authorization", "Bearer " + token);
            dlConn.setRequestProperty("Cache-Control", "no-cache");
            dlConn.setConnectTimeout(15000);
            dlConn.setReadTimeout(15000);

            // จัดการระบบ Redirect (HTTP 302/301) ไปยัง AWS S3
            int status = dlConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM) {
                String redirectedUrl = dlConn.getHeaderField("Location");
                dlConn.disconnect();
                
                dlConn = (HttpURLConnection) new URL(redirectedUrl).openConnection();
                dlConn.setConnectTimeout(15000);
                dlConn.setReadTimeout(15000);
            }

            File outputDir = new File(getExternalFilesDir(null), "build_output");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File apkFile = new File(outputDir, apkName);

            try (InputStream in = new BufferedInputStream(dlConn.getInputStream());
                 FileOutputStream fos = new FileOutputStream(apkFile)) {

                byte[] data = new byte[8192];
                long total = 0;
                int count;
                int fileLength = Math.max(dlConn.getContentLength(), 1);
                int lastProgress = -1;

                while ((count = in.read(data)) != -1) {
                    total += count;
                    fos.write(data, 0, count);

                    int progress = (int) (total * 100 / fileLength);
                    
                    if (progress != lastProgress && progress % 5 == 0) {
                        String msg = "📥 กำลังดาวน์โหลดไฟล์ APK... " + progress + "%";
                        sendProgress(msg);
                        updateNotification(msg, progress);
                        lastProgress = progress;
                    }
                }
            }

            if (apkFile.exists() && apkFile.length() > 50000) {
                sendProgress("🎉 ดาวน์โหลดสำเร็จเรียบร้อย! กำลังเรียกตัวติดตั้งระบบ...");
                updateNotification("ดาวน์โหลดสำเร็จ! กำลังติดตั้ง...", 100);
                endDownload(true, apkFile);
                installApk(apkFile);
            } else {
                sendProgress("⚠️ ตรวจพบความเสียหาย: ไฟล์ APK มีขนาดเล็กผิดปกติ (" + apkFile.length() + " bytes)");
                endDownload(false, null);
            }

        } catch (Exception e) {
            sendProgress("❌ เกิดข้อผิดพลาดขณะดาวน์โหลด: " + e.getMessage());
            e.printStackTrace();
            endDownload(false, null);
        } finally {
            if (conn != null) { try { conn.disconnect(); } catch (Exception ignored) {} }
            if (dlConn != null) { try { dlConn.disconnect(); } catch (Exception ignored) {} }
        }
    }

    private String readStream(InputStream in) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } finally {
            try { in.close(); } catch (Exception ignored) {}
        }
    }

    private void installApk(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            sendProgress("❌ ไม่สามารถเปิดหน้าต่างติดตั้งแอปพลิเคชันได้: " + e.getMessage());
        }
    }

    private void sendProgress(String text) {
        Intent intent = new Intent(ACTION_DOWNLOAD_PROGRESS);
        intent.putExtra(EXTRA_LOG_TEXT, text);
        sendBroadcast(intent);
    }

private void endDownload(boolean success, File apkFile) {
    Intent intent = new Intent(ACTION_DOWNLOAD_COMPLETE);
    intent.putExtra(EXTRA_SUCCESS, success);
    if (apkFile != null) {
        intent.putExtra(EXTRA_APK_PATH, apkFile.getAbsolutePath());
    }
    sendBroadcast(intent);

    // 🌟 เรียกฟังก์ชันแสดงแจ้งเตือนแบบค้างไว้หลังดาวน์โหลดเสร็จ
    showFinalNotification(apkFile, success ? "ดาวน์โหลด APK สำเร็จแล้ว แตะเพื่อติดตั้ง" : "ดาวน์โหลด APK ล้มเหลว", !success);

    isRunning = false;
}

private void showFinalNotification(File apkFile, String resultText, boolean isError) {
    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    NotificationCompat.Builder finalBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(isError ? "❌ MiniStudio: ดาวน์โหลดล้มเหลว" : "✅ MiniStudio: ดาวน์โหลด APK เสร็จแล้ว")
            .setContentText(resultText)
            .setSmallIcon(isError ? android.R.drawable.stat_notify_error : android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // ดันความสำคัญเพื่อให้แจ้งเตือนเด่นชัด
            .setOngoing(false) // ให้ผู้ใช้ปัดทิ้งเองได้
            .setAutoCancel(true) // แตะแล้วหายไปเองอัตโนมัติ
            .setProgress(0, 0, false);

    // ถ้าดาวน์โหลดสำเร็จ ผูก Intent เปิดหน้าติดตั้ง APK ไว้เมื่อกดที่ Notification
    if (!isError && apkFile != null && apkFile.exists()) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
        finalBuilder.setContentIntent(pendingIntent);
    }

    // 🌟 ปลดสถานะ Foreground โดยคงการแจ้งเตือนนี้ไว้บน Status Bar
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        stopForeground(STOP_FOREGROUND_DETACH);
    } else {
        stopForeground(false);
    }

    // แสดงแจ้งเตือนตัวสุดท้ายค้างไว้
    if (notificationManager != null) {
        notificationManager.notify(NOTIFICATION_ID, finalBuilder.build());
    }

    stopSelf(); // ปิดการทำงาน Service เบื้องหลัง
}

private void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Download APK Service",
                NotificationManager.IMPORTANCE_DEFAULT // 👈 ดันขึ้น Status Bar ชัดเจน
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
    private Notification createNotification(String message, int progress) {
        Intent notificationIntent = new Intent(this, ProjectListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MiniStudio ดาวน์โหลด APK")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (progress > 0) {
            builder.setProgress(100, progress, false);
        } else {
            builder.setProgress(0, 0, true);
        }

        return builder.build();
    }

    private void updateNotification(String message, int progress) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(message, progress));
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
