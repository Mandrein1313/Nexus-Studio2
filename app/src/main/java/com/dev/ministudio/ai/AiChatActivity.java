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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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

    /** เก็บบล็อกโค้ดสำหรับปุ่ม Copy / ใส่ Editor */
    private final ArrayList<String> codeBlocks = new ArrayList<>();

    /** ประวัติ role/content ส่งให้ AI + เซฟลงเครื่อง */
    private final ArrayList<GeminiAssistant.ChatMessage> conversation = new ArrayList<>();

    private static final String CHAT_PREFS = "ai_chat_history";
    private static final String KEY_HTML = "chat_html";
    private static final String KEY_JSON = "chat_json";
    private static final String KEY_CODES = "chat_codes";

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

        // ถ้ามีปุ่มใน layout (แนะนำ id = btnAiHistory)
        ImageButton btnHistory = null;
        try {
            btnHistory = findViewById(getResources().getIdentifier(
                    "btnAiHistory", "id", getPackageName()));
        } catch (Exception ignored) {
        }

        setupWebView();

        btnBack.setOnClickListener(v -> {
            stopSpeaking();
            saveConversation();
            finish();
        });

        btnClear.setOnClickListener(v -> {
            stopSpeaking();
            chatHistory = "";
            lastAiReply = "";
            codeBlocks.clear();
            conversation.clear();
            clearConversationStorage();
            loadEmptyChat();
            Toast.makeText(this, "ล้างประวัติแล้ว", Toast.LENGTH_SHORT).show();
        });

        // กดค้างถังขยะ = เปิดรายการประวัติ (ใช้ได้แม้ยังไม่มีปุ่มแยก)
        btnClear.setOnLongClickListener(v -> {
            showHistoryDialog();
            return true;
        });

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> showHistoryDialog());
        }

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

        // โหลดประวัติที่เซฟไว้ (ถ้าไม่มีจะเป็นหน้าว่าง)
        loadConversation();
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
        webAiChat.removeJavascriptInterface("NexusAI");
        webAiChat.addJavascriptInterface(new AiBridge(), "NexusAI");
    }

    private void loadEmptyChat() {
        chatHistory = "";
        String html = "<!DOCTYPE html><html><head><meta charset='utf-8'/>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'/>"
                + "</head><body style='background:#1A1B26;color:#A9B1D6;"
                + "font-family:sans-serif;padding:16px;'>"
                + "<p style='color:#565F89;'>เริ่มสนทนากับ AI ได้เลย</p>"
                + "<p style='color:#565F89;font-size:12px;'>กดค้างที่ถังขยะ = ดูประวัติ</p>"
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

        ArrayList<GeminiAssistant.ChatMessage> historySnapshot =
                new ArrayList<>(conversation);
        while (historySnapshot.size() > 16) {
            historySnapshot.remove(0);
        }

        geminiAssistant.askAI(msg, historySnapshot, 0.4, 2048,
                new GeminiAssistant.AICallback() {
                    @Override
                    public void onSuccess(String responseText) {
                        runOnUiThread(() -> {
                            isWaitingReply = false;
                            lastAiReply = responseText;

                            conversation.add(new GeminiAssistant.ChatMessage("user", msg));
                            conversation.add(new GeminiAssistant.ChatMessage("assistant", responseText));
                            while (conversation.size() > 16) {
                                conversation.remove(0);
                            }

                            replaceLastAiBubble(responseText);
                            saveConversation();
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
        int divStart = chatHistory.lastIndexOf("<div style='margin:12px 0;text-align:left;'>");
        if (divStart >= 0 && chatHistory.indexOf(marker, divStart) >= 0) {
            chatHistory = chatHistory.substring(0, divStart);
        }
        appendAiBubble(text);
    }

    private void appendUserBubble(String text) {
        chatHistory += "<div style='margin:12px 0;text-align:right;'>"
                + "<span style='background:#3B4261;color:#C0CAF5;padding:10px 14px;"
                + "border-radius:16px 16px 4px 16px;display:inline-block;max-width:85%;'>"
                + escapeHtml(text).replace("\n", "<br>") + "</span></div>";
        reloadChat();
    }

    private void appendAiBubble(String text) {
        chatHistory += "<div style='margin:12px 0;text-align:left;'>"
                + "<div style='background:#24283B;color:#A9B1D6;padding:12px 14px;"
                + "border-radius:16px 16px 16px 4px;display:inline-block;max-width:92%;"
                + "text-align:left;line-height:1.45;'>"
                + formatAiHtml(text)
                + "</div></div>";
        reloadChat();
    }

    private String formatAiHtml(String text) {
        if (text == null) return "";

        StringBuilder out = new StringBuilder();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "```([a-zA-Z0-9_+\\-]*)\\s*\\n?([\\s\\S]*?)```");
        java.util.regex.Matcher m = p.matcher(text);

        int last = 0;
        while (m.find()) {
            out.append(escapeHtml(text.substring(last, m.start())).replace("\n", "<br>"));

            String lang = m.group(1) != null ? m.group(1) : "";
            String code = m.group(2) != null ? m.group(2) : "";
            while (code.endsWith("\n") || code.endsWith("\r")) {
                code = code.substring(0, code.length() - 1);
            }

            int index = codeBlocks.size();
            codeBlocks.add(code);

            out.append("<div style='margin:10px 0;background:#1A1B26;border:1px solid #3B4261;")
                    .append("border-radius:10px;overflow:hidden;'>");

            out.append("<div style='display:flex;justify-content:space-between;align-items:center;")
                    .append("padding:6px 10px;background:#16161E;border-bottom:1px solid #3B4261;'>")
                    .append("<span style='color:#7AA2F7;font-size:12px;'>")
                    .append(escapeHtml(lang.isEmpty() ? "code" : lang))
                    .append("</span>")
                    .append("<div>")
                    .append("<button type='button' onclick='NexusAI.copyCode(")
                    .append(index)
                    .append(")' style='background:#3B4261;color:#C0CAF5;border:none;")
                    .append("border-radius:6px;padding:4px 10px;font-size:12px;margin-right:6px;cursor:pointer;'>")
                    .append("Copy</button>")
                    .append("<button type='button' onclick='NexusAI.insertCode(")
                    .append(index)
                    .append(")' style='background:#7C3AED;color:#FFFFFF;border:none;")
                    .append("border-radius:6px;padding:4px 10px;font-size:12px;cursor:pointer;'>")
                    .append("ใส่ Editor</button>")
                    .append("</div></div>");

            out.append("<pre style='margin:0;padding:12px;overflow-x:auto;")
                    .append("font-family:monospace;font-size:13px;line-height:1.5;")
                    .append("color:#C0CAF5;white-space:pre;'>")
                    .append(escapeHtml(code))
                    .append("</pre></div>");

            last = m.end();
        }

        if (last < text.length()) {
            out.append(escapeHtml(text.substring(last)).replace("\n", "<br>"));
        }
        if (out.length() == 0) {
            return escapeHtml(text).replace("\n", "<br>");
        }
        return out.toString();
    }

    private void reloadChat() {
        String html = "<!DOCTYPE html><html><head>"
                + "<meta charset='utf-8'/>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'/>"
                + "<style>"
                + "body{background:#1A1B26;color:#A9B1D6;font-family:sans-serif;padding:12px;margin:0;}"
                + "pre{white-space:pre;word-wrap:normal;}"
                + "button{outline:none;}"
                + "</style></head><body>"
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
                .replace("\"", "&quot;");
    }

    // ========== ประวัติการสนทนา (เซฟ / โหลด / dialog) ==========

    private void saveConversation() {
        try {
            JSONArray arr = new JSONArray();
            for (GeminiAssistant.ChatMessage m : conversation) {
                JSONObject o = new JSONObject();
                o.put("role", m.role);
                o.put("content", m.content);
                arr.put(o);
            }
            JSONArray codes = new JSONArray();
            for (String c : codeBlocks) {
                codes.put(c);
            }
            getSharedPreferences(CHAT_PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_HTML, chatHistory)
                    .putString(KEY_JSON, arr.toString())
                    .putString(KEY_CODES, codes.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConversation() {
        try {
            android.content.SharedPreferences p =
                    getSharedPreferences(CHAT_PREFS, MODE_PRIVATE);
            String html = p.getString(KEY_HTML, "");
            String json = p.getString(KEY_JSON, "[]");
            String codesJson = p.getString(KEY_CODES, "[]");

            conversation.clear();
            codeBlocks.clear();

            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                conversation.add(new GeminiAssistant.ChatMessage(
                        o.optString("role", "user"),
                        o.optString("content", "")
                ));
            }
            JSONArray codes = new JSONArray(codesJson);
            for (int i = 0; i < codes.length(); i++) {
                codeBlocks.add(codes.optString(i, ""));
            }

            if (html != null && !html.trim().isEmpty()) {
                chatHistory = html;
                reloadChat();
            } else {
                loadEmptyChat();
            }
        } catch (Exception e) {
            e.printStackTrace();
            loadEmptyChat();
        }
    }

    private void clearConversationStorage() {
        getSharedPreferences(CHAT_PREFS, MODE_PRIVATE).edit().clear().apply();
    }

    private void showHistoryDialog() {
        final ArrayList<String> titles = new ArrayList<>();
        for (int i = 0; i < conversation.size(); i++) {
            GeminiAssistant.ChatMessage m = conversation.get(i);
            if ("user".equals(m.role)) {
                String t = m.content != null ? m.content.trim().replace("\n", " ") : "";
                if (t.length() > 48) t = t.substring(0, 48) + "…";
                titles.add((titles.size() + 1) + ". " + t);
            }
        }

        if (titles.isEmpty()) {
            Toast.makeText(this, "ยังไม่มีประวัติการสนทนา", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("ประวัติการสนทนา (" + titles.size() + " รอบ)")
                .setItems(titles.toArray(new String[0]), (d, which) -> {
                    if (webAiChat != null) {
                        webAiChat.post(() -> webAiChat.pageUp(true));
                    }
                    Toast.makeText(this, titles.get(which), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ปิด", null)
                .setNeutralButton("ล้างทั้งหมด", (d, w) -> {
                    stopSpeaking();
                    chatHistory = "";
                    lastAiReply = "";
                    codeBlocks.clear();
                    conversation.clear();
                    clearConversationStorage();
                    loadEmptyChat();
                    Toast.makeText(this, "ล้างประวัติแล้ว", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    public class AiBridge {
        @android.webkit.JavascriptInterface
        public void copyCode(int index) {
            runOnUiThread(() -> {
                if (index < 0 || index >= codeBlocks.size()) {
                    Toast.makeText(AiChatActivity.this, "ไม่พบโค้ด", Toast.LENGTH_SHORT).show();
                    return;
                }
                String code = codeBlocks.get(index);
                android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("code", code));
                    Toast.makeText(AiChatActivity.this, "📋 คัดลอกโค้ดแล้ว", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void insertCode(int index) {
            runOnUiThread(() -> {
                if (index < 0 || index >= codeBlocks.size()) {
                    Toast.makeText(AiChatActivity.this, "ไม่พบโค้ด", Toast.LENGTH_SHORT).show();
                    return;
                }
                String code = codeBlocks.get(index);
                saveConversation();
                android.content.Intent data = new android.content.Intent();
                data.putExtra("insert_code", code);
                setResult(RESULT_OK, data);
                Toast.makeText(AiChatActivity.this, "✨ ใส่โค้ดใน Editor แล้ว", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        @android.webkit.JavascriptInterface
        public void copyText(String text) {
            runOnUiThread(() -> {
                if (text == null) return;
                String decoded = text
                        .replace("\\n", "\n")
                        .replace("\\'", "'")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
                android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("code", decoded));
                    Toast.makeText(AiChatActivity.this, "📋 คัดลอกโค้ดแล้ว", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        saveConversation();
        super.onPause();
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
