package com.dev.ministudio;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeOptimizerManager {

    public static String createOptimizePrompt(String fileName, String rawCode) {
        if (rawCode == null || rawCode.trim().isEmpty()) return null;

        StringBuilder prompt = new StringBuilder();
        prompt.append("คุณคือ Senior Android Developer และ Expert Code Reviewer ประจำระบบ MiniStudio\n");
        prompt.append("หน้าที่ของคุณคือตรวจสอบและปรับปรุง (Refactor & Optimize) ซอร์สโค้ดนี้ให้มีประสิทธิภาพสูงสุดและทำงานได้รวดเร็วขึ้นเป็นเท่าตัว\n\n");
        prompt.append("[ข้อมูลไฟล์ที่ต้องปรับปรุง]\nชื่อไฟล์: ").append(fileName).append("\n\n");
        prompt.append("[ซอร์สโค้ดต้นฉบับ]:\n```\n").append(rawCode).append("\n```\n\n");
        prompt.append("----------------------------------------\n");
        prompt.append("[เงื่อนไขและกฎเหล็กในการตอบกลับ]:\n");
        prompt.append("1. ค้นหาจุดเยิ่นเย้อ, จุดเสี่ยง Memory Leak\n2. ปรับปรุงโค้ดให้ สั้น กระชับ มีประสิทธิภาพ\n3. ห้ามตัดฟังก์ชันการทำงานหลักทิ้ง\n4. ตอบกลับตามรูปแบบนี้เท่านั้น:\n\n");
        prompt.append("✨ [โค้ดที่ปรับปรุงแล้ว]\n```java\n(ใส่โค้ดที่ปรับปรุงใหม่ทั้งหมดที่นี่ โดยไม่มีคำอธิบายปะปนในโค้ด)\n```\n\n");
        prompt.append("📝 [รายละเอียดการปรับปรุง]\n(อธิบายการปรับปรุงสั้นๆ เป็นภาษาไทย)");

        return prompt.toString();
    }

    public static OptimizedResult parseAiResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return new OptimizedResult("", "⚠️ ไม่ได้รับการตอบกลับจาก AI ครับน้า");
        }

        String updatedCode = "";
        String explanation = "";

        try {
            // 🌟 แก้ไข Regex ให้ถูกต้อง: หาโค้ดใน ```java ... ```
            Pattern codePattern = Pattern.compile("```java\\s*([\\s\\S]*?)\\s*```");
            Matcher matcher = codePattern.matcher(aiResponse);

            if (matcher.find()) {
                updatedCode = matcher.group(1).trim();
            }

            String searchKey = "📝 [รายละเอียดการปรับปรุง]";
            if (aiResponse.contains(searchKey)) {
                explanation = aiResponse.substring(aiResponse.indexOf(searchKey)).trim();
            } else {
                explanation = aiResponse.replaceAll("```[\\s\\S]*?```", "").trim();
            }

        } catch (Exception e) {
            e.printStackTrace();
            explanation = "❌ เกิดข้อผิดพลาดขณะแยกโค้ด: " + e.getMessage();
        }

        return new OptimizedResult(updatedCode, explanation);
    }
}
