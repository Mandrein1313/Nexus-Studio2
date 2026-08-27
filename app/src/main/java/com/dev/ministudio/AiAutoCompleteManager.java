package com.dev.ministudio;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import io.github.rosemoe.sora.widget.CodeEditor;
import com.dev.ministudio.AiLayoutAnalyzer;

public class AiAutoCompleteManager {

    private final Context context;
    private final CodeEditor codeEditor;
    private final AiLayoutAnalyzer aiAnalyzer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private final Handler debounceHandler = new Handler();
    private Runnable debounceRunnable;
    private boolean isAiThinking = false;
    
    public interface AutoCompleteCallback {
        void onSuggestionReady(String suggestionText);
    }

    public AiAutoCompleteManager(Context context, CodeEditor codeEditor, AiLayoutAnalyzer aiAnalyzer) {
        this.context = context;
        this.codeEditor = codeEditor;
        this.aiAnalyzer = aiAnalyzer;
    }

    /**
     * ⚡ สั่งให้ระบบเริ่มวิเคราะห์โค้ดจังหวะที่น้าขยับตัวอักษร
     */
    public void onTextChanged(String currentCode, int cursorLine, int cursorColumn, AutoCompleteCallback callback) {
        // ใช้ระบบ Debounce (หน่วงเวลา 1.2 วินาทีหลังหยุดพิมพ์) เพื่อป้องกันไม่ให้ยิงถาม AI ถี่เกินไปจนค้าง
        debounceHandler.removeCallbacks(debounceRunnable);
        
        if (isAiThinking) return;

        debounceRunnable = () -> {
            executeAiSuggest(currentCode, cursorLine, callback);
        };
        debounceHandler.postDelayed(debounceRunnable, 1200);
    }

    private void executeAiSuggest(String fullCode, int currentLine, AutoCompleteCallback callback) {
        if (aiAnalyzer == null) return;
        
        isAiThinking = true;
        
        // ส่งข้อความสั่งการ AI ให้ทำหน้าที่เติมโค้ดส่วนถัดไปสั้นๆ เท่านั้น
        String prompt = "คุณคือระบบ AI Copilot เติมโค้ดอัตโนมัติของ MiniStudio\n" +
                "นี่คือโค้ดทั้งหมดที่ผู้ใช้กำลังพิมพ์อยู่ขณะนี้:\n" +
                "```java\n" + fullCode + "\n```\n\n" +
                "คำสั่งเด็ดขาด:\n" +
                "1. จงวิเคราะห์และเขียนรหัสโค้ดถัดไปที่ผู้ใช้น่าจะต้องการพิมพ์ต่อจากบรรทัดที่ " + (currentLine + 1) + "\n" +
                "2. ส่งคำตอบกลับมาเฉพาะ 'รหัสโค้ดท่อนถัดไปสั้นๆ ไม่เกิน 2-3 บรรทัด' เท่านั้น ห้ามมีคำอธิบายภาษาไทย ห้ามมีบล็อกโค้ดใดๆ ทั้งสิ้น";

        aiAnalyzer.askAi(prompt, new AiLayoutAnalyzer.OnAnalysisListener() {
            @Override
            public void onStart() {
                // บอทเริ่มประมวลผลเงียบๆ
            }

            @Override
            public void onSuccess(android.text.SpannableString formattedResult) {
                isAiThinking = false;
                String suggestion = formattedResult.toString().trim();
                
                // กรองพวกสัญลักษณ์ที่ AI อาจจะแถมมาออกให้เกลี้ยง
                suggestion = suggestion.replace("```java", "").replace("```", "");
                
                final String finalSuggestion = suggestion;
                if (!finalSuggestion.isEmpty()) {
                    mainHandler.post(() -> callback.onSuggestionReady(finalSuggestion));
                }
            }

            @Override
            public void onError(String errorMessage) {
                isAiThinking = false;
            }
        });
    }
}
