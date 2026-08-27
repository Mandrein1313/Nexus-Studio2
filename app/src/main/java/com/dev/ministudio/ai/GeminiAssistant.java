package com.dev.ministudio.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Nexus AI — เรียก Groq API (OpenAI-compatible)
 * รองรับ system prompt, ประวัติแชท, temperature, max_tokens
 */
public class GeminiAssistant {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    /** โมเดล default บน free/developer tier (llama-3.3-70b ถูก deprecate แล้ว) */
    private static final String DEFAULT_MODEL = "openai/gpt-oss-20b";

    private static final String PREFS_NAME = "ai_settings";
    private static final String KEY_API = "groq_api_key";
    private static final String KEY_MODEL = "groq_model";

    private static final String SYSTEM_PROMPT =
            "คุณคือ Nexus AI ผู้ช่วยเขียนโค้ด Android/Java ในแอป Nexus Studio บนมือถือ\n" +
            "- ตอบภาษาไทยเป็นหลัก ยกเว้นโค้ดและชื่อคลาส/เมธอด\n" +
            "- โค้ดให้ใส่ใน markdown code block เช่น ```java ... ```\n" +
            "- ตอบกระชับ ชัดเจน เหมาะกับหน้าจอมือถือ\n" +
            "- ถ้ามี context โค้ดหรือ error ให้ใช้เป็นหลักในการตอบ\n" +
            "- อย่าแต่งโค้ดที่ไม่เกี่ยวข้องหรือยาวเกินจำเป็น";

    private final Context context;

    public GeminiAssistant(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public interface AICallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    /** ข้อความหนึ่งรอบในประวัติแชท */
    public static class ChatMessage {
        public final String role;   // "user" | "assistant" | "system"
        public final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content != null ? content : "";
        }
    }

