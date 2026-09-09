package com.dev.ministudio;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import java.io.File;

public class GitHubPushService extends Service {
    private static final String CHANNEL_ID = "github_push_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int NOTIFICATION_FINAL_ID = 1002;
    public static final String ACTION_CANCEL = "com.dev.ministudio.ACTION_CANCEL_PUSH";

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    
    // 🌟 ตัวแปรเช็กสถานะการยกเลิก
    private volatile boolean isCancelled = false;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 🛑 ดักจับเมื่อผู้ใช้กดปุ่ม "ยกเลิก" จาก Notification
        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            isCancelled = true;
            showFinalNotification("🚫 ยกเลิกการอัปโหลดเรียบร้อยแล้ว", true);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        String projectName = intent.getStringExtra("projectName");
        String username = intent.getStringExtra("username");
        String token = intent.getStringExtra("token");
        String repoUrl = intent.getStringExtra("repoUrl");

        isCancelled = false;

        // 🔘 สร้าง Intent สำหรับปุ่มยกเลิก
        Intent cancelIntent = new Intent(this, GitHubPushService.class);
        cancelIntent.setAction(ACTION_CANCEL);
        PendingIntent pendingCancelIntent = PendingIntent.getService(
                this, 0, cancelIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // 🌟 ตั้งค่า Notification พร้อมเพิ่มปุ่ม "ยกเลิก"
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🚀 MiniStudio: " + (projectName != null ? projectName : "Pushing..."))
                .setContentText("กำลังเริ่มระบบ... 0%")
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "ยกเลิก", pendingCancelIntent); // << ปุ่มยกเลิก

        startForeground(NOTIFICATION_ID, notificationBuilder.build());

        new Thread(() -> {
            File projectDir = new File("/sdcard/MiniStudio/" + projectName);
            try {
                if (isCancelled) return;

                Git git;
                updateStatus("[1/4] ตรวจสอบระบบ Git...", 0, true);
                if (!new File(projectDir, ".git").exists()) {
                    git = Git.init().setDirectory(projectDir).call();
                } else {
                    git = Git.open(projectDir);
                }

                if (isCancelled) return;

                updateStatus("[2/4] รวบรวมไฟล์ทั้งหมด...", 0, true);
                git.add().addFilepattern(".").call();

                if (isCancelled) return;

                updateStatus("[3/4] บันทึกประวัติ (Commit)...", 0, true);
                try {
                    git.commit().setMessage("Updated via MiniStudio - " + new java.util.Date()).call();
                } catch (Exception ce) {
                    // กรณีไม่มีไฟล์เปลี่ยนแปลง
                }

                if (isCancelled) return;

                git.getRepository().getConfig().setString("remote", "origin", "url", repoUrl);
                git.getRepository().getConfig().save();

                // 🌟 [4/4] Push พร้อมระบบเช็กการยกเลิกเรียลไทม์
                git.push()
                   .setRemote(repoUrl)
                   .setPushAll()
                   .setProgressMonitor(new ProgressMonitor() {
                       private int totalTasks = 0;
                       private int completed = 0;

                       @Override
                       public void start(int totalTasks) {}

                       @Override
                       public void beginTask(String title, int totalWork) {
                           this.totalTasks = totalWork;
                           this.completed = 0;
                           if (!isCancelled) {
                               updateProgress("[4/4] " + title + " 0%", 0);
                           }
                       }

                       @Override
                       public void update(int completedWork) {
                           this.completed += completedWork;
                           if (totalTasks > 0 && !isCancelled) {
                               int percent = (int) ((completed / (float) totalTasks) * 100);
                               updateProgress("[4/4] กำลังอัปโหลด... " + percent + "%", percent);
                           }
                       }

                       @Override
                       public void endTask() {}

                       // 🛑 ส่งค่าสถานะยกเลิกไปยัง JGit เพื่อสั่งหยุดส่งข้อมูลทันที!
                       @Override
                       public boolean isCancelled() {
                           return isCancelled;
                       }

                       @Override
                       public void showDuration(boolean enabled) {}
                   })
                   .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                   .call();

                if (!isCancelled) {
                    showFinalNotification("🎉 อัปโหลดขึ้น GitHub สำเร็จแล้วครับ!", false);
                }

            } catch (Exception e) {
                if (!isCancelled) {
                    e.printStackTrace();
                    showFinalNotification("❌ ล้มเหลว: " + e.getMessage(), true);
                }
            } finally {
                stopForeground(true);
                stopSelf();
            }
        }).start();

        return START_NOT_STICKY;
    }

    private void updateStatus(String text, int progress, boolean indeterminate) {
        if (isCancelled) return;
        notificationBuilder.setContentText(text)
                           .setProgress(100, progress, indeterminate);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void updateProgress(String text, int percent) {
        if (isCancelled) return;
        notificationBuilder.setContentText(text)
                           .setProgress(100, percent, false);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void showFinalNotification(String resultText, boolean isError) {
        NotificationCompat.Builder finalBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(isError ? "❌ MiniStudio: Push ล้มเหลว" : "✅ MiniStudio: Push สำเร็จ")
                .setContentText(resultText)
                .setSmallIcon(isError ? android.R.drawable.stat_notify_error : android.R.drawable.stat_sys_upload_done)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setAutoCancel(true)
                .setProgress(0, 0, false);

        notificationManager.notify(NOTIFICATION_FINAL_ID, finalBuilder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "GitHub Sync Progress",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("แสดงความคืบหน้าการ Push โค้ดขึ้น GitHub");
            channel.setSound(null, null);
            channel.enableVibration(false);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
