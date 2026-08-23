package com.dev.ministudio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.SpannableString;
import java.util.Locale;

import com.dev.ministudio.ai.GeminiAssistant;

public class AiLayoutAnalyzer {

    private final GeminiAssistant aiAssistant;
    private final Handler mainHandler;
    private TextToSpeech tts;
    private boolean ttsInitialized = false;

    public interface OnAnalysisListener {
        void onStart();
        void onSuccess(SpannableString formattedResult);
        void onError(String error);
    }

    public AiLayoutAnalyzer(Context context) {
        Context appContext = context.getApplicationContext();
        this.aiAssistant = new GeminiAssistant(appContext);
        this.mainHandler = new Handler(Looper.getMainLooper());
        initTTS(appContext);
    }

    private void initTTS(Context context) {
        this.tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("th", "TH"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }
                tts.setSpeechRate(0.95f);
                tts.setPitch(1.05f);
                ttsInitialized = true;
            }
        });
    }

    public void analyzeCode(String fileName, String rawCode, final OnAnalysisListener listener) {
        if (!aiAssistant.hasApiKey()) {
            if (listener != null) {
                listener.onError("ยังไม่ได้ตั้งค่า API Key กรุณากรอก Key ก่อนใช้งานครับ");
            }
            return;
        }

        if (listener != null) listener.onStart();
        String prompt = "ไฟล์: " + fileName + "\n\nโค้ด:\n" + rawCode + "\n\nช่วยวิเคราะห์ปัญหา, Code Smell, คำแนะนำ และให้คะแนน 1-10 เป็นภาษาไทย";
        
        aiAssistant.askAi(prompt, new GeminiAssistant.AICallback() {
            @Override
            public void onSuccess(final String responseText) {
                mainHandler.post(() -> processResponse(responseText, listener));
            }
            @Override
            public void onError(final String errorMessage) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onError(errorMessage);
                });
            }
        });
    }

    public void askAi(String userQuestion, final OnAnalysisListener listener) {
        if (!aiAssistant.hasApiKey()) {
            if (listener != null) {
                listener.onError("ยังไม่ได้ตั้งค่า API Key กรุณากรอก Key ก่อนใช้งานครับ");
            }
            return;
        }

        if (listener != null) listener.onStart();
        String prompt = "คุณคือผู้เชี่ยวชาญด้าน Android Development ช่วยตอบคำถามหรือให้คำแนะนำเกี่ยวกับเรื่องนี้ให้หน่อยครับ: \n\n" + userQuestion;
        
        aiAssistant.askAi(prompt, new GeminiAssistant.AICallback() {
            @Override
            public void onSuccess(final String responseText) {
                mainHandler.post(() -> processResponse(responseText, listener));
            }
            @Override
            public void onError(final String errorMessage) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onError(errorMessage);
                });
            }
        });
    }

    private void processResponse(String responseText, OnAnalysisListener listener) {
        // แตกตัวอักษรเพื่อส่งไปให้ระบบอ่านออกเสียง (ล้างเครื่องหมายแปลกๆ ออก)
        String cleanText = responseText
                .replaceAll("`", "")
                .replaceAll("/", " ")
                .replaceAll("\\\\", " ")
                .replaceAll("-", " ");
        
        speakText(cleanText);
        
        SpannableString formatted = new SpannableString(responseText);
        
        if (listener != null) {
            listener.onSuccess(formatted);
        }
    }

    private void speakText(String text) {
        if (tts != null && ttsInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AI_ANALYSIS");
        }
    }

    // ➕ 🛠️ เมทอดที่เพิ่มขึ้นมาใหม่: สำหรับสั่งให้ AI หยุดพูดทันทีในพริบตา!
    public void stopSpeaking() {
        if (tts != null && ttsInitialized) {
            try {
                if (tts.isSpeaking()) {
                    tts.stop();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        if (tts != null) {
            try { 
                tts.stop(); 
                tts.shutdown(); 
            } catch (Exception e) {
                e.printStackTrace();
            }
            tts = null;
        }
    }
}