    public boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.trim().isEmpty();
    }

    private String getApiKey() {
        if (context == null) return "";
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_API, "");
    }

    private String getModel() {
        if (context == null) return DEFAULT_MODEL;
        String model = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_MODEL, DEFAULT_MODEL);
        if (model == null || model.trim().isEmpty()) return DEFAULT_MODEL;
        model = model.trim();

        // โมเดลที่ free/developer tier ใช้ไม่ได้แล้ว → บังคับใช้ default
        if (model.equals("llama-3.3-70b-versatile")
                || model.equals("llama-3.1-8b-instant")
                || model.equals("llama3-70b-8192")
                || model.equals("llama3-8b-8192")
                || model.equals("mixtral-8x7b-32768")
                || model.equals("gemma2-9b-it")) {
            return DEFAULT_MODEL;
        }
        return model;
    }

    /** เรียกแบบสั้น (ไม่มีประวัติ) — ใช้ system prompt มาตรฐาน */
    public void askAI(final String prompt, final AICallback callback) {
        askAI(prompt, null, 0.4, 2048, callback);
    }

    /** alias เดิม — โค้ดเก่าที่เรียก askAi ยังใช้ได้ */
    public void askAi(final String prompt, final AICallback callback) {
        askAI(prompt, callback);
    }

    /** เรียกแบบสั้น + ตั้ง temperature / maxTokens */
    public void askAI(final String prompt, final double temperature,
                      final int maxTokens, final AICallback callback) {
        askAI(prompt, null, temperature, maxTokens, callback);
    }

    /**
     * เรียกแบบเต็ม
     * @param prompt      ข้อความ user ปัจจุบัน
     * @param history     ประวัติแชทเก่า (null ได้) — จะตัดเหลือประมาณ 8 รอบล่าสุด
     * @param temperature 0.0–1.0 (แนะนำ 0.2–0.4 สำหรับโค้ด)
     * @param maxTokens   จำนวน token สูงสุดของคำตอบ
     */
    public void askAI(final String prompt,
                      final List<ChatMessage> history,
                      final double temperature,
                      final int maxTokens,
                      final AICallback callback) {
        if (callback == null) return;

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String apiKey = getApiKey();
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    callback.onError("ไม่พบ Groq API Key กรุณาไปตั้งค่าที่ AI Settings ก่อนใช้งาน");
                    return;
                }

                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(90000);

                JSONObject body = new JSONObject();
                body.put("model", getModel());
                body.put("temperature", Math.max(0.0, Math.min(1.0, temperature)));
                body.put("max_tokens", Math.max(64, Math.min(8192, maxTokens)));

                JSONArray messages = new JSONArray();

                // System
                JSONObject sys = new JSONObject();
                sys.put("role", "system");
                sys.put("content", SYSTEM_PROMPT);
                messages.put(sys);

                // ประวัติ (ตัดเหลือ 8 รอบล่าสุด)
                if (history != null && !history.isEmpty()) {
                    int start = Math.max(0, history.size() - 8);
                    for (int i = start; i < history.size(); i++) {
                        ChatMessage m = history.get(i);
                        if (m == null || m.content == null || m.content.trim().isEmpty()) continue;
                        String role = m.role;
                        if (!"user".equals(role) && !"assistant".equals(role) && !"system".equals(role)) {
                            role = "user";
                        }
                        JSONObject jm = new JSONObject();
                        jm.put("role", role);
                        jm.put("content", m.content);
                        messages.put(jm);
                    }
                }

                // User ปัจจุบัน
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", prompt != null ? prompt : "");
                messages.put(userMsg);

                body.put("messages", messages);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        (responseCode >= 200 && responseCode < 300)
                                ? conn.getInputStream()
                                : (conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()),
                        StandardCharsets.UTF_8));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    callback.onSuccess(parseGroqResponse(response.toString()));
                } else {
                    callback.onError(friendlyHttpError(responseCode, response.toString()));
                }

            } catch (java.net.SocketTimeoutException e) {
                callback.onError("หมดเวลาการเชื่อมต่อ (timeout) — เน็ตช้าหรือ API ไม่ตอบ ลองใหม่อีกครั้ง");
            } catch (java.net.UnknownHostException e) {
                callback.onError("ไม่พบเซิร์ฟเวอร์ — ตรวจสอบการเชื่อมต่ออินเทอร์เน็ต");
            } catch (Exception e) {
                String msg = e.getMessage();
                callback.onError((msg != null && !msg.isEmpty())
                        ? e.getClass().getSimpleName() + ": " + msg
                        : e.getClass().getSimpleName());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /** เวอร์ชันสั้นสำหรับ autocomplete */
    public void askForCompletion(final String prompt, final AICallback callback) {
        askAI(prompt, null, 0.2, 256, callback);
    }

    private String parseGroqResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            return json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
        } catch (Exception e) {
            return "ไม่สามารถอ่านผลลัพธ์จาก AI ได้";
        }
    }

    private String friendlyHttpError(int code, String body) {
        String lower = body != null ? body.toLowerCase() : "";
        if (code == 401 || lower.contains("invalid api key") || lower.contains("unauthorized")) {
            return "API Key ไม่ถูกต้องหรือหมดอายุ — ไปตั้งค่าใหม่ที่ AI Settings";
        }
        if (code == 429 || lower.contains("rate limit")) {
            return "เรียก API บ่อยเกินไป (rate limit) — รอสักครู่แล้วลองใหม่";
        }
        if (code == 400 || (code == 404 && lower.contains("model"))) {
            return "โมเดลนี้ใช้ไม่ได้หรือไม่มีสิทธิ์ — ลองเปลี่ยนเป็น openai/gpt-oss-20b";
        }
        if (code == 400) {
            return "คำขอไม่ถูกต้อง (400) — อาจโมเดลไม่รองรับหรือ prompt ยาวเกินไป";
        }
        if (code >= 500) {
            return "เซิร์ฟเวอร์ Groq มีปัญหาชั่วคราว (HTTP " + code + ") — ลองใหม่ภายหลัง";
        }
        String shortBody = body;
        if (shortBody != null && shortBody.length() > 200) {
            shortBody = shortBody.substring(0, 200) + "...";
        }
        return "HTTP " + code + (shortBody != null && !shortBody.isEmpty() ? "\n" + shortBody : "");
    }

    public static List<ChatMessage> newHistory() {
        return new ArrayList<>();
    }

    public static void addUser(List<ChatMessage> history, String content) {
        if (history != null) history.add(new ChatMessage("user", content));
    }

    public static void addAssistant(List<ChatMessage> history, String content) {
        if (history != null) history.add(new ChatMessage("assistant", content));
    }
}