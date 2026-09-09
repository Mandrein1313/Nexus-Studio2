package com.dev.ministudio;

import android.content.Context;

import com.dev.ministudio.ai.GeminiAssistant;

import java.util.List;

/**
 * วิเคราะห์ Android Build Error ด้วย AI แล้วให้คำแนะนำภาษาไทย
 */
public class AiBuildDoctor {

    private final GeminiAssistant ai;

    public AiBuildDoctor(Context context) {
        this.ai = new GeminiAssistant(context);
    }

    /**
     * วิเคราะห์รายการ error แบบ async
     * ต้องใช้ callback เพราะการเรียก API ทำบน background thread
     */
    public void analyzeBuildErrors(List<ParsedError> errors, GeminiAssistant.AICallback callback) {
        if (callback == null) return;

        if (errors == null || errors.isEmpty()) {
            callback.onError("ไม่มี error ให้วิเคราะห์");
            return;
        }

        if (!ai.hasApiKey()) {
            callback.onError("ไม่พบ Groq API Key — ไปตั้งค่าที่ AI Settings ก่อน");
            return;
        }

        StringBuilder errorText = new StringBuilder();
        int count = 0;
        for (ParsedError e : errors) {
            if (e == null) continue;
            // จำกัดจำนวน error ที่ส่ง เพื่อไม่ให้ prompt ยาวเกิน
            if (count >= 15) {
                errorText.append("... และ error อีก ").append(errors.size() - count).append(" รายการ\n");
                break;
            }
            if (e.message != null && !e.message.trim().isEmpty()) {
                errorText.append("- ").append(e.message.trim()).append("\n");
                count++;
            }
        }

        if (errorText.length() == 0) {
            callback.onError("ไม่พบข้อความ error ที่อ่านได้");
            return;
        }

        String prompt =
                "คุณเป็นผู้ช่วยวิเคราะห์ Android Build Error สำหรับแอป Nexus Studio\n\n" +
                "นี่คือ Error จากการ Build:\n" +
                errorText +
                "\nกรุณา:\n" +
                "1. สรุปสาเหตุหลักเป็นข้อ ๆ (ภาษาไทย)\n" +
                "2. แนะนำวิธีแก้ไขทีละขั้นตอนสั้น ๆ\n" +
                "3. ถ้าเป็น dependency / Gradle / XML / Java ที่พบบ่อย ให้บอกไฟล์หรือบรรทัดที่น่าสงสัย\n" +
                "ตอบกระชับ เหมาะกับหน้าจอมือถือ";

        // temperature ต่ำเพื่อคำตอบเสถียร, max_tokens พอสำหรับคำแนะนำ
        ai.askAI(prompt, null, 0.3, 1024, callback);
    }
}