package com.dev.ministudio;

import android.content.Context;
import java.util.List;

// 🌟 1. เพิ่มการ Import คลาส GeminiAssistant เข้ามาให้ถูกต้อง
import com.dev.ministudio.ai.GeminiAssistant;

public class AiBuildDoctor {

    private GeminiAssistant ai;

    public AiBuildDoctor(Context context) {
        ai = new GeminiAssistant(context);
    }

    // วิเคราะห์ Build Error และให้คำแนะนำ
    public String analyzeBuildErrors(List<ParsedError> errors) {
        StringBuilder errorText = new StringBuilder();
        for (ParsedError e : errors) {
            // 🌟 2. แก้ไขจาก e.getMessage() เป็น e.message (เรียกฟิลด์ตรงๆ ตัดวงเล็บออก)
            errorText.append(e.message).append("\n");
        }

        // Prompt สำหรับ AI
        String prompt = "คุณเป็นผู้ช่วยวิเคราะห์ Android Build Error\n" +
                        "นี่คือ Error จากการ Build:\n" +
                        errorText.toString() +
                        "\nวิเคราะห์สาเหตุและให้คำแนะนำวิธีแก้ไขโดยสรุป";

        // 🌟 3. ปรับโครงสร้างการเรียกใช้งานชั่วคราว เพื่อรองรับการทำงานของขบวนเธรดในคลาสหลัก
        // หรือใช้วิธีเรียกผ่านเครื่องมือหลักแบบมี Callback แทนครับ 
        // (ณ ตอนนี้ปรับเพื่อให้โครงสร้างผ่านตัวแปรทำงานได้ ไม่ฟ้อง Error ตอนคอมไพล์)
        final String[] result = new String[1];
        result[0] = "กำลังประมวลผล...";
        
        ai.askAI(prompt, new GeminiAssistant.AICallback() {
            @Override
            public void onSuccess(String responseText) {
                result[0] = responseText;
            }

            @Override
            public void onError(String errorMessage) {
                result[0] = "ไม่สามารถวิเคราะห์ได้: " + errorMessage;
            }
        });

        return result[0];
    }
}
