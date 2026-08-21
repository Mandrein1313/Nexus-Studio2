package com.dev.ministudio.ai;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import com.dev.ministudio.R; 
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

public class AiChatActivity extends AppCompatActivity {

    private WebView webAiChat;
    private EditText etAiInput;
    private String chatHistory = "";

    // ส่งจาก MainActivity (optional)
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

        webAiChat = findViewById(R.id.webAiChat);
        etAiInput = findViewById(R.id.etAiInput);
        ImageButton btnBack = findViewById(R.id.btnAiBack);
        ImageButton btnClear = findViewById(R.id.btnAiClear);
        ImageButton btnSend = findViewById(R.id.btnAiSend);
        ImageButton btnSpeak = findViewById(R.id.btnAiSpeak);

        setupWebView();

        btnBack.setOnClickListener(v -> finish());

        btnClear.setOnClickListener(v -> {
            chatHistory = "";
            loadEmptyChat();
            Toast.makeText(this, "ล้างประวัติแล้ว", Toast.LENGTH_SHORT).show();
        });

        btnSend.setOnClickListener(v -> sendMessage());

        etAiInput.setOnEditorActionListener((tv, actionId, event) -> {
            sendMessage();
            return true;
        });

        btnSpeak.setOnClickListener(v -> {
            // ต่อกับระบบพูดของเดิม (AiLayoutAnalyzer) ภายหลังได้
            Toast.makeText(this, "เสียง AI (เชื่อมต่อภายหลัง)", Toast.LENGTH_SHORT).show();
        });

        // ถ้ามีโค้ดส่งมาจาก editor
        String snippet = getIntent().getStringExtra(EXTRA_CODE_SNIPPET);
        if (snippet != null && !snippet.isEmpty()) {
            etAiInput.setText("ช่วยอธิบายหรือปรับปรุงโค้ดนี้:\n" + snippet);
        }

        loadEmptyChat();
    }

    private void setupWebView() {
        WebSettings settings = webAiChat.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webAiChat.setBackgroundColor(Color.parseColor("#1A1B26"));

        // สะพาน JS → Android (คัดลอก / แทรกโค้ด)
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
        if (msg.isEmpty()) return;

        etAiInput.setText("");
        appendUserBubble(msg);

        // TODO: เรียก AI API ของโปรเจกต์ (ย้ายจาก MainActivity / AiLayoutAnalyzer)
        // ตอนนี้แสดงข้อความจำลอง
        appendAiBubble("ได้รับข้อความแล้ว — เชื่อมต่อ API ในขั้นตอนถัดไป\n\nคุณพิมพ์ว่า:\n" + msg);
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
        webAiChat.post(() -> webAiChat.scrollTo(0, webAiChat.getContentHeight()));
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
    }

    /** JS bridge — คัดลอก / ส่งโค้ดกลับ editor */
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
        if (webAiChat != null) {
            webAiChat.destroy();
        }
        super.onDestroy();
    }
}
