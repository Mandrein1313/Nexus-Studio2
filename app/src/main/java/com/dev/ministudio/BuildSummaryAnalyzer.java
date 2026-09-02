package com.dev.ministudio;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildSummaryAnalyzer {

    public interface LogOutputListener {
        void onAppendLog(String text, int color);
    }

    // รองรับ path เต็ม / ชื่อไฟล์สั้น / มีคำว่า ERROR: นำหน้า
    private static final Pattern JAVAC_ERROR =
            Pattern.compile(
                    "(?:ERROR:\\s+)?(?:[^\\s]+/)*([^/\\s]+\\.java):(\\d+):\\s*(?:error:)?\\s*(.*)",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern XML_ERROR =
            Pattern.compile(
                    "(?:ERROR:\\s+)?(?:[^\\s]+/)*([^/\\s]+\\.xml):(\\d+):\\s*(?:AAPT:\\s*error:|error:)?\\s*(.*)",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern KOTLIN_ERROR =
            Pattern.compile(
                    "(?:ERROR:\\s+)?(?:[^\\s]+/)*([^/\\s]+\\.kt):(\\d+):\\s*(?:error:)?\\s*(.*)",
                    Pattern.CASE_INSENSITIVE
            );

    private boolean hasError = false;
    private String errorType = "UNKNOWN";
    private String errorDetails = "";
    private ParsedError lastError;
    private final ArrayList<ParsedError> errorList = new ArrayList<>();

    /** คำแนะนำจาก AI (ถ้ามี) */
    private String aiSuggestion = null;

    private final int COLOR_HEADER = Color.parseColor("#FF5252");
    private final int COLOR_FILE = Color.parseColor("#FFAB40");
    private final int COLOR_LINE = Color.parseColor("#64B5F6");
    private final int COLOR_TYPE = Color.parseColor("#FFAB40");
    private final int COLOR_MESSAGE = Color.parseColor("#FF8A80");
    private final int COLOR_SUGGEST = Color.parseColor("#81C784");
    private final int COLOR_SEPARATOR = Color.parseColor("#BDBDBD");

    public void clearErrors() {
        errorList.clear();
        lastError = null;
        hasError = false;
        errorType = "UNKNOWN";
        errorDetails = "";
        aiSuggestion = null;
    }

    public ParsedError getLastError() {
        return lastError;
    }

    public ArrayList<ParsedError> getErrorList() {
        return errorList;
    }

    public boolean hasError() {
        return hasError;
    }

    public void setAiSuggestion(String suggestion) {
        this.aiSuggestion = suggestion;
    }

    /**
     * สร้าง prompt ส่งให้ AI — รองรับหลาย error
     */
    public String createAiPrompt() {
        if (!hasError) return null;

        StringBuilder prompt = new StringBuilder();
        prompt.append("คุณคือ Android Build Doctor ผู้เชี่ยวชาญการตรวจโค้ด\n\n");
        prompt.append("วิเคราะห์ Error ที่เกิดขึ้นจากการบิวด์โปรเจกต์นี้:\n");
        prompt.append("----------------------------------------\n");

        if (!errorList.isEmpty()) {
            prompt.append("[พบข้อผิดพลาด ").append(errorList.size()).append(" รายการ]\n\n");
            int max = Math.min(errorList.size(), 12);
            for (int i = 0; i < max; i++) {
                ParsedError e = errorList.get(i);
                prompt.append(String.format("%d. ไฟล์: %s\n", i + 1, e.file));
                prompt.append(String.format("   บรรทัด: %d\n", e.line));
                prompt.append(String.format("   ประเภท: %s\n", e.type));
                prompt.append(String.format("   ข้อความ: %s\n\n", e.message));
            }
            if (errorList.size() > max) {
                prompt.append("... และอีก ")
                        .append(errorList.size() - max)
                        .append(" รายการ\n\n");
            }
        } else {
            prompt.append("ประเภท Error: ").append(errorType).append("\n");
            prompt.append("รายละเอียด: ").append(errorDetails).append("\n\n");
        }

        prompt.append("----------------------------------------\n");
        prompt.append("เงื่อนไขการตอบกลับ:\n");
        prompt.append("- ตอบเป็นภาษาไทย กระชับ\n");
        prompt.append("- ถ้ามีหลาย error ให้ไล่ทีละข้อสั้น ๆ\n");
        prompt.append("- ใช้รูปแบบ:\n\n");
        prompt.append("🔍 สาเหตุ:\n(อธิบายสั้น ๆ)\n\n");
        prompt.append("💡 วิธีแก้:\n(ขั้นตอนเป็นข้อ ๆ)\n\n");
        prompt.append("🛠 ตัวอย่างโค้ด:\n(ถ้ามี)");

        return prompt.toString();
    }

    /**
     * วิเคราะห์ทีละบรรทัดจาก log
     * @return true ถ้าเป็นบรรทัดสำคัญที่ควรไฮไลต์ใน console (เช่น build failed)
     */
    public boolean analyzeLine(String line, int defaultColor, LogOutputListener listener) {
        if (line == null) return false;

        if (checkRegexError(line, JAVAC_ERROR, "JAVA_ERROR")
                || checkRegexError(line, XML_ERROR, "XML_AAPT2_ERROR")
                || checkRegexError(line, KOTLIN_ERROR, "KOTLIN_ERROR")) {
            return false;
        }

        // ลูกศรชี้คอลัมน์ผิด
        if (hasError && lastError != null && line.contains("^")) {
            int colIndex = line.indexOf("^");
            if (colIndex >= 0) {
                lastError.column = colIndex;
            }
        }

        String lowerLine = line.toLowerCase();

        if (lowerLine.contains("repository not found")) {
            hasError = true;
            errorType = "GIT_URL_MISSING";
            errorDetails = "ไม่พบ GitHub Repository";
            return false;
        }

        if (line.contains("Authentication failed")
                || line.contains("401 Unauthorized")
                || line.contains("Bad credentials")
                || line.contains("403 Forbidden")) {
            hasError = true;
            errorType = "AUTH_ERROR";
            errorDetails = "GitHub Token ไม่ถูกต้องหรือไม่มีสิทธิ์";
            return false;
        }

        if (lowerLine.contains("build failed") || lowerLine.contains("compilejava failed")) {
            hasError = true;
            if ("UNKNOWN".equals(errorType)) {
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
            String file = m.group(1).trim();
            int lineNumber = Integer.parseInt(m.group(2));
            String message = m.group(3).trim();

            if (file.contains("/")) {
                file = file.substring(file.lastIndexOf("/") + 1);
            }

            // กัน error ซ้ำ (ไฟล์ + บรรทัด + ข้อความ)
            for (ParsedError existing : errorList) {
                if (existing.file.equals(file)
                        && existing.line == lineNumber
                        && existing.message.equals(message)) {
                    return true;
                }
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

    /** แสดงสรุปใน console — รองรับหลาย error */
    public void printSummary(LogOutputListener listener) {
        if (!hasError || listener == null) return;

        final int MAX_SHOW = 8;

        listener.onAppendLog("\n" + "═".repeat(50) + "\n", COLOR_HEADER);
        listener.onAppendLog("🔍 วิเคราะห์สาเหตุการบิลด์ล้มเหลว\n", COLOR_HEADER);

        if (!errorList.isEmpty()) {
            listener.onAppendLog("📊 พบข้อผิดพลาด: ", COLOR_HEADER);
            listener.onAppendLog(errorList.size() + " รายการ\n\n", COLOR_MESSAGE);

            int show = Math.min(errorList.size(), MAX_SHOW);
            for (int i = 0; i < show; i++) {
                ParsedError e = errorList.get(i);

                listener.onAppendLog("——— #" + (i + 1) + " ———\n", COLOR_SEPARATOR);

                listener.onAppendLog("📍 ไฟล์: ", COLOR_HEADER);
                listener.onAppendLog(e.file + "\n", COLOR_FILE);

                listener.onAppendLog("📍 บรรทัด: ", COLOR_HEADER);
                listener.onAppendLog(e.line + "\n", COLOR_LINE);

                listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
                listener.onAppendLog(typeLabel(e.type) + "\n", COLOR_TYPE);

                listener.onAppendLog("💬 รายละเอียด: ", COLOR_HEADER);
                listener.onAppendLog(e.message + "\n", COLOR_MESSAGE);
            }

            if (errorList.size() > MAX_SHOW) {
                listener.onAppendLog(
                        "\n… และอีก " + (errorList.size() - MAX_SHOW)
                                + " รายการ (ส่งให้ AI ครบใน prompt)\n",
                        COLOR_SEPARATOR
                );
            }
        } else {
            listener.onAppendLog("📌 ประเภท: ", COLOR_HEADER);
            listener.onAppendLog(typeLabel(errorType) + "\n", COLOR_TYPE);

            listener.onAppendLog("💬 รายละเอียด: ", COLOR_HEADER);
            listener.onAppendLog(errorDetails + "\n", COLOR_MESSAGE);
        }

        listener.onAppendLog("\n", COLOR_SEPARATOR);

        if (aiSuggestion != null && !aiSuggestion.trim().isEmpty()) {
            listener.onAppendLog("🤖 AI Build Doctor:\n", COLOR_HEADER);
            listener.onAppendLog(aiSuggestion + "\n", COLOR_SUGGEST);
        } else {
            listener.onAppendLog("💡 คำแนะนำ: ", COLOR_HEADER);
            listener.onAppendLog(getSuggestion() + "\n", COLOR_SUGGEST);
        }

        listener.onAppendLog("═".repeat(50) + "\n", COLOR_HEADER);
    }

    private String typeLabel(String type) {
        if (type == null) return "UNKNOWN";
        switch (type) {
            case "JAVA_ERROR":
                return "ข้อผิดพลาด Java (Compile Error)";
            case "XML_AAPT2_ERROR":
                return "ข้อผิดพลาด XML (AAPT2)";
            case "KOTLIN_ERROR":
                return "ข้อผิดพลาด Kotlin";
            case "GIT_URL_MISSING":
                return "Git Error";
            case "AUTH_ERROR":
                return "Authentication Error";
            case "BUILD_COMPILE_FAILED":
                return "Build / Compile Failed";
            default:
                return type;
        }
    }

    private String getSuggestion() {
        if (aiSuggestion != null && !aiSuggestion.trim().isEmpty()) {
            return aiSuggestion;
        }

        switch (errorType) {
            case "JAVA_ERROR":
                return "ตรวจสอบไวยากรณ์, เครื่องหมาย {}, (), ; ตรงบรรทัดที่ระบุ"
                        + (errorList.size() > 1 ? " (มีหลายจุด — แก้ทีละไฟล์/บรรทัด)" : "");
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