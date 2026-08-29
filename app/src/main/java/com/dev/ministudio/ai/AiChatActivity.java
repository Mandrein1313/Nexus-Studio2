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

    private final ArrayList<String> codeBlocks = new ArrayList<>();
    private final ArrayList<GeminiAssistant.ChatMessage> conversation = new ArrayList<>();

    /** key แยกประวัติตามโปรเจกต์ */
    private String projectKey = "default";
    private String projectName = "";
    private String appPackageName = "";
    private String projectLanguage = "Java"; // default
    private String projectUrl = ""; // ลิงก์ GitHub ถ้ามี
    private String openFilePath = "";
    private String openFileContent = "";

    // รูปที่แนบไว้รอส่ง
    private String pendingImageBase64 = null;
    private String pendingImageMime = "image/jpeg";
    private String pendingImageDataUrl = null; // โชว์ใน bubble

    private static final String CHAT_PREFS = "ai_chat_history";

    public static final String EXTRA_PROJECT_NAME = "projectName";
    public static final String EXTRA_FILE_PATH = "filePath";
    public static final String EXTRA_CODE_SNIPPET = "codeSnippet";
    public static final String EXTRA_PACKAGE_NAME = "packageName";
    public static final String EXTRA_LANGUAGE = "language"; // "Java" หรือ "Kotlin"
    public static final String EXTRA_PROJECT_URL = "projectUrl";
    public static final String EXTRA_OPEN_FILE_PATH = "openFilePath";
    public static final String EXTRA_OPEN_FILE_CONTENT = "openFileContent";
    // เลือกรูปจากแกลเลอรี
    private final androidx.activity.result.ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri == null) return;
                        new Thread(() -> {
                            try {
                                String[] result = encodeImageFromUri(uri);
                                runOnUiThread(() -> {
                                    if (result == null) {
                                        Toast.makeText(this, "อ่านรูปไม่สำเร็จ", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    pendingImageBase64 = result[0];
                                    pendingImageMime = result[1];
                                    pendingImageDataUrl = "data:" + pendingImageMime + ";base64," + pendingImageBase64;
                                    Toast.makeText(this, "📎 แนบรูปแล้ว — พิมพ์คำถามแล้วกดส่ง", Toast.LENGTH_SHORT).show();
                                });
                            } catch (Exception e) {
                                runOnUiThread(() ->
                                        Toast.makeText(this, "แนบรูปไม่สำเร็จ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        }).start();
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.parseColor("#1F2335"));
        getWindow().setNavigationBarColor(Color.parseColor("#1A1B26"));

        setContentView(R.layout.activity_ai_chat);

        // ชื่อโปรเจกต์ → ใช้เป็น key ประวัติ
        String name = getIntent().getStringExtra(EXTRA_PROJECT_NAME);
        if (name != null && !name.trim().isEmpty()) {
            projectName = name.trim();
            projectKey = projectName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        } else {
            projectName = "";
            projectKey = "default";
        }

        // อ่าน package name
        String pkg = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        appPackageName = (pkg != null && !pkg.trim().isEmpty()) ? pkg.trim() : "";

        // อ่านภาษา (Java/Kotlin)
        String lang = getIntent().getStringExtra(EXTRA_LANGUAGE);
        if (lang != null && !lang.trim().isEmpty()) {
            projectLanguage = lang.trim();
        } else if (!projectName.isEmpty()) {
            // เดาจากโฟลเดอร์บนดิสก์
            projectLanguage = detectProjectLanguage("/sdcard/MiniStudio/" + projectName);
        }

        // อ่านลิงก์โปรเจกต์ (GitHub)
        String url = getIntent().getStringExtra(EXTRA_PROJECT_URL);
        projectUrl = (url != null && !url.trim().isEmpty()) ? url.trim() : "";

        // ไฟล์ที่เปิดอยู่ใน editor
        String ofp = getIntent().getStringExtra(EXTRA_OPEN_FILE_PATH);
        openFilePath = (ofp != null) ? ofp.trim() : "";

        String ofc = getIntent().getStringExtra(EXTRA_OPEN_FILE_CONTENT);
        openFileContent = (ofc != null) ? ofc : "";

        geminiAssistant = new GeminiAssistant(this);
        initTts();
        

        webAiChat = findViewById(R.id.webAiChat);
        etAiInput = findViewById(R.id.etAiInput);
        ImageButton btnBack = findViewById(R.id.btnAiBack);
        ImageButton btnClear = findViewById(R.id.btnAiClear);
        ImageButton btnSend = findViewById(R.id.btnAiSend);
        btnSpeak = findViewById(R.id.btnAiSpeak);
        ImageButton btnHistory = findViewById(R.id.btnAiHistory);
        ImageButton btnAttach = findViewById(R.id.btnAiAttach);

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
            Toast.makeText(this, "ล้างประวัติโปรเจกต์นี้แล้ว", Toast.LENGTH_SHORT).show();
        });

        btnClear.setOnLongClickListener(v -> {
            showHistoryDialog();
            return true;
        });

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> showHistoryDialog());
        }

        // ปุ่มแนบรูป
        if (btnAttach != null) {
            btnAttach.setOnClickListener(v -> {
                if (pendingImageBase64 != null) {
                    // ถ้ามีรูปอยู่แล้ว → ถามว่าจะเอารูปเดิมหรือเปลี่ยนใหม่
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("แนบรูป")
                            .setMessage("มีรูปแนบอยู่แล้ว — เลือกรูปใหม่หรือลบรูปเดิม?")
                            .setPositiveButton("เลือกรูปใหม่", (d, w) -> pickImageLauncher.launch("image/*"))
                            .setNegativeButton("ลบรูปเดิม", (d, w) -> {
                                pendingImageBase64 = null;
                                pendingImageDataUrl = null;
                                Toast.makeText(this, "ลบรูปที่แนบแล้ว", Toast.LENGTH_SHORT).show();
                            })
                            .setNeutralButton("ยกเลิก", null)
                            .show();
                } else {
                    pickImageLauncher.launch("image/*");
                }
            });
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

        // โหลดประวัติของโปรเจกต์นี้เท่านั้น
        loadConversation();

        if (openFilePath != null && !openFilePath.isEmpty()) {
            String name = openFilePath;
            int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
            if (slash >= 0 && slash < name.length() - 1) {
                name = name.substring(slash + 1);
            }
            Toast.makeText(this, "แนบไฟล์: " + name, Toast.LENGTH_SHORT).show();
            }
          }

    /** ดูว่าโปรเจกต์ใช้ java หรือ kotlin เป็นหลัก */
    private String detectProjectLanguage(String rootPath) {
        try {
            java.io.File kotlinDir = new java.io.File(rootPath, "app/src/main/kotlin");
            java.io.File javaDir = new java.io.File(rootPath, "app/src/main/java");

            boolean hasKt = kotlinDir.exists() && hasSourceFile(kotlinDir, ".kt");
            boolean hasJava = javaDir.exists() && hasSourceFile(javaDir, ".java");

            if (hasKt && !hasJava) return "Kotlin";
            if (hasJava && !hasKt) return "Java";
            if (hasKt) return "Kotlin"; // มีทั้งคู่ เน้น Kotlin
        } catch (Exception ignored) {
        }
        return "Java";
    }

    private boolean hasSourceFile(java.io.File dir, String ext) {
        if (dir == null || !dir.isDirectory()) return false;
        java.io.File[] list = dir.listFiles();
        if (list == null) return false;
        for (java.io.File f : list) {
            if (f.isDirectory()) {
                if (hasSourceFile(f, ext)) return true;
            } else if (f.getName().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /** คืนค่า [0]=base64, [1]=mime หรือ null ถ้าล้มเหลว */
    private String[] encodeImageFromUri(android.net.Uri uri) throws Exception {
        android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                getContentResolver(), uri);
        if (bitmap == null) return null;

        // ย่อให้กว้างสุด ~1280px เพื่อไม่ให้ payload ใหญ่เกิน
        int maxSide = 1280;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w > maxSide || h > maxSide) {
            float scale = Math.min((float) maxSide / w, (float) maxSide / h);
            int nw = Math.round(w * scale);
            int nh = Math.round(h * scale);
            android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, nw, nh, true);
            if (scaled != bitmap) {
                bitmap.recycle();
                bitmap = scaled;
            }
        }

        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, bos);
        bitmap.recycle();

        byte[] bytes = bos.toByteArray();

        // กันใหญ่เกิน ~3.5MB หลัง encode โดยคร่าว ๆ
        if (bytes.length > 3_500_000) {
            bos.reset();
            android.graphics.Bitmap again = android.provider.MediaStore.Images.Media.getBitmap(
                    getContentResolver(), uri);
            int max2 = 800;
            float sc = Math.min((float) max2 / again.getWidth(), (float) max2 / again.getHeight());
            android.graphics.Bitmap small = android.graphics.Bitmap.createScaledBitmap(
                    again, Math.round(again.getWidth() * sc), Math.round(again.getHeight() * sc), true);
            small.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, bos);
            small.recycle();
            again.recycle();
            bytes = bos.toByteArray();
        }

        String b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
        return new String[]{b64, "image/jpeg"};
    }

    /** context โปรเจกต์ + package + ภาษา ให้ AI ใช้ตอนตอบ */
    private String buildProjectContext() {
    StringBuilder sb = new StringBuilder();

    boolean isKotlin = projectLanguage != null
            && projectLanguage.toLowerCase().contains("kotlin");
    String langLabel = isKotlin ? "Kotlin" : "Java";
    String srcFolder = isKotlin ? "kotlin" : "java";
    String mainFile = isKotlin ? "MainActivity.kt" : "MainActivity.java";

    if (projectName != null && !projectName.isEmpty()) {
        sb.append("โปรเจกต์: ").append(projectName).append("\n");
    }
    sb.append("ภาษา: ").append(langLabel).append("\n");

    if (appPackageName != null && !appPackageName.isEmpty()) {
        sb.append("package name: ").append(appPackageName).append("\n");
        sb.append("เมื่อเขียนโค้ด ให้ใช้ package ").append(appPackageName)
                .append(" เท่านั้น ห้ามใช้ com.example อื่นเว้นแต่ผู้ใช้ขอ\n");
    }

    // ลิงก์ Git — ใส่ครั้งเดียว ก่อน return
    if (projectUrl != null && !projectUrl.isEmpty()) {
        sb.append("ลิงก์โปรเจกต์ (Git): ").append(projectUrl).append("\n");
        sb.append("ถ้าผู้ใช้อ้างถึง repo นี้ ให้ยึดชื่อ/URL นี้ในคำตอบ\n");
    }

    sb.append("โครงสร้างโปรเจกต์ (มาตรฐาน Nexus Studio):\n");
    sb.append("- app/src/main/").append(srcFolder).append("/{package}/")
            .append(mainFile).append("\n");
    sb.append("- app/src/main/res/layout/activity_main.xml\n");
    sb.append("- app/src/main/res/values/strings.xml, colors.xml, styles.xml\n");
    sb.append("- app/src/main/res/drawable/, mipmap-*\n");
    sb.append("- app/src/main/AndroidManifest.xml\n");
    sb.append("- build.gradle / GitHub Actions (cloud build)\n");

    sb.append("กฎการตอบ:\n");
    if (isKotlin) {
        sb.append("- เขียนโค้ดเป็น Kotlin (").append(mainFile).append(")\n");
        sb.append("- ใช้ syntax Kotlin ไม่ใช่ Java\n");
    } else {
        sb.append("- เขียนโค้ดเป็น Java (").append(mainFile).append(")\n");
        sb.append("- ใช้ syntax Java ไม่ใช่ Kotlin\n");
    }
    sb.append("- แก้ UI ที่ activity_main.xml, logic ที่ MainActivity, resource ที่ res/values\n");

   if (openFilePath != null && !openFilePath.isEmpty()) {
      sb.append("ไฟล์ที่เปิดอยู่: ").append(openFilePath).append("\n");
}
   if (openFileContent != null && !openFileContent.trim().isEmpty()) {
      sb.append("เนื้อหาไฟล์ที่เปิดอยู่ (อาจถูกตัดถ้ายาว):\n");
      sb.append("```\n");
      sb.append(openFileContent);
      sb.append("\n```\n");
      sb.append("เมื่อผู้ใช้พูดถึง \"ไฟล์นี้\" / \"โค้ดนี้\" ให้ยึดไฟล์ด้านบนเป็นหลัก\n");
}

    return sb.toString().trim();
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
        String hint = projectName.isEmpty()
                ? "เริ่มสนทนากับ AI ได้เลย"
                : "โปรเจกต์: " + escapeHtml(projectName) + " (" + escapeHtml(projectLanguage) + ") — เริ่มสนทนาได้เลย";
        String html = "<!DOCTYPE html><html><head><meta charset='utf-8'/>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'/>"
                + "</head><body style='background:#1A1B26;color:#A9B1D6;"
                + "font-family:sans-serif;padding:16px;'>"
                + "<p style='color:#565F89;'>" + hint + "</p>"
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

        // ถ้ามีรูปแนบอยู่
        final String dataUrl = pendingImageDataUrl;
        final String imgB64 = pendingImageBase64;
        final String imgMime = pendingImageMime;
        pendingImageDataUrl = null;
        pendingImageBase64 = null;

        // แสดงข้อความผู้ใช้
        if (imgB64 != null && !imgB64.isEmpty() && dataUrl != null) {
            appendUserBubbleWithImage(msg, dataUrl);
        } else {
            appendUserBubble(msg);
        }

        appendAiBubble("⏳ กำลังคิด...");
        isWaitingReply = true;

        // ประวัติเก่า (ยังไม่รวมข้อความปัจจุบัน)
        ArrayList<GeminiAssistant.ChatMessage> historySnapshot =
                new ArrayList<>(conversation);
        while (historySnapshot.size() > 16) {
            historySnapshot.remove(0);
        }

        // แนบ context โปรเจกต์/package/ภาษา เฉพาะตอนส่ง AI — ประวัติเก็บแค่ข้อความผู้ใช้จริง
        String context = buildProjectContext();
        String promptToAi = context.isEmpty()
                ? msg
                : (context + "\n\nคำถามผู้ใช้:\n" + msg);

        if (imgB64 != null && !imgB64.isEmpty()) {
            // ส่งพร้อมรูป
            geminiAssistant.askAI(promptToAi, imgB64, imgMime, historySnapshot, 0.35, 2048,
                    new GeminiAssistant.AICallback() {
                        @Override
                        public void onSuccess(String responseText) {
                            runOnUiThread(() -> {
                                isWaitingReply = false;
                                lastAiReply = responseText;

                                conversation.add(new GeminiAssistant.ChatMessage("user", msg + " [มีรูป]"));
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
        } else {
            // ส่ง text อย่างเดียว
            geminiAssistant.askAI(promptToAi, historySnapshot, 0.4, 2048,
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

    /** แสดง bubble ผู้ใช้พร้อมรูป */
    private void appendUserBubbleWithImage(String text, String dataUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='margin:12px 0;text-align:right;'>");
        sb.append("<div style='display:inline-block;max-width:85%;text-align:left;'>");

        if (dataUrl != null && !dataUrl.isEmpty()) {
            sb.append("<div style='margin-bottom:6px;'>")
              .append("<img src='").append(dataUrl).append("' ")
              .append("style='max-width:220px;max-height:220px;border-radius:12px;")
              .append("border:1px solid #3B4261;display:block;background:#16161E;'/>")
              .append("</div>");
        }

        sb.append("<span style='background:#3B4261;color:#C0CAF5;padding:10px 14px;")
          .append("border-radius:16px 16px 4px 16px;display:inline-block;'>")
          .append(escapeHtml(text != null ? text : "").replace("\n", "<br>"))
          .append("</span></div></div>");

        chatHistory += sb.toString();
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
                + "img{max-width:100%;height:auto;}"
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

    /** ตัด data URL ออกจาก HTML ที่จะเซฟ */
    private String chatHistoryForStorage() {
        return chatHistory.replaceAll(
                "<img src='data:image/[^']+' [^>]*>",
                "<span style='color:#7AA2F7;'>📎 [รูปที่ส่ง]</span>");
    }

    // ========== ประวัติแยกตามโปรเจกต์ ==========

    private String keyHtml()  { return "chat_html_" + projectKey; }
    private String keyJson()  { return "chat_json_" + projectKey; }
    private String keyCodes() { return "chat_codes_" + projectKey; }

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
            for (String c : codeBlocks) codes.put(c);

            getSharedPreferences(CHAT_PREFS, MODE_PRIVATE).edit()
                    .putString(keyHtml(), chatHistoryForStorage())
                    .putString(keyJson(), arr.toString())
                    .putString(keyCodes(), codes.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConversation() {
        try {
            android.content.SharedPreferences p =
                    getSharedPreferences(CHAT_PREFS, MODE_PRIVATE);
            String html = p.getString(keyHtml(), "");
            String json = p.getString(keyJson(), "[]");
            String codesJson = p.getString(keyCodes(), "[]");

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
        getSharedPreferences(CHAT_PREFS, MODE_PRIVATE).edit()
                .remove(keyHtml())
                .remove(keyJson())
                .remove(keyCodes())
                .apply();
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

        String title = projectName.isEmpty()
                ? "ประวัติการสนทนา"
                : "ประวัติ · " + projectName;

        if (titles.isEmpty()) {
            Toast.makeText(this, "ยังไม่มีประวัติในโปรเจกต์นี้", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title + " (" + titles.size() + " รอบ)")
                .setItems(titles.toArray(new String[0]), (d, which) -> {
                    if (webAiChat != null) {
                        webAiChat.post(() -> webAiChat.pageUp(true));
                    }
                    Toast.makeText(this, titles.get(which), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ปิด", null)
                .setNeutralButton("ล้างโปรเจกต์นี้", (d, w) -> {
                    stopSpeaking();
                    chatHistory = "";
                    lastAiReply = "";
                    codeBlocks.clear();
                    conversation.clear();
                    clearConversationStorage();
                    loadEmptyChat();
                    Toast.makeText(this, "ล้างประวัติโปรเจกต์นี้แล้ว", Toast.LENGTH_SHORT).show();
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
