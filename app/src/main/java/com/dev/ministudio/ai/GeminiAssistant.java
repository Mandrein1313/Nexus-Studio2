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

public class GeminiAssistant {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final Context context;

    public GeminiAssistant(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    public interface AICallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    public boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.trim().isEmpty();
    }

    private String getApiKey() {
        if (context == null) return "";
        SharedPreferences prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE);
        return prefs.getString("groq_api_key", "");
    }

    // 🌟 1. เมทอดรองรับสำหรับไฟล์ที่เรียกใช้ด้วยพิมพ์เล็ก (askAi) - ป้องกันแอปเด้งจากไฟล์เก่า
    public void askAi(final String prompt, final AICallback callback) {
        askAI(prompt, callback); // ส่งต่อไปทำงานที่ askAI ตัวจริงด้านล่าง
    }

    // 🌟 2. เมทอดตัวจริงที่น้าแก้ไขล่าสุด (askAI)
    public void askAI(final String prompt, final AICallback callback) {
        if (callback == null) return;

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String apiKey = getApiKey();
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    callback.onError("ไม่พบ Groq API Key กรุณาตั้งค่าคีย์ก่อนใช้งาน");
                    return;
                }

                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);

                JSONObject body = new JSONObject();
                body.put("model", "llama-3.3-70b-versatile");

                JSONArray messages = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", prompt);
                messages.put(msg);
                body.put("messages", messages);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                BufferedReader reader;

                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    callback.onSuccess(parseGroqResponse(response.toString()));
                } else {
                    callback.onError("HTTP " + responseCode + "\n" + response);
                }

            } catch (Exception e) {
                callback.onError(e.getClass().getSimpleName() + ": " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private String parseGroqResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            return json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        } catch (Exception e) {
            return "ไม่สามารถอ่านผลลัพธ์จาก AI ได้";
        }
    }
}
