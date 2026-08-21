package com.dev.ministudio.utils;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * โหมดจับ crash: เงียบเมื่อไม่มี error / ชัดเมื่อมี FATAL + stack
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

    /** หลังเจอ FATAL แล้ว รับ stack ต่ออีกกี่บรรทัด */
    private int stackLinesRemaining = 0;

    public void start(String packageFilter, Listener listener) {
        stop();
        running.set(true);
        stackLinesRemaining = 0;

        worker = new Thread(() -> {
            BufferedReader reader = null;
            try {
                try {
                    Runtime.getRuntime().exec(new String[]{"logcat", "-c"}).waitFor();
                } catch (Exception ignored) {
                }

                // ปิด noise ส่วนใหญ่ เหลือ error / fatal / runtime
                ProcessBuilder pb = new ProcessBuilder(
                        "logcat",
                        "-v", "threadtime",
                        "*:S",
                        "AndroidRuntime:E",
                        "System.err:W",
                        "ActivityManager:E",
                        "DEBUG:E",
                        "*:F"
                );
                pb.redirectErrorStream(true);
                process = pb.start();

                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while (running.get() && (line = reader.readLine()) != null) {
                    if (!shouldShow(line, packageFilter)) continue;

                    final String out = line;
                    main.post(() -> {
                        if (listener != null) listener.onLine(out);
                    });
                }
            } catch (Exception e) {
                if (running.get()) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (!isStopNoise(msg)) {
                        main.post(() -> {
                            if (listener != null) listener.onError("Logcat: " + msg);
                        });
                    }
                }
            } finally {
                running.set(false);
                try {
                    if (reader != null) reader.close();
                } catch (Exception ignored) {
                }
                if (process != null) {
                    process.destroy();
                    process = null;
                }
                main.post(() -> {
                    if (listener != null) listener.onStopped();
                });
            }
        }, "LogcatReader");
        worker.start();
    }

    public void stop() {
        running.set(false);
        stackLinesRemaining = 0;
        if (process != null) {
            try {
                process.destroy();
            } catch (Exception ignored) {
            }
            process = null;
        }
        if (worker != null) {
            try {
                worker.join(400);
            } catch (InterruptedException ignored) {
            }
            worker = null;
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private boolean shouldShow(String line, String packageFilter) {
        if (line == null || line.isEmpty()) return false;

        // junk กราฟิก
        if (line.contains("updateBlastSurfaceIfNeeded")
                || line.contains("handleResized abandoned")
                || line.contains("BLASTBufferQueue")
                || line.contains("VRI[")) {
            return false;
        }

        // ต่อ stack หลัง FATAL
        if (stackLinesRemaining > 0) {
            if (line.contains("\tat ")
                    || line.contains("Caused by:")
                    || line.contains("java.")
                    || line.contains("android.")
                    || line.trim().startsWith("...")) {
                stackLinesRemaining--;
                return true;
            }
            // บรรทัดว่างใน stack ยังรับได้
            if (line.trim().isEmpty()) return true;
            stackLinesRemaining = 0;
        }

        boolean isFatal = line.contains("FATAL EXCEPTION")
                || line.contains("Fatal signal");
        boolean isRuntime = line.contains("AndroidRuntime");
        boolean isProcess = line.contains("Process:") && line.contains("PID:");
        boolean isJavaCrash = line.contains("java.lang.")
                && (line.contains("Exception") || line.contains("Error"));
        boolean isLevelEorF = line.contains(" E/")
                || line.contains(" F/")
                || line.matches(".*\\sE\\s+\\S+\\s*:.*")
                || line.matches(".*\\sF\\s+\\S+\\s*:.*");

        if (isFatal) {
            stackLinesRemaining = 40; // เก็บ stack ตามมา
            return true;
        }
        if (isRuntime || isProcess || isJavaCrash || isLevelEorF) {
            if (packageFilter != null && !packageFilter.isEmpty()) {
                // ช่วง crash ไม่กรองแน่นเกินไป กันพลาด stack
                if (isRuntime || isFatal || isProcess || isJavaCrash) return true;
                return line.contains(packageFilter);
            }
            return true;
        }
        return false;
    }

    private static boolean isStopNoise(String msg) {
        String m = msg.toLowerCase();
        return m.contains("closed")
                || m.contains("interrupt")
                || m.contains("stream closed")
                || m.contains("broken pipe");
    }
}