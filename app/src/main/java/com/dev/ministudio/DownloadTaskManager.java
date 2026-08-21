package com.dev.ministudio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import java.io.File;

public class DownloadTaskManager {

    public interface DownloadListener {
        void onDownloadLog(String text, int color);
        void onDownloadFinished(boolean success, File apkFile);
    }

    private final Context context;
    private final String projectName;
    private final DownloadListener listener;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final int COLOR_INFO = Color.parseColor("#4FC3F7");
    private final int COLOR_SUCCESS = Color.parseColor("#81C784");
    private final int COLOR_ERROR = Color.parseColor("#FF8A80");
    private final int COLOR_WARNING = Color.parseColor("#FFB74D");
    private BroadcastReceiver downloadReceiver;

    public DownloadTaskManager(Context context, String projectName, DownloadListener listener) {
        this.context = context;
        this.projectName = projectName;
        this.listener = listener;
    }

    public void startFetchAndInstall() {
        // 1. ลงทะเบียน BroadcastReceiver เพื่อรอรับ Log และผลลัพธ์จาก Foreground Service
        registerReceiver();

        // 2. สั่งเริ่ม Foreground Service
        Intent serviceIntent = new Intent(context, DownloadApkService.class);
        serviceIntent.putExtra("project_name", projectName);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            sendProgress("❌ ไม่สามารถเริ่มบริการดาวน์โหลดได้: " + e.getMessage(), COLOR_ERROR);
            unregisterReceiver();
            if (listener != null) {
                listener.onDownloadFinished(false, null);
            }
        }
    }

    private void registerReceiver() {
        if (downloadReceiver != null) return; // ป้องกันการลงทะเบียนซ้ำ

        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) return;

                String action = intent.getAction();

                if (DownloadApkService.ACTION_DOWNLOAD_PROGRESS.equals(action)) {
                    String logText = intent.getStringExtra(DownloadApkService.EXTRA_LOG_TEXT);
                    if (logText != null) {
                        int color = parseLogColor(logText);
                        sendProgress(logText, color);
                    }
                } else if (DownloadApkService.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    boolean success = intent.getBooleanExtra(DownloadApkService.EXTRA_SUCCESS, false);
                    String apkPath = intent.getStringExtra(DownloadApkService.EXTRA_APK_PATH);
                    File apkFile = apkPath != null ? new File(apkPath) : null;

                    uiHandler.post(() -> {
                        if (listener != null) {
                            listener.onDownloadFinished(success, apkFile);
                        }
                    });

                    // ยกเลิกการดักจับสัญญาณเมื่อทำงานเสร็จสิ้น
                    unregisterReceiver();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadApkService.ACTION_DOWNLOAD_PROGRESS);
        filter.addAction(DownloadApkService.ACTION_DOWNLOAD_COMPLETE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(downloadReceiver, filter);
        }
    }

    public void unregisterReceiver() {
        if (downloadReceiver != null) {
            try {
                context.unregisterReceiver(downloadReceiver);
            } catch (Exception ignored) {
                // เคลียร์เมื่อ Receiver ถูกยกเลิกไปก่อนหน้าแล้ว
            }
            downloadReceiver = null;
        }
    }

    private int parseLogColor(String logText) {
        if (logText.startsWith("❌")) return COLOR_ERROR;
        if (logText.startsWith("🎉")) return COLOR_SUCCESS;
        if (logText.startsWith("⏳") || logText.startsWith("⚠️")) return COLOR_WARNING;
        return COLOR_INFO;
    }

    private void sendProgress(final String text, final int color) {
        uiHandler.post(() -> {
            if (listener != null) {
                listener.onDownloadLog(text, color);
            }
        });
    }
}
