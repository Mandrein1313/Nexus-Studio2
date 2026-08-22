package com.dev.ministudio.ai;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.dev.ministudio.R;

import java.util.Locale;

public class AiChatActivity extends AppCompatActivity {

    private WebView webAiChat;
    private EditText etAiInput;
    private String chatHistory = "";
    private GeminiAssistant geminiAssistant;
    private boolean isWaitingReply = false;

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean isSpeaking = false;
    private String lastAiReply = "";
    private ImageButton btnSpeak;

    public static final String EXTRA_PROJECT_NAME = "projectName";
    public static final String EXTRA_FILE_PATH = "filePath";
    public static final String EXTRA_CODE_SNIPPET = "codeSnippet";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.parseColor("#1F2335"));
        getWindow().setNavigationBarColor(Color.parseColor("#1A1B26"));

        setContentView(R.layout.activity_ai_chat);

        geminiAssistant = new GeminiAssistant(this);
        initTts();

        webAiChat = findViewById(R.id.webAiChat);
        etAiInput = findViewById(R.id.etAiInput);
        ImageButton btnBack = findViewById(R.id.btnAiBack);
        ImageButton btnClear = findViewById(R.id.btnAiClear);
        ImageButton btnSend = findViewById(R.id.btnAiSend);
        btnSpeak = findViewById(R.id.btnAiSpeak);

        setupWebView();

        btnBack.setOnClickListener(v -> {
            stopSpeaking();
            finish();
        });

        btnClear.setOnClickListener(v -> {
            stopSpeaking();
            chatHistory = "";
            lastAiReply = "";
            loadEmptyChat();
            Toast.makeText(this, "ล้างประวัติแล้ว", Toast.LENGTH_SHORT).show();
        });

        btnSend.setOnClickListener(v -> sendMessage());

        etAiInput.setOnEditorActionListener((tv, actionId, event) -> {
            sendMessage();
            return true;
        });

        if (btnSpeak != null) {
            btnSpeak.setOnClickListener(v -> {
                if (isSpeaking) {
                    stopSpeaking();
                    Toast.makeText(this, "หยุดเสียงแล้ว", Toast.LENGTH_SHORT).show();
                } else if (lastAiReply != null && !lastAiReply.isEmpty()) {
                    speakText(lastAiReply);
                } else {
                    Toast.makeText(this, "ยังไม่มีข้อความ AI ให้พูด", Toast.LENGTH_SHORT).show();
                }
            });
        }

        String snippet = getIntent().getStringExtra(EXTRA_CODE_SNIPPET);
        if (snippet != null && !snippet.isEmpty()) {
            etAiInput.setText("ช่วยอธิบายหรือปรับปรุงโค้ดนี้:\n" + snippet);
        }

        loadEmptyChat();
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new Locale("th", "TH"));
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.US);
                }
                ttsReady = true;
            } else {
                ttsReady = false;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    isSpeaking = true;
                    runOnUiThread(() -> {
                        if (btnSpeak != null) btnSpeak.setAlpha(1f);
                    });
                }

                @Override
                public void onDone(String utteranceId) {
                    isSpeaking = false;
                    runOnUiThread(() -> {
                        if (btnSpeak != null) btnSpeak.setAlpha(0.85f);
                    });
                }

                @Override
                public void onError(String utteranceId) {
                    isSpeaking = false;
                    runOnUiThread(() -> {
                        if (btnSpeak != null) btnSpeak.setAlpha(0.85f);
                    });
                }
            });
        }
    }

    private void speakText(String text) {
        if (tts == null || !ttsReady || text == null || text.trim().isEmpty()) {
            Toast.makeText(this, "ระบบเสียงยังไม่พร้อม", Toast.LENGTH_SHORT).show();
            return;
        }
        stopSpeaking();

        String clean = text
                .replaceAll("```[\\s\\S]*?```", " (โค้ด) ")
                .replaceAll("[#*_`]", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (clean.length() > 1200) {
            clean = clean.substring(0, 1200) + "...";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "ai_reply");
        } else {
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null);
        }
        isSpeaking = true;
        if (btnSpeak != null) btnSpeak.setAlpha(1f);
    }

    private void stopSpeaking() {
        if (tts != null) {
            tts.stop();
        }
        isSpeaking = false;
        if (btnSpeak != null) btnSpeak.setAlpha(0.85f);
    }

    private void setupWebView() {
        WebSettings settings = webAiChat.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webAiChat.setBackgroundColor(Color.parseColor("#1A1B26"));
        webAiChat.addJavascriptInterface(new AiBridge(), "NexusAI");
    }

    private void loadEmptyChat() {
        String html = "<html><body style='background:#1A1B26;color:#A9B1D6;"
                + "font-family:sans-serif;padding:16px;'>"
                + "<p style='color:#565F89;'>เริ่มสนทนากับ AI ได้เลย</p>"
                + "</body></html>";
        webAiChat.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    private void sendMessage() {
        String msg = etAiInput.getText().toString().trim();
        if (msg.isEmpty() || isWaitingReply) return;

        if (!geminiAssistant.hasApiKey()) {
            Toast.makeText(this,
                    "ยังไม่มี Groq API Key\nไปตั้งค่าที่ AI Settings",
                    Toast.LENGTH_LONG).show();
            appendAiBubble("❌ ไม่พบ API Key — กรุณาไปหน้า AI Settings แล้วบันทึก groq_api_key");
            return;
        }

        stopSpeaking();
        etAiInput.setText("");
        appendUserBubble(msg);
        appendAiBubble("⏳ กำลังคิด...");
        isWaitingReply = true;

        geminiAssistant.askAI(msg, new GeminiAssistant.AICallback() {
            @Override
            public void onSuccess(String responseText) {
                runOnUiThread(() -> {
                    isWaitingReply = false;
                    lastAiReply = responseText;
                    replaceLastAiBubble(responseText);
                    // พูดอัตโนมัติหลังตอบ — ถ้าไม่ต้องการ ลบบรรทัดนี้
                    speakText(responseText);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    isWaitingReply = false;
                    lastAiReply = "";
                    replaceLastAiBubble("❌ " + errorMessage);
                });
            }
        });
    }

    private void replaceLastAiBubble(String text) {
        String marker = "⏳ กำลังคิด...";
        int idx = chatHistory.lastIndexOf(escapeHtml(marker));
        if (idx >= 0) {
            int divStart = chatHistory.lastIndexOf("<div style='margin:12px 0;text-align:left;'>");
            if (divStart >= 0) {
                chatHistory = chatHistory.substring(0, divStart);
            }
        }
        appendAiBubble(text);
    }

    private void appendUserBubble(String text) {
        chatHistory += "<div style='margin:12px 0;text-align:right;'>"
                + "<span style='background:#3B4261;color:#C0CAF5;padding:10px 14px;"
                + "border-radius:16px 16px 4px 16px;display:inline-block;max-width:85%;'>"
                + escapeHtml(text) + "</span></div>";
        reloadChat();
    }

    private void appendAiBubble(String text) {
        chatHistory += "<div style='margin:12px 0;text-align:left;'>"
                + "<span style='background:#24283B;color:#A9B1D6;padding:10px 14px;"
                + "border-radius:16px 16px 16px 4px;display:inline-block;max-width:85%;'>"
                + escapeHtml(text) + "</span></div>";
        reloadChat();
    }

    private void reloadChat() {
        String html = "<html><body style='background:#1A1B26;color:#A9B1D6;"
                + "font-family:sans-serif;padding:12px;'>"
                + chatHistory
                + "</body></html>";
        webAiChat.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        webAiChat.post(() -> webAiChat.pageDown(true));
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    public class AiBridge {
        @android.webkit.JavascriptInterface
        public void copyText(String text) {
            runOnUiThread(() -> {
                android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("ai", text));
                    Toast.makeText(AiChatActivity.this, "คัดลอกแล้ว", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        stopSpeaking();
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        try {
            if (webAiChat != null) {
                webAiChat.stopLoading();
                webAiChat.loadUrl("about:blank");
                webAiChat.removeAllViews();
                webAiChat.destroy();
                webAiChat = null;
            }
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }
}