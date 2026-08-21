package com.dev.ministudio;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildSummaryAnalyzer {

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    // 🌟 [ปรับปรุงความแม่นยำสูง]: ออกแบบกลุ่ม Regex ใหม่ทั้งหมดเพื่อให้ทนทานต่อ Log ทุกรูปแบบบน GitHub
    // รองรับทั้งแบบสั้น แบบพาธเต็ม (Absolute Path) และแบบมี "ERROR: " นำหน้า โดยจะดึงชื่อไฟล์แท้ ๆ ได้เสมอ
    private static final Pattern JAVAC_ERROR =
            Pattern.compile("(?:ERROR:\\s+)?(?:[^\\s]+/)*([^/\\s]+\\.java):(\\d+):\\s*(?:error:)?\\s*(.*)",
                    Pattern.CASE_INSENSITIVE);
                    
    private static final Pattern XML_ERROR =
            Pattern.compile("(?:ERROR:\\s+)?(?:[^\\s]+/)*([^/\\s]+\\.xml):(\\d+):\\s*(?:AAPT:\\s*error:|error:)?\\s*(.*)",
                    Pattern.CASE_INSENSITIVE);
                    
    private static final Pattern KOTLIN_ERROR =
            Pattern.compile("(?:ERROR:\\s+)?(?:[^\\s]+/)*([^/\\s]+\\.kt):(\\d+):\\s*(?:error:)?\\s*(.*)",
                    Pattern.CASE_INSENSITIVE);

    private boolean hasError = false;
    private String errorType = "UNKNOWN";
    private String errorDetails = "";
    private ParsedError lastError;
    private final ArrayList<ParsedError> errorList = new ArrayList<>();

    // 🤖 ฟิลด์สำหรับเก็บคำแนะนำจาก AI
    private String aiSuggestion = null;

    // 🎨 สีสันใหม่ที่สวยและชัดเจน
    private final int COLOR_HEADER = Color.parseColor("#FF5252");     // แดงสด (หัวข้อ)
    private final int COLOR_FILE = Color.parseColor("#FFAB40");       // ส้ม (ไฟล์)
    private final int COLOR_LINE = Color.parseColor("#64B5F6");       // น้ำเงิน (บรรทัด)
    private final int COLOR_TYPE = Color.parseColor("#FFAB40");       // ส้ม (ประเภท)
    private final int COLOR_MESSAGE = Color.parseColor("#FF8A80");    // แดงอ่อน (รายละเอียด)
    private final int COLOR_SUGGEST = Color.parseColor("#81C784");    // เขียว (คำแนะนำ)
    private final int COLOR_SUCCESS = Color.parseColor("#81C784");
    private final int COLOR_SEPARATOR = Color.parseColor("#BDBDBD");

    public void clearErrors() {
        errorList.clear();
        lastError = null;
        hasError = false;
        errorType = "UNKNOWN";
        errorDetails = "";
        aiSuggestion = null; // เคลียร์ค่า AI ทุกครั้งที่เริ่มบิวด์ใหม่
    }

    public ParsedError getLastError() {
        return lastError;
    }

    public ArrayList<ParsedError> getErrorList() {
        return errorList;
    }

    // 🤖 เมธอดสำหรับรับคำแนะนำที่มาจาก AI
    public void setAiSuggestion(String suggestion) {
        this.aiSuggestion = suggestion;
    }

    // 🤖 ปรับปรุงจุดที่ 2 & 3: สร้าง Prompt ให้ฉลาดขึ้น + รองรับการส่งหลาย Error พร้อมกัน
    public String createAiPrompt() {
        if (!hasError) return null;
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("คุณคือ Android Build Doctor ผู้เชี่ยวชาญการตรวจโค้ด\n\n");
        prompt.append("วิเคราะห์ Error ที่เกิดขึ้นจากการบิวด์โปรเจกต์นี้:\n");
        prompt.append("----------------------------------------\n");
        
        if (!errorList.isEmpty()) {
            prompt.append("[รายการข้อผิดพลาดที่พบ]\n");
            for (int i = 0; i < errorList.size(); i++) {
                ParsedError e = errorList.get(i);
                prompt.append(String.format("%d. ไฟล์: %s\n", i + 1, e.file));
                prompt.append(String.format("   บรรทัด: %d\n", e.line));
                prompt.append(String.format("   ข้อความ: %s\n\n", e.message));
            }
        } else {
            // เคสทั่วไปที่ไม่ได้เข้า Regex (เช่น Git หรือ Auth Error)
            prompt.append("ประเภท Error: ").append(errorType).append("\n");
            prompt.append("รายละเอียด: ").append(errorDetails).append("\n\n");
        }
        
        prompt.append("----------------------------------------\n");
        prompt.append("เงื่อนไขการตอบกลับ:\n");
        prompt.append("- ตอบเป็นภาษาไทยที่เข้าใจง่าย กระชับ\n");
        prompt.append("- ใช้รูปแบบดังต่อไปนี้ในการตอบ:\n\n");
        prompt.append("🔍 สาเหตุ:\n");
        prompt.append("(อธิบายสั้นๆ ว่าเกิดจากอะไร)\n\n");
        prompt.append("💡 วิธีแก้:\n");
        prompt.append("(บอกขั้นตอนการแก้ไขเป็นข้อๆ)\n\n");
        prompt.append("🛠 ตัวอย่างโค้ด:\n");
        prompt.append("(แสดงตัวอย่างโค้ดที่ถูกต้อง หรือจุดที่ต้องระวัง ถ้ามี)");
        
        return prompt.toString();
    }

    public boolean analyzeLine(String line, int defaultColor, LogOutputListener listener) {
        if (line == null) return false;

        // ตรวจสอบ Error ด้วย Regex (เพิ่มความมั่นใจในการดักจับคำ)
        if (checkRegexError(line, JAVAC_ERROR, "JAVA_ERROR") ||
            checkRegexError(line, XML_ERROR, "XML_AAPT2_ERROR") ||
            checkRegexError(line, KOTLIN_ERROR, "KOTLIN_ERROR")) {
            return false;
        }

        // ดักตำแหน่ง ^ (ลูกศรชี้ตำแหน่งผิด)
        if (hasError && lastError != null && line.contains("^")) {
            int colIndex = line.indexOf("^");
            if (colIndex >= 0) {
                lastError.column = colIndex;
            }
        }

        String lowerLine = line.toLowerCase();

        // ตรวจสอบข้อผิดพลาดอื่นๆ
        if (lowerLine.contains("repository not found")) {
            hasError = true;
            errorType = "GIT_URL_MISSING";
            errorDetails = "ไม่พบ GitHub Repository";
            return false;
        }

        if (line.contains("Authentication failed") || line.contains("401 Unauthorized") ||
            line.contains("Bad credentials") || line.contains("403 Forbidden")) {
            hasError = true;
            errorType = "AUTH_ERROR";
            errorDetails = "GitHub Token ไม่ถูกต้องหรือไม่มีสิทธิ์";
            return false;
        }

        if (lowerLine.contains("build failed") || lowerLine.contains("compilejava failed")) {
            hasError = true;
            if (errorType.equals("UNKNOWN")) {
                errorType = "BUILD_COMPILE_FAILED";
                errorDetails = "กระบวนการคอมไพล์ล้มเหลว";
            }
            return true;
        }

        return false;
    }

    private boolean checkRegexError(String line, Pattern pattern, String typeStr) {
        Matcher m = pattern.matcher(line.trim());
        if (!m.find()) return false;

        try {
            // ดึงเฉพาะชื่อไฟล์เพียว ๆ ออกมาเลย เช่น "activity_main.xml" หรือ "MainActivity.java"
            String file = m.group(1).trim(); 
            int lineNumber = Integer.parseInt(m.group(2));
            String message = m.group(3).trim();

            // คลีนคำนำหน้ากรณีตัดคำพลาดจากพาธลึก ๆ
            if (file.contains("/")) {
                file = file.substring(file.lastIndexOf("/") + 1);
            }

            lastError = new ParsedError(file, lineNumber, 0, typeStr, message);
            errorList.add(lastError);

            hasError = true;
            errorType = typeStr;
            errorDetails = message;

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 🎨 ฟังก์ชันแสดงผลสรุปที่สวยงาม
    public void printSummary(LogOutputListener listener) {
        if (!hasError || listener == null) return;

        listener.onAppendLog("\n" + "═".repeat(50) + "\n", COLOR_HEADER);
        listener.onAppendLog("🔍 วิเคราะห์สาเหตุการบิวด์ล้มเหลว\n", COLOR_HEADER);

        if (lastError != null) {
            listener.onAppendLog("📍 ไฟล์ล่าสุดที่พบปัญหา: ", COLOR_HEADER);
            listener.onAppendLog(lastError.file + "\n", COLOR_FILE);
            
            listener.onAppendLog("📍 บรรทัดที่: ", COLOR_HEADER);
            listener.onAppendLog(lastError.line + "\n", COLOR_LINE);
        }

        switch (errorType) {
            case "JAVA_ERROR":
                listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
                listener.onAppendLog("ข้อผิดพลาด Java (Compile Error)\n", COLOR_TYPE);
                break;
            case "XML_AAPT2_ERROR":
                listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
                listener.onAppendLog("ข้อผิดพลาด XML (AAPT2)\n", COLOR_TYPE);
                break;
            case "KOTLIN_ERROR":
                listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
                listener.onAppendLog("ข้อผิดพลาด Kotlin\n", COLOR_TYPE);
                break;
            case "GIT_URL_MISSING":
                listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
                listener.onAppendLog("Git Error\n", COLOR_TYPE);
                break;
            case "AUTH_ERROR":
                listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
                listener.onAppendLog("Authentication Error\n", COLOR_TYPE);
                break;
            default:
                listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
                listener.onAppendLog(errorType + "\n", COLOR_TYPE);
                break;
        }

        listener.onAppendLog("💬 รายละเอียด: ", COLOR_HEADER);
        listener.onAppendLog(errorDetails + "\n", COLOR_MESSAGE);

        // 🤖 ปรับปรุงจุดที่ 1: แยกหัวข้อถอดคำว่า (AI) ออก หาก AI ไม่ได้ทำงาน
        if (aiSuggestion != null && !aiSuggestion.trim().isEmpty()) {
            listener.onAppendLog("🤖 AI Build Doctor: \n", COLOR_HEADER);
        } else {
            listener.onAppendLog("💡 คำแนะนำ: ", COLOR_HEADER);
        }
        listener.onAppendLog(getSuggestion() + "\n", COLOR_SUGGEST);

        listener.onAppendLog("═".repeat(50) + "\n", COLOR_HEADER);
    }

    private String getSuggestion() {
        // 🤖 ถ้ามีข้อมูลคำแนะนำจาก AI ให้คืนค่าของ AI ทันที
        if (aiSuggestion != null && !aiSuggestion.trim().isEmpty()) {
            return aiSuggestion;
        }

        // ถ้าไม่มีให้ใช้ Local Template สำรองแบบเดิม
        switch (errorType) {
            case "JAVA_ERROR":
                return "ตรวจสอบไวยากรณ์, เครื่องหมาย {}, (), ; ตรงบรรทัดที่ระบุ";
            case "XML_AAPT2_ERROR":
                return "ตรวจสอบแท็ก XML เปิด-ปิด ไม่ตรงกัน หรือแอตทริบิวต์ผิด";
            case "KOTLIN_ERROR":
                return "ตรวจสอบประเภทตัวแปร, Null Safety, หรือการสืบทอดคลาส";
            case "GIT_URL_MISSING":
                return "กรุณาตรวจสอบ URL ของ Repository ในการตั้งค่าให้ถูกต้อง";
            case "AUTH_ERROR":
                return "กรุณาตรวจสอบ GitHub Token หรือสิทธิ์การเข้าถึงคลังโค้ด";
            default:
                return "ตรวจสอบโค้ดและลอง Build ใหม่ครับ";
        }
    }
}
