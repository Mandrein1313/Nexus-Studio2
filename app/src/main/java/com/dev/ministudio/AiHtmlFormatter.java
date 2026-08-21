package com.dev.ministudio;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiHtmlFormatter {

    public static String convertMarkdownToHtml(String markdownText) {
        if (markdownText == null) return "";
        
        String htmlContent = markdownText;
        
        // ตัวแปลงไฮไลต์หัวข้อหรือข้อความหนาเบื้องต้น
        htmlContent = htmlContent.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        
        // ค้นหาบล็อกโค้ด ``` เพื่อเก็บแยกรูปแบบดิบไว้ชั่วคราว ป้องกันไม่ให้โดนแปลง \n เป็น <br>
        Pattern codeBlockPattern = Pattern.compile("```(\\w*)\\n(.*?)\\n```", Pattern.DOTALL);
        Matcher matcher = codeBlockPattern.matcher(htmlContent);
        
        List<String> codeBlocksList = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        int idCounter = 0;
        
        while (matcher.find()) {
            String lang = matcher.group(1);
            String rawCode = matcher.group(2);
            
            // แปลงโค้ดดิบเพื่อนำไปแสดงผลบนหน้า pre ของ HTML อย่างปลอดภัย
            String displayCode = rawCode
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
            
            String uniqueId = "code_" + idCounter;
            
            // สร้างกล่องโค้ดที่มีโครงสร้าง HTML สวยงามพร้อมปุ่มคำสั่งควบคุมระบบ
            String blockHtml = "<div class='code-container'>" +
                    "  <div class='code-header'>" +
                    "    <span>" + (lang.isEmpty() ? "code" : lang) + "</span>" +
                    "    <div class='action-buttons'>" +
                    "      <button class='btn-copy' onclick=\"copyToClipboard('" + uniqueId + "', this)\">Copy</button>" +
                    "      <button class='btn-use' onclick=\"insertIntoEditor('" + uniqueId + "')\">นำโค้ดไปใช้งาน</button>" +
                    "    </div>" +
                    "  </div>" +
                    "  <pre id='" + uniqueId + "'>" + displayCode + "</pre>" +
                    "</div>";
            
            codeBlocksList.add(blockHtml);
            
            // แทนที่บล็อกโค้ดที่ดึงออกมาด้วยตัวระบุ Placeholder ชั่วคราว
            matcher.appendReplacement(sb, Matcher.quoteReplacement(":::MINISTUDIO_CODE_BLOCK_PLACEHOLDER_" + idCounter + ":::"));
            idCounter++;
        }
        matcher.appendTail(sb);
        htmlContent = sb.toString();
        
        // 🛠️ ตอนนี้เราสามารถแปลงการขึ้นบรรทัดใหม่ธรรมดาเป็น <br> ได้อย่างปลอดภัย โดยไม่ไปรบกวนโครงสร้างของโค้ดจริงแล้วครับน้า
        htmlContent = htmlContent.replace("\n", "<br>");

        // นำบล็อกโค้ดตัวจริงที่มีรูปแบบใหม่อันสมบูรณ์กลับมาใส่แทนที่ Placeholder ชั่วคราว
        for (int i = 0; i < codeBlocksList.size(); i++) {
            htmlContent = htmlContent.replace(":::MINISTUDIO_CODE_BLOCK_PLACEHOLDER_" + i + ":::", codeBlocksList.get(i));
        }

        // ประกอบโครงสร้างหน้าเว็บ แม่แบบ CSS สไตล์ดาร์กสวยงามเหมือนเดิม
        return "<html><head><style>" +
                "body { background-color: #1E1E1E; color: #E0E0E0; font-family: sans-serif; padding: 10px; font-size: 14px; line-height: 1.5; }" +
                "strong { color: #FFB74D; }" +
                ".code-container { background-color: #2D2D2D; border: 1px solid #3E3E3E; border-radius: 6px; margin: 12px 0; overflow: hidden; }" +
                ".code-header { background-color: #252526; padding: 6px 12px; display: flex; justify-content: space-between; align-items: center; color: #858585; font-size: 11px; font-family: monospace; border-bottom: 1px solid #3E3E3E; }" +
                ".action-buttons { display: flex; gap: 6px; }" +
                ".code-header button { border: none; padding: 4px 10px; border-radius: 4px; cursor: pointer; font-size: 11px; font-weight: bold; transition: background 0.2s; }" +
                ".btn-copy { background: #3E3E3E; color: #D4D4D4; }" +
                ".btn-copy:active { background: #555555; }" +
                ".btn-use { background: #BB86FC; color: #121212; }" +
                ".btn-use:active { background: #9a66da; }" +
                "pre { margin: 0; padding: 12px; overflow-x: auto; font-family: monospace; font-size: 13px; color: #9CDCFE; background-color: #1E1E1E; white-space: pre; }" +
                "</style>" +
                "<script>" +
                "function copyToClipboard(elementId, btn) {" +
                "  try {" +
                "    var text = document.getElementById(elementId).innerText;" +
                "    if (window.AndroidBridge && typeof window.AndroidBridge.copyToSystemClipboard === 'function') {" +
                "      window.AndroidBridge.copyToSystemClipboard(text);" +
                "      btn.innerText = 'Copied!';" +
                "      setTimeout(function() { btn.innerText = 'Copy'; }, 2000);" +
                "    } else {" +
                "      alert('สะพานเชื่อมระบบคัดลอกขัดข้อง');" +
                "    }" +
                "  } catch(e) {" +
                "    alert('Error: ' + e.message);" +
                "  }" +
                "}" +
                "" +
                "function insertIntoEditor(elementId) {" +
                "  try {" +
                "    var codeString = document.getElementById(elementId).innerText;" +
                "    if (window.AndroidBridge && typeof window.AndroidBridge.insertCodeIntoEditor === 'function') {" +
                "      window.AndroidBridge.insertCodeIntoEditor(codeString);" +
                "    } else {" +
                "      alert('ระบบเชื่อมต่อระหว่างหน้าแชทกับโปรแกรมแก้ไขขัดข้อง');" +
                "    }" +
                "  } catch(e) {" +
                "    alert('เกิดข้อผิดพลาดในการดึงซอร์สโค้ด: ' + e.message);" +
                "  }" +
                "}" +
                "</script></head><body>" + htmlContent + "</body></html>";
    }
}