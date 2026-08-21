package com.dev.ministudio.utils; 

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * อ่าน logcat แบบสตรีม — บนเครื่องทั่วไปมักเห็น log ของแอปตัวเองเป็นหลัก
 */
public class LogcatReader {

    public interface Listener {
        void onLine(String line);
        void onError(String message);
        void onStopped();
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Process process;
    private Thread worker;

    public void start(String packageFilter, Listener listener) {
        stop();
        running.set(true);

        worker = new Thread(() -> {
            try {
                // ล้าง buffer เก่า (บางเครื่องต้องใช้สิทธิ์)
                try {
                    Runtime.getRuntime().exec(new String[]{"logcat", "-c"}).waitFor();
                } catch (Exception ignored) {
                }

                // รูปแบบอ่านง่าย + กรองระดับ Warning ขึ้นไป
                ProcessBuilder pb = new ProcessBuilder(
                        "logcat",
                        "-v", "threadtime",
                        "*:W"
                );
                pb.redirectErrorStream(true);
                process = pb.start();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                while (running.get() && (line = reader.readLine()) != null) {
                    if (packageFilter != null && !packageFilter.isEmpty()) {
                        // กรองตาม package / tag สำคัญ
                        if (!line.contains(packageFilter)
                                && !line.contains("AndroidRuntime")
                                && !line.contains("FATAL EXCEPTION")) {
                            continue;
                        }
                    }
                    final String out = line;
                    main.post(() -> {
                        if (listener != null) listener.onLine(out);
                    });
                }
            } catch (Exception e) {
                main.post(() -> {
                    if (listener != null) {
                        listener.onError("Logcat: " + e.getMessage());
                    }
                });
            } finally {
                running.set(false);
                main.post(() -> {
                    if (listener != null) listener.onStopped();
                });
            }
        }, "LogcatReader");
        worker.start();
    }

    public void stop() {
        running.set(false);
        if (process != null) {
            process.destroy();
            process = null;
        }
        if (worker != null) {
            try {
                worker.join(500);
            } catch (InterruptedException ignored) {
            }
            worker = null;
        }
    }

    public boolean isRunning() {
        return running.get();
    }
}
