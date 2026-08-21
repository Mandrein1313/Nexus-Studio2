package com.dev.ministudio.utils;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * อ่าน logcat แบบสตรีม
 * บนเครื่องทั่วไปมักเห็น log ของแอปตัวเอง + ระบบบางส่วน
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
            BufferedReader reader = null;
            try {
                // ไม่บังคับ clear — บางเครื่องไม่มีสิทธิ์
                try {
                    Process clear = Runtime.getRuntime().exec(new String[]{"logcat", "-c"});
                    clear.waitFor();
                } catch (Exception ignored) {
                }

                // *:E = Error ขึ้นไป (น้อย noise กว่า *:W)
                // อยากเห็น Warning ด้วย เปลี่ยนเป็น "*:W"
                ProcessBuilder pb = new ProcessBuilder(
                        "logcat",
                        "-v", "threadtime",
                        "*:E",
                        "AndroidRuntime:E"
                );
                pb.redirectErrorStream(true);
                process = pb.start();

                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while (running.get() && (line = reader.readLine()) != null) {
                    if (packageFilter != null && !packageFilter.isEmpty()) {
                        if (!line.contains(packageFilter)
                                && !line.contains("AndroidRuntime")
                                && !line.contains("FATAL EXCEPTION")
                                && !line.contains("Process:")) {
                            continue;
                        }
                    }
                    // ข้าม junk ที่ไม่ช่วย debug
                    if (isNoise(line)) continue;

                    final String out = line;
                    main.post(() -> {
                        if (listener != null) listener.onLine(out);
                    });
                }
            } catch (Exception e) {
                // ตอนกด Stop มักขึ้น closed / interrupted — ไม่ต้องโชว์แดง
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

    private static boolean isStopNoise(String msg) {
        String m = msg.toLowerCase();
        return m.contains("closed")
                || m.contains("interrupt")
                || m.contains("stream closed")
                || m.contains("pipe")
                || m.contains("broken pipe");
    }

    private static boolean isNoise(String line) {
        return line.contains("updateBlastSurfaceIfNeeded")
                || line.contains("handleResized abandoned")
                || line.contains("BLASTBufferQueue");
    }
}