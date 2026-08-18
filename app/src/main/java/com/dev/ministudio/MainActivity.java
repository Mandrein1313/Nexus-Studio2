package com.dev.ministudio;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue; 
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView; 
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.FrameLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.langs.java.JavaLanguage;
import io.github.rosemoe.sora.event.ContentChangeEvent;
import com.dev.ministudio.fs.FileSystemManager;
import com.dev.ministudio.model.ProjectModel;
import com.dev.ministudio.model.FileNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import android.text.SpannableString;
import android.content.Intent;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import android.os.Build;
import androidx.core.view.WindowCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.content.SharedPreferences;

public class MainActivity extends AppCompatActivity {

    // Views
    private TextView tvSaveStatus, tvFilePath;
    private CodeEditor codeEditor; 
    private DrawerLayout drawerLayout;
    private ListView treeView; 
    private LinearLayout searchBar;
    private android.widget.EditText etFind, etReplace; 
    
    // Tab System Views
    private RecyclerView tabRecyclerView;
    private TabAdapter tabAdapter;

    // 🌟 ระบบ Dialog เต็มหน้าจอชุดใหม่ (Full-screen Panel)
    private android.app.Dialog fullPanelDialog;
    private TabLayout dialogTabLayout;
    private ViewPager2 dialogViewPager;
    private PanelPagerAdapter dialogPanelAdapter;
    
    private TextView tvConsole;
        
    // Controllers & Models
    private ProjectModel currentProject;

    // Utils
    private final Handler autoSaveHandler = new Handler(); 
    private Runnable saveRunnable;
    private int lastSearchIndex = 0;
    
    private float currentCodeFontSize = 14.0f; 

    // 🛠️ แยกออกไปจัดการที่ระบบภายนอกคลาสหลัก
    private ProjectTreeManager projectTreeManager;

    private BuildEnvironmentManager buildEnvManager;
    private static final int PICK_FILE_REQUEST_CODE = 2026; 
    
    private ProjectDialogManager dialogManager;
    
    // 🤖 ตัวจัดการวิเคราะห์เลย์เอาต์ระดับสูงเพื่อความเสถียร
    public com.dev.ministudio.AiLayoutAnalyzer aiLayoutAnalyzer; 
    
    private RecyclerView rvErrorPanel;
    
    // 🌟 ระบบ XML Preview
    private FrameLayout previewContainer;
    private boolean isPreviewMode = false; 
    private String chatHistory = "";
    // Views ตัวใหม่เพิ่มเติม
    private LinearLayout emptyStateView;
    private AiAutoCompleteManager aiAutoCompleteManager;
   private LinearLayout aiSuggestionBar;
   private TextView tvAiSuggestionText;
   private String lastReceivedSuggestion = "";
   private String pendingProjectName = "";
   private boolean isLightEditorTheme = false;
   private boolean isShortcutExpanded = false;
   
   
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

    getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

    int barColor = android.graphics.Color.parseColor("#1A1B26");
    getWindow().setStatusBarColor(barColor);
    getWindow().setNavigationBarColor(barColor);

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
    View decor = getWindow().getDecorView();
    decor.setSystemUiVisibility(0);
}

    setContentView(R.layout.activity_main);

    View drawerContent = findViewById(R.id.drawer_content);
    if (drawerContent != null) {
        int statusBarHeight = 0;
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resId);
        }
        int extra = (int) (8 * getResources().getDisplayMetrics().density);
        drawerContent.setPadding(
                drawerContent.getPaddingLeft(),
                statusBarHeight + extra,
                drawerContent.getPaddingRight(),
                drawerContent.getPaddingBottom()
        );
    }

    buildEnvManager = new BuildEnvironmentManager(this);

    initViews();
    setupLogic();
}
private void initViews() {
    etFind = findViewById(R.id.etFind);
    etReplace = findViewById(R.id.etReplace);
    searchBar = findViewById(R.id.searchBar);
    codeEditor = findViewById(R.id.codeEditor); 
    tvFilePath = findViewById(R.id.tvFilePath); 
    tvSaveStatus = findViewById(R.id.tvSaveStatus);
    emptyStateView = findViewById(R.id.emptyStateView);
    
    treeView = findViewById(R.id.treeView); 
    tabRecyclerView = findViewById(R.id.tabRecyclerView);
    tabRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    drawerLayout = findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, android.R.string.ok, android.R.string.cancel);
    drawerLayout.addDrawerListener(toggle);
    toggle.syncState();

    findViewById(R.id.btnNext).setOnClickListener(v -> findAndHighlight());
    findViewById(R.id.btnReplace).setOnClickListener(v -> replaceText());
    
    setupShortcutBar();

    // ===== ปุ่มด้านล่าง =====
    TextView btnToggleShortcut = findViewById(R.id.btnToggleShortcut);
    View shortcutRow = findViewById(R.id.shortcutRow);
    ImageView btnColorPicker = findViewById(R.id.btnColorPicker);
    TextView btnOpenBrace = findViewById(R.id.btnOpenBrace);
    TextView btnCloseBrace = findViewById(R.id.btnCloseBrace);
    TextView btnMainAi = findViewById(R.id.btnMainAi);
    ImageView btnUndo = findViewById(R.id.btnUndo);
    ImageView btnRedo = findViewById(R.id.btnRedo);

    // ===== ตั้งค่าเริ่มต้น: ซ่อนแถบสัญลักษณ์ =====
    isShortcutExpanded = false;
    if (shortcutRow != null) {
        shortcutRow.setVisibility(View.GONE);
    }
    if (btnToggleShortcut != null) {
        btnToggleShortcut.setText("⌄");
        btnToggleShortcut.setRotation(180f);
    }

    if (btnUndo != null) {
        btnUndo.setOnClickListener(v -> {
            if (codeEditor != null) codeEditor.undo();
        });
    }
    if (btnRedo != null) {
        btnRedo.setOnClickListener(v -> {
            if (codeEditor != null) codeEditor.redo();
        });
    }

    // ปุ่ม Color Picker (ไอคอนจริง)
    if (btnColorPicker != null) {
        btnColorPicker.setOnClickListener(v -> showFullColorPickerDialog());
    }

    if (btnOpenBrace != null) {
        btnOpenBrace.setOnClickListener(v -> {
            if (codeEditor != null && codeEditor.getCursor() != null) {
                codeEditor.getText().insert(
                        codeEditor.getCursor().getLeftLine(),
                        codeEditor.getCursor().getLeftColumn(),
                        "{"
                );
            }
        });
    }
    if (btnCloseBrace != null) {
        btnCloseBrace.setOnClickListener(v -> {
            if (codeEditor != null && codeEditor.getCursor() != null) {
                codeEditor.getText().insert(
                        codeEditor.getCursor().getLeftLine(),
                        codeEditor.getCursor().getLeftColumn(),
                        "}"
                );
            }
        });
    }
    if (btnMainAi != null) {
        btnMainAi.setOnClickListener(v -> handleAiAction(false));
    }

    // ===== Toggle ย่อ/ขยาย =====
    if (btnToggleShortcut != null && shortcutRow != null) {
        btnToggleShortcut.setOnClickListener(v -> {
            if (isShortcutExpanded) {
                isShortcutExpanded = false;
                shortcutRow.animate()
                        .alpha(0f)
                        .translationY(-30f)
                        .setDuration(180)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator())
                        .withEndAction(() -> {
                            shortcutRow.setVisibility(View.GONE);
                            shortcutRow.setAlpha(1f);
                            shortcutRow.setTranslationY(0f);
                        })
                        .start();
                btnToggleShortcut.animate().rotation(180f).setDuration(200).start();
                btnToggleShortcut.setText("⌄");
            } else {
                isShortcutExpanded = true;
                shortcutRow.setVisibility(View.VISIBLE);
                shortcutRow.setAlpha(0f);
                shortcutRow.setTranslationY(-30f);
                shortcutRow.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(220)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
                btnToggleShortcut.animate().rotation(0f).setDuration(200).start();
                btnToggleShortcut.setText("⌃");
            }
        });
    }

    rvErrorPanel = findViewById(R.id.rvErrorPanel);
    if (rvErrorPanel != null) {
        rvErrorPanel.setLayoutManager(new LinearLayoutManager(this));
    }

    previewContainer = findViewById(R.id.previewContainer);
}
private void setupLogic() {
    aiLayoutAnalyzer = new com.dev.ministudio.AiLayoutAnalyzer(this);
    dialogManager = new ProjectDialogManager(this, parentNode -> {
        triggerTreeRefresh(parentNode);
    });

    if (codeEditor == null) return;

    codeEditor.setEditorLanguage(new JavaLanguage());

    // โหลดธีมจากหน้า Projects
    SharedPreferences appPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
    boolean isLight = appPrefs.getBoolean("editor_light_theme", false);
    if (isLight) {
        codeEditor.setColorScheme(new com.dev.ministudio.editor.NexusLightColorScheme());
    } else {
        codeEditor.setColorScheme(new com.dev.ministudio.editor.NexusColorScheme());
    }
    isLightEditorTheme = isLight;

    codeEditor.setTextSize(currentCodeFontSize);
    codeEditor.setTypefaceText(android.graphics.Typeface.MONOSPACE);
    codeEditor.setLineSpacing(2f, 1.2f);
    codeEditor.setWordwrap(false);
    codeEditor.setUndoEnabled(true);
    codeEditor.setHighlightCurrentBlock(true);

    // แผง AI suggestion
    aiSuggestionBar = findViewById(R.id.aiSuggestionBar);
    tvAiSuggestionText = findViewById(R.id.tvAiSuggestionText);
    Button btnAcceptAi = findViewById(R.id.btnAcceptAiSuggestion);

    aiAutoCompleteManager = new AiAutoCompleteManager(this, codeEditor, aiLayoutAnalyzer);

    if (btnAcceptAi != null) {
        btnAcceptAi.setOnClickListener(v -> {
            if (codeEditor != null && !lastReceivedSuggestion.isEmpty()) {
                int line = codeEditor.getCursor().getLeftLine();
                int column = codeEditor.getCursor().getLeftColumn();
                codeEditor.getText().insert(line, column, lastReceivedSuggestion);
                lastReceivedSuggestion = "";
                if (aiSuggestionBar != null) {
                    aiSuggestionBar.setVisibility(View.GONE);
                }
                showToast("✨ เติมโค้ดสำเร็จ!");
            }
        });
    }

    // Auto-Save + AI Auto-Complete
    codeEditor.subscribeEvent(ContentChangeEvent.class, (event, unsubscribe) -> {
        if (tvSaveStatus != null) {
            tvSaveStatus.setText("Editing...");
            tvSaveStatus.setTextColor(android.graphics.Color.parseColor("#FF9E64"));
        }

        autoSaveHandler.removeCallbacks(saveRunnable);
        saveRunnable = () -> {
            saveFile();
            if (tvSaveStatus != null) {
                tvSaveStatus.setText("Saved");
                tvSaveStatus.setTextColor(android.graphics.Color.parseColor("#9ECE6A"));
            }
        };
        autoSaveHandler.postDelayed(saveRunnable, 1500);

        if (codeEditor.getCursor() != null && aiAutoCompleteManager != null) {
            String fullText = codeEditor.getText().toString();
            int curLine = codeEditor.getCursor().getLeftLine();
            int curCol = codeEditor.getCursor().getLeftColumn();

            aiAutoCompleteManager.onTextChanged(fullText, curLine, curCol, suggestionText -> {
                runOnUiThread(() -> {
                    lastReceivedSuggestion = suggestionText;
                    if (tvAiSuggestionText != null) {
                        tvAiSuggestionText.setText(suggestionText);
                    }
                    if (aiSuggestionBar != null) {
                        aiSuggestionBar.setVisibility(View.VISIBLE);
                    }
                });
            });
        }
    });

    // แสดงสีที่แถบสถานะเมื่อ cursor อยู่บนรหัสสี
    codeEditor.subscribeEvent(
            io.github.rosemoe.sora.event.SelectionChangeEvent.class,
            (event, unsubscribe) -> showColorPreviewIfNeeded()
    );

    // แตะรหัสสี → เปิด Edit Color (จับตำแหน่งแม่นกว่า)
    codeEditor.subscribeEvent(io.github.rosemoe.sora.event.ClickEvent.class, (event, unsubscribe) -> {
        if (codeEditor == null) return;

        // รอให้ cursor ขยับตามนิ้วก่อน แล้วค่อยอ่านตำแหน่ง
        codeEditor.post(() -> {
            if (codeEditor.getCursor() == null) return;

            int line = codeEditor.getCursor().getLeftLine();
            int col = codeEditor.getCursor().getLeftColumn();

            String lineText;
            try {
                lineText = codeEditor.getText().getLineString(line);
            } catch (Exception e) {
                return;
            }
            if (lineText == null || lineText.isEmpty()) return;

            // ลำดับ 8 → 6 → 3 กันจับ #RGB สั้นเกินจาก #RRGGBB
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "#(?:[0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{3})"
            );
            java.util.regex.Matcher matcher = pattern.matcher(lineText);

            String bestHex = null;
            int bestStart = -1;
            int bestEnd = -1;
            int bestDist = Integer.MAX_VALUE;

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                // อยู่ในช่วง หรือชิดขอบ ±1
                if (col >= start - 1 && col <= end) {
                    int mid = (start + end) / 2;
                    int dist = Math.abs(col - mid);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestHex = matcher.group();
                        bestStart = start;
                        bestEnd = end;
                    }
                }
            }

            if (bestHex != null) {
                showFullColorPickerDialog(bestHex, line, bestStart, line, bestEnd);
            }
        });
    });

    // โหลดโปรเจกต์
    String projectName = getIntent().getStringExtra("projectName");
    if (projectName != null) {
        String rootPath = "/sdcard/MiniStudio/" + projectName;
        currentProject = new ProjectModel(projectName, rootPath);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(currentProject.getProjectName());
        }
        setupTabLogic();
        if (treeView != null) {
            projectTreeManager = new ProjectTreeManager(this, treeView);
            projectTreeManager.initializeFileTree();
        }
        setEditorActiveState(false);
    }
}
private void showColorPreviewIfNeeded() {
    if (codeEditor == null || codeEditor.getCursor() == null || tvSaveStatus == null) {
        return;
    }

    int line = codeEditor.getCursor().getLeftLine();
    int col = codeEditor.getCursor().getLeftColumn();

    String lineText;
    try {
        lineText = codeEditor.getText().getLineString(line);
    } catch (Exception e) {
        return;
    }
    if (lineText == null || lineText.isEmpty()) return;

    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})\\b"
    );
    java.util.regex.Matcher matcher = pattern.matcher(lineText);

    String foundHex = null;
    while (matcher.find()) {
        if (col >= matcher.start() && col <= matcher.end()) {
            foundHex = matcher.group();
            break;
        }
    }

    if (foundHex != null) {
        int color = parseHexColor(foundHex);
        if (color != 0) {
            tvSaveStatus.setText("● " + foundHex);
            tvSaveStatus.setTextColor(color);
            return;
        }
    }

    // ออกจากรหัสสี → คืน Saved ถ้ากำลังโชว์ preview อยู่
    CharSequence cur = tvSaveStatus.getText();
    if (cur != null && cur.toString().startsWith("●")) {
        tvSaveStatus.setText("Saved");
        tvSaveStatus.setTextColor(android.graphics.Color.parseColor("#9ECE6A"));
    }
}

private int parseHexColor(String hex) {
    try {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() == 3) {
            h = "" + h.charAt(0) + h.charAt(0)
                    + h.charAt(1) + h.charAt(1)
                    + h.charAt(2) + h.charAt(2);
            return android.graphics.Color.parseColor("#" + h);
        } else if (h.length() == 6) {
            return android.graphics.Color.parseColor("#" + h);
        } else if (h.length() == 8) {
            return (int) Long.parseLong(h, 16);
        }
    } catch (Exception ignored) {
    }
    return 0;
}

    // 🌟 ฟังก์ชันเปิดหน้าต่าง Dialog คอนโซลแบบเต็มหน้าจอ (เวอร์ชันแก้ไขให้เห็น Status Bar + ดักปิดเสียง AI)
private void showFullPanelDialog(int initialTabPosition) {
    if (fullPanelDialog != null && fullPanelDialog.isShowing()) {
        dialogViewPager.setCurrentItem(initialTabPosition, true);
        return;
    }

    fullPanelDialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar);
    fullPanelDialog.setContentView(R.layout.dialog_full_console_panel);
    fullPanelDialog.setCancelable(true);

    if (fullPanelDialog.getWindow() != null) {
        android.view.Window window = fullPanelDialog.getWindow();
        window.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        );
        // กันเนื้อหาไม่ให้ทับ status bar / navigation bar
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(android.graphics.Color.parseColor("#1E1E1E"));
        window.setNavigationBarColor(android.graphics.Color.parseColor("#1E1E1E"));
    }

    dialogTabLayout = fullPanelDialog.findViewById(R.id.tabLayout);
    dialogViewPager = fullPanelDialog.findViewById(R.id.viewPager);
    
    fullPanelDialog.findViewById(R.id.btnCloseConsole).setOnClickListener(v -> fullPanelDialog.dismiss());
    
    View btnToggleExpand = fullPanelDialog.findViewById(R.id.btnToggleExpand);
    if (btnToggleExpand != null) btnToggleExpand.setVisibility(View.GONE);

    fullPanelDialog.findViewById(R.id.btnClearConsole).setOnClickListener(v -> {
        if (dialogPanelAdapter != null) {
            TextView consoleView = dialogPanelAdapter.getTvConsole();
            android.webkit.WebView webView = dialogPanelAdapter.getWebAiOutput();
            
            if (consoleView != null) consoleView.setText("");
            if (webView != null) {
                chatHistory = ""; 
                webView.loadDataWithBaseURL(null, "<html><body style='background-color:#1E1E1E;'></body></html>", "text/html", "utf-8", null);
            }
        }
        if (tvConsole != null) tvConsole.setText("");
    });

    dialogPanelAdapter = new PanelPagerAdapter(this);
    dialogViewPager.setAdapter(dialogPanelAdapter);
    dialogViewPager.setUserInputEnabled(false); 

    new TabLayoutMediator(dialogTabLayout, dialogViewPager, (tab, position) -> {
        tab.setText(position == 0 ? "Console" : "AI");
    }).attach();

    dialogViewPager.post(() -> {
        if (dialogPanelAdapter != null) {
            tvConsole = dialogPanelAdapter.getTvConsole();
            dialogViewPager.setCurrentItem(initialTabPosition, false);
        }
    });

    // ดักปิดเสียง AI เมื่อปิดหน้าต่าง
    fullPanelDialog.setOnDismissListener(dialog -> {
        if (aiLayoutAnalyzer != null) {
            aiLayoutAnalyzer.stopSpeaking(); 
        }
    });

    fullPanelDialog.show();
}

    public void handleAiQuery() {
        if (fullPanelDialog == null || !fullPanelDialog.isShowing()) {
            showFullPanelDialog(1);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialogPanelAdapter == null) return;

            android.widget.EditText etAiInput = dialogPanelAdapter.getEtAiInput();
            android.webkit.WebView webAiOutput = dialogPanelAdapter.getWebAiOutput();

            if (etAiInput == null || webAiOutput == null) return;

            // 🎯 เปิดสิทธิ์การใช้งาน JavaScript และผูกสะพานเชื่อมตัวหลัก
            webAiOutput.getSettings().setJavaScriptEnabled(true);
            webAiOutput.getSettings().setDomStorageEnabled(true);
            webAiOutput.removeJavascriptInterface("AndroidBridge");
            webAiOutput.addJavascriptInterface(new WebAppInterface(MainActivity.this), "AndroidBridge");

            String userQuestion = etAiInput.getText().toString().trim();
            if (userQuestion.isEmpty()) {
                chatHistory += "\n\n⚠️ *กรุณาพิมพ์คำถามก่อนครับ*";
                String html = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
                webAiOutput.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
                return;
            }

            // สั่งหยุดพูดทันทีก่อนที่ AI ตัวใหม่จะประมวลผลคำถามถัดไป (ป้องกันเสียงตีกัน)
            if (aiLayoutAnalyzer != null) {
                aiLayoutAnalyzer.stopSpeaking();
            }

            dialogViewPager.setCurrentItem(1, true);

            chatHistory += "\n\n👤 **คุณ:** " + userQuestion;
            String htmlUser = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
            webAiOutput.loadDataWithBaseURL(null, htmlUser, "text/html", "utf-8", null);

            String fullPrompt = chatHistory + "\nผู้ใช้ถาม: " + userQuestion;

            aiLayoutAnalyzer.askAi(fullPrompt, new AiLayoutAnalyzer.OnAnalysisListener() {
                @Override
                public void onStart() {
                    runOnUiThread(() -> {
                        try {
                            android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                            if (currentWeb != null) {
                                currentWeb.getSettings().setJavaScriptEnabled(true);
                                currentWeb.removeJavascriptInterface("AndroidBridge");
                                currentWeb.addJavascriptInterface(new WebAppInterface(MainActivity.this), "AndroidBridge");
                                
                                String tempHtml = AiHtmlFormatter.convertMarkdownToHtml(chatHistory + "\n\n🤖 *AI กำลังคิด...*");
                                currentWeb.loadDataWithBaseURL(null, tempHtml, "text/html", "utf-8", null);
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }

                @Override
                public void onSuccess(android.text.SpannableString formattedResult) {
                    runOnUiThread(() -> {
                        try {
                            android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                            chatHistory += "\n\n🤖 **AI:** " + formattedResult.toString();
                            
                            if (currentWeb != null) {
                                currentWeb.getSettings().setJavaScriptEnabled(true);
                                currentWeb.removeJavascriptInterface("AndroidBridge");
                                currentWeb.addJavascriptInterface(new WebAppInterface(MainActivity.this), "AndroidBridge");
                                
                                String htmlResult = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
                                currentWeb.loadDataWithBaseURL(null, htmlResult, "text/html", "utf-8", null);
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        try {
                            android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                            chatHistory += "\n\n❌ **AI เกิดข้อผิดพลาด:** " + errorMessage;
                            
                            if (currentWeb != null) {
                                currentWeb.getSettings().setJavaScriptEnabled(true);
                                currentWeb.removeJavascriptInterface("AndroidBridge");
                                currentWeb.addJavascriptInterface(new WebAppInterface(MainActivity.this), "AndroidBridge");

                                String htmlError = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
                                currentWeb.loadDataWithBaseURL(null, htmlError, "text/html", "utf-8", null);
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
                }
            });

            etAiInput.setText("");
        }, 300);
    }
    // 🌟 ระบบตรวจจับสกัดกั้นและแก้บั๊กอัจฉริยะ (AI Error Fixer Pipeline) สำหรับระบบที่ 1 ตัวใหม่ล่าสุดครับท่าน
    public void triggerAiErrorFixerPipeline() {
        if (codeEditor == null || currentProject == null) {
            showToast("⚠️ ไม่สามารถเข้าถึงตัวจัดเตรียมรหัสซอร์สโค้ดได้");
            return;
        }

        // 1. ดึงข้อความล็อก Error จาก Console ออกมาทั้งหมด
        String consoleLog = "";
        if (dialogPanelAdapter != null && dialogPanelAdapter.getTvConsole() != null) {
            consoleLog = dialogPanelAdapter.getTvConsole().getText().toString().trim();
        } else if (tvConsole != null) {
            consoleLog = tvConsole.getText().toString().trim();
        }

        if (consoleLog.isEmpty() || consoleLog.equals("> Ready to build...")) {
            showToast("🔎 ยังไม่มีบันทึกข้อผิดพลาด (Error Log) ปรากฏขึ้นในคอนโซลครับท่าน");
            return;
        }

        // 2. ดึงข้อมูลโค้ดดิบในหน้าตัวแก้ไขปัจจุบันที่กำลังทำงาน
        java.io.File currentFile = currentProject.getCurrentOpenFile();
        final String fileName = (currentFile != null) ? currentFile.getName() : "UnknownFile.java";
        String currentSourceCode = codeEditor.getText().toString();

        // 3. ปรับโครงสร้างเพื่อบังคับมุมมองแท็บย้ายไปหน้าต่างแผงแสดงผล AI อัตโนมัติ
        if (dialogViewPager != null) {
            dialogViewPager.setCurrentItem(1, true);
        }

        // สั่งระงับเสียงพูดเดิมทันทีป้องกันการทำงานเหลื่อมล้ำซ้อนกันครับท่าน
        if (aiLayoutAnalyzer != null) {
            aiLayoutAnalyzer.stopSpeaking();
        }

        // 4. บันทึกและแสดงข้อความบอกฝั่งผู้ใช้ให้ทราบบนหน้ากระดานสนทนา
        chatHistory += "\n\n🚨 **[ระบบตรวจจับอัตโนมัติ]:** ร้องขอให้แก้ไขบั๊กของไฟล์ `" + fileName + "` จากข้อความผิดพลาดในระบบ Console";
        
        runOnUiThread(() -> {
            try {
                android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                if (currentWeb != null) {
                    currentWeb.getSettings().setJavaScriptEnabled(true);
                    currentWeb.removeJavascriptInterface("AndroidBridge");
                    currentWeb.addJavascriptInterface(new WebAppInterface(MainActivity.this), "AndroidBridge");
                    
                    String tempHtml = AiHtmlFormatter.convertMarkdownToHtml(chatHistory + "\n\n🤖 *AI กำลังวิเคราะห์สาเหตุและค้นหาจุดพังเพื่อซ่อมโค้ดให้ท่าน...*");
                    currentWeb.loadDataWithBaseURL(null, tempHtml, "text/html", "utf-8", null);
                }
            } catch (Exception e) { e.printStackTrace(); }
        });

        // 5. ป้อนคำสั่ง Prompt คุณภาพวิเคราะห์เจาะลึกส่งให้โมเดลประมวลผลแก้ปัญหาตรงจุด
        String errorFixerPrompt = "คุณคือระบบ AI ตรวจจับและแก้ไขบั๊กอัตโนมัติประจำโปรแกรม MiniStudio\n\n" +
                "นี่คือชื่อไฟล์ที่เกิดปัญหา: " + fileName + "\n\n" +
                "❌ ข้อความผิดพลาดที่เกิดขึ้นในหน้าจอ Console (Error Log):\n" +
                "```\n" + consoleLog + "\n```\n\n" +
                "📄 ซอร์สโค้ดปัจจุบันในไฟล์นี้ทั้งหมด:\n" +
                "```java\n" + currentSourceCode + "\n```\n\n" +
                "กรุณาทำตามคำสั่งต่อไปนี้อย่างเข้มงวด:\n" +
                "1. อธิบายสั้นๆ ว่าโค้ดพังที่บรรทัดไหน และเกิดจากสาเหตุใด\n" +
                "2. ส่งซอร์สโค้ดของไฟล์นี้ทั้งหมดที่แก้ไขปัญหาเสร็จสมบูรณ์ร้อยเปอร์เซ็นต์แล้วกลับมาให้ในบล็อกโค้ด ```java เพื่อให้ผู้ใช้สามารถกดปุ่มนำไปใช้งานสวมทับได้ทันที";

        aiLayoutAnalyzer.askAi(errorFixerPrompt, new AiLayoutAnalyzer.OnAnalysisListener() {
            @Override
            public void onStart() {}

            @Override
            public void onSuccess(android.text.SpannableString formattedResult) {
                runOnUiThread(() -> {
                    try {
                        android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                        chatHistory += "\n\n🤖 **AI Fixer แนะนำแนวทางแก้ไขสำหรับไฟล์ (" + fileName + "):**\n" + formattedResult.toString();
                        
                        if (currentWeb != null) {
                            currentWeb.getSettings().setJavaScriptEnabled(true);
                            currentWeb.removeJavascriptInterface("AndroidBridge");
                            currentWeb.addJavascriptInterface(new WebAppInterface(MainActivity.this), "AndroidBridge");
                            
                            String htmlResult = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
                            currentWeb.loadDataWithBaseURL(null, htmlResult, "text/html", "utf-8", null);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    try {
                        android.webkit.WebView currentWeb = dialogPanelAdapter.getWebAiOutput();
                        chatHistory += "\n\n❌ **AI Fixer ไม่สามารถวิเคราะห์ได้:** " + errorMessage;
                        
                        if (currentWeb != null) {
                            currentWeb.getSettings().setJavaScriptEnabled(true);
                            currentWeb.removeJavascriptInterface("AndroidBridge");
                            currentWeb.addJavascriptInterface(new WebAppInterface(MainActivity.this), "AndroidBridge");
                            
                            String htmlError = AiHtmlFormatter.convertMarkdownToHtml(chatHistory);
                            currentWeb.loadDataWithBaseURL(null, htmlError, "text/html", "utf-8", null);
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                });
            }
        });
    }

    private void toggleXmlPreview() {
        if (codeEditor == null || previewContainer == null) {
            showToast("⚠️ ไม่พบแผงควบคุมระบบพรีวิวในหน้าจอนี้");
            return;
        }

        if (!isPreviewMode) {
            try {
                String currentXmlCode = codeEditor.getText().toString();
                XmlPreviewManager previewManager = new XmlPreviewManager(MainActivity.this);
                View generatedView = previewManager.inflateXml(currentXmlCode);

                if (generatedView != null) {
                    previewContainer.removeAllViews();
                    previewContainer.addView(generatedView);

                    codeEditor.setVisibility(View.GONE);
                    previewContainer.setVisibility(View.VISIBLE);
                    
                    isPreviewMode = true;
                    showToast("✨ แสดงผลพรีวิวเลย์เอาต์สำเร็จ!");
                    invalidateOptionsMenu(); 
                }
            } catch (Exception e) {
                showToast("❌ ไวยากรณ์ XML ขัดข้อง: " + e.getMessage());
            }
        } else {
            previewContainer.setVisibility(View.GONE);
            codeEditor.setVisibility(View.VISIBLE);
            isPreviewMode = false;
            invalidateOptionsMenu();
        }
    }

    private void startCloudBuildPipeline() {
        if (currentProject == null) {
            showToast("กรุณาเปิดโปรเจกต์ก่อนทำการรัน");
            return;
        }

        SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String savedToken = prefs.getString("token", "");

        if (username.isEmpty() || savedToken.isEmpty()) {
            showToast("❌ ยังไม่ได้ตั้งค่าบัญชี GitHub กรุณาตั้งค่าที่ปุ่มฟันเพืองหน้าแรกก่อนครับ");
            return;
        }

        saveFile(); 
        showFullPanelDialog(0);

        final BuildSummaryAnalyzer analyzer = new BuildSummaryAnalyzer();
        analyzer.clearErrors(); 
        
        final boolean[] isPipelineStopped = {false};

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dialogPanelAdapter != null) {
                tvConsole = dialogPanelAdapter.getTvConsole();
            }
            if (tvConsole != null) tvConsole.setText("");

            appendLog("##[group]เริ่มขั้นตอนการตั้งค่า & ตรวจสอบโปรเจกต์เบื้องต้น", TerminalColor.LOG_GRAY); 
            appendLog("🔔 [กำลังจัดเตรียมสภาพแวดล้อม...] เริ่มทำงานระบบ Workflow สำเร็จ", TerminalColor.LOG_WHITE);
            appendLog("📂 ที่อยู่โปรเจกต์ (Root Path): " + currentProject.getRootPath(), TerminalColor.BORDER_BLUE); 
            appendLog("##[endgroup]", TerminalColor.LOG_GRAY);

            BuildTaskManager buildTask = new BuildTaskManager(
                MainActivity.this, 
                currentProject.getRootPath(),
                new BuildTaskManager.BuildListener() {
                    
                    @Override 
                    public void onLogAppend(final String text, final int color) { 
                        if (isPipelineStopped[0]) return;

                        String lowerText = text != null ? text.toLowerCase() : "";
                        boolean isErrorLine = lowerText.contains("error:") || lowerText.contains("failed:") || color == Color.RED;

                        boolean hasFailed = analyzer.analyzeLine(text, color, new BuildSummaryAnalyzer.LogOutputListener() {
                            @Override
                            public void onAppendLog(String logText, int logColor) {
                                appendLog(logText, logColor); 
                            }
                        });

                        if (hasFailed) {
                            isPipelineStopped[0] = true;
                            showToast("💥 บิวด์ล้มเหลว! (Exit Code 1)");
                            return;
                        }

                        if (text != null && (text.startsWith("📍") || text.startsWith("💬"))) {
                            return;
                        }

                        if (color == Color.GREEN || lowerText.contains("success")) {
                            appendLog(text, TerminalColor.SUGGEST_GREEN); 
                        } else if (color == Color.YELLOW) {
                            appendLog(text, TerminalColor.TARGET_YELLOW); 
                        } else if (color == Color.CYAN) {
                            appendLog(text, TerminalColor.LOG_CYAN); 
                        } else if (isErrorLine) {
                            appendLog(text, TerminalColor.DETAIL_RED); 
                        } else {
                            appendLog(text, TerminalColor.TEXT_WHITE); 
                        }
                    }

                    @Override 
                    public void onBuildStarted() { 
                        showToast("กำลังเริ่มระบบ Cloud Workflow... 🐙"); 
                        appendLog("\n##[group]🚀 เรียกทำงานคำสั่ง: compileJava", TerminalColor.LOG_GRAY);
                        appendLog("🔄 กำลังเชื่อมต่อไปยังเซิร์ฟเวอร์คอมไพล์บนคลาวด์...", TerminalColor.LOG_WHITE);
                    }

                    @Override
                    public void onBuildFinished(boolean success, String apkPath) {
                        if (isPipelineStopped[0]) return;

                        appendLog("##[endgroup]", TerminalColor.LOG_GRAY);

                        if (success) {
                            showToast("บิวด์แอปสำเร็จ! 🎉");
                            appendLog("\n##[group]🎉 งานหลังบิวด์: จัดเก็บไฟล์ระบบแอปพลิเคชัน", TerminalColor.SUGGEST_GREEN);
                            appendLog("✅ สำเร็จ: กระบวนการทำงานทั้งหมดเสร็จสิ้นโดยไม่มีข้อผิดพลาด", TerminalColor.SUGGEST_GREEN);
                            appendLog("📦 ไฟล์แอปที่ได้ (APK): " + (apkPath != null ? apkPath : "outputs/apk/debug/app-debug.apk"), TerminalColor.LOG_CYAN);
                            appendLog("##[endgroup]", TerminalColor.SUGGEST_GREEN);
                            
                            runOnUiThread(() -> { if (rvErrorPanel != null) rvErrorPanel.setVisibility(View.GONE); });
                        } else {
                            showToast("กระบวนการทำงานล้มเหลว");
                            appendLog("\n##[error] การทำงานหยุดช้าลงเนื่องจากการปิดตัวของระบบบิวด์อย่างกะทันหัน", TerminalColor.ERROR_RED);
                            
                            if (analyzer != null) {
                                analyzer.printSummary(new BuildSummaryAnalyzer.LogOutputListener() {
                                    @Override
                                    public void onAppendLog(String text, int color) {
                                        if (dialogPanelAdapter != null) tvConsole = dialogPanelAdapter.getTvConsole();
                                        appendColoredText(tvConsole, text, color);
                                    }
                                });
                            }
                            
                            final ParsedError err = analyzer.getLastError();
                            if (err != null) {
                                runOnUiThread(() -> {
                                    executeJumpToError(err);
                                });
                            }
                        }
                    }
                }
            );

            String githubToken = savedToken; 
            String projectName = currentProject.getProjectName();
            String repoUrl = "https://github.com/" + username + "/" + projectName + ".git";
            String packageName = "com.dev.ministudio"; 

            buildTask.startCloudBuild(githubToken, repoUrl, projectName, packageName); 
            buildTask.setAnalyzer(analyzer);
        }, 300);
    }

    private void executeJumpToError(final ParsedError errorItem) {
        if (errorItem == null || currentProject == null) return;

        try {
            java.io.File targetFile = new java.io.File(errorItem.file);
            if (!targetFile.isAbsolute()) {
                targetFile = new java.io.File(currentProject.getRootPath(), errorItem.file);
            }

            if (targetFile.exists()) {
                openFile(targetFile); 
                
                if (codeEditor != null) {
                    final int zeroBasedLine = Math.max(0, errorItem.line - 1); 
                    final int targetColumn = Math.max(0, errorItem.column);

                    codeEditor.postDelayed(() -> {
                        try {
                            if (codeEditor.getSearcher() != null) {
                                codeEditor.getSearcher().stopSearch();
                            }
                            codeEditor.jumpToLine(zeroBasedLine);            
                            codeEditor.setSelection(zeroBasedLine, targetColumn);
                            codeEditor.setSelectionRegion(zeroBasedLine, targetColumn, zeroBasedLine, targetColumn + 4);
                            
                            if (rvErrorPanel != null) {
                                rvErrorPanel.setVisibility(View.VISIBLE);
                            }
                            showToast("🚨 วาร์ปล็อกเป้าหมายพังในบรรทัดที่ " + errorItem.line + " สำเร็จครับ!");
                        } catch (Exception layoutEx) {
                            layoutEx.printStackTrace();
                        }
                    }, 200); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openFilePicker() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); 
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        startActivityForResult(android.content.Intent.createChooser(intent, "เลือกไฟล์ที่จะนำเข้า"), PICK_FILE_REQUEST_CODE);
    }

public void openFile(File file) {
    if (file == null) return;

    // 1. สั่งเปิดไฟล์ผ่าน Manager ก่อน
    if (projectTreeManager != null) {
        projectTreeManager.openFile(file);
    }

    // 2. บังคับอัปเดต UI ทันทีโดยไม่ต้องรอ Callback ที่อาจช้า
    updateFilePathStatus(file);
    
    // 3. สั่งให้ Editor พร้อมทำงานและ Visible ทันที
    runOnUiThread(() -> {
        if (codeEditor != null) {
            // ดึงไฟล์มาโชว์ใน editor (ถ้าคลาส projectTreeManager ไม่ได้ทำไว้)
            // ตัวอย่างเช่น: codeEditor.setText(FileUtils.read(file));
            
            if (codeEditor.getVisibility() != View.VISIBLE) {
                setEditorActiveState(true);
            }
        }
    });
}


    public void saveFile() {
        if (projectTreeManager != null) {
            projectTreeManager.saveFile();
        }
    }

    private void appendLog(final String text, final int color) {
        runOnUiThread(() -> {
            if (dialogPanelAdapter != null) {
                tvConsole = dialogPanelAdapter.getTvConsole();
            }
            if (tvConsole != null) {
                appendColoredText(tvConsole, text + "\n", color);
            }
        });
    }

private void setupShortcutBar() {
    LinearLayout shortcutBar = findViewById(R.id.shortcutBar);
    LinearLayout aiShortcutBar = findViewById(R.id.aiShortcutBar);
    if (shortcutBar == null) return;

    shortcutBar.removeAllViews();
    if (aiShortcutBar != null) {
        aiShortcutBar.removeAllViews();
        aiShortcutBar.setVisibility(View.GONE); // ซ่อน AI ด้านบน
    }

    float density = getResources().getDisplayMetrics().density;
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, (int) (36 * density)
    );
    params.setMargins((int) (3 * density), (int) (2 * density),
            (int) (3 * density), (int) (2 * density));

    // เหลือแค่สัญลักษณ์ (ไม่มี Undo/Redo และ AI แล้ว)
    String[] shortcuts = {
    // วงเล็บ
    "{", "}", "[", "]", "(", ")", "<", ">",
    // ตัวดำเนินการ
    "=", "+", "-", "*", "/", "%",
    // เครื่องหมายคำพูด
    "\"", "'", "`",
    // อื่น ๆ
    ".", ",", ":", ";", "!", "?",
    "&", "|", "_", "#", "@", "$"
};
    for (String symbol : shortcuts) {
        shortcutBar.addView(createButton(symbol, params, v -> {
            if (codeEditor != null && codeEditor.getCursor() != null) {
                codeEditor.getText().insert(
                        codeEditor.getCursor().getLeftLine(),
                        codeEditor.getCursor().getLeftColumn(),
                        symbol
                );
            }
        }, "#A9B1D6", "#24283B"));
    }
}

private TextView createButton(String text, LinearLayout.LayoutParams ignored,
                              View.OnClickListener listener, String textColor, String bgColor) {
    float density = getResources().getDisplayMetrics().density;
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, (int) (36 * density));
    params.setMargins((int) (3 * density), (int) (2 * density),
            (int) (3 * density), (int) (2 * density));

    TextView btn = new TextView(this);
    btn.setText(text);
    btn.setTextSize(14);
    btn.setGravity(Gravity.CENTER);
    btn.setPadding((int) (10 * density), 0, (int) (10 * density), 0);
    btn.setTextColor(Color.parseColor(textColor));
    btn.setLayoutParams(params);

    GradientDrawable shape = new GradientDrawable();
    shape.setCornerRadius(6 * density);
    shape.setColor(Color.parseColor(bgColor));
    btn.setBackground(shape);
    btn.setOnClickListener(listener);
    return btn;
}
/** เรียกจากปุ่ม palette → แทรกสีใหม่ */
private void showFullColorPickerDialog() {
    showFullColorPickerDialog(null, -1, -1, -1, -1);
}

/**
 * @param initialHex สีเริ่มต้น เช่น #FF008577 (null = Insert ใหม่)
 * @param replaceStartLine ถ้าระบุ >= 0 จะแทนที่ช่วงนี้แทนการ insert
 */
private void showFullColorPickerDialog(String initialHex,
                                       int replaceStartLine, int replaceStartCol,
                                       int replaceEndLine, int replaceEndCol) {
    android.app.Dialog dialog = new android.app.Dialog(this);
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

    float density = getResources().getDisplayMetrics().density;
    int wheelSize = (int) (220 * density);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding((int) (24 * density), (int) (24 * density),
            (int) (24 * density), (int) (16 * density));
    root.setBackground(createRoundedBg("#1F2335", 20));

    // Title
    TextView title = new TextView(this);
    title.setText(initialHex != null ? "Edit Color" : "Insert Color");
    title.setTextColor(Color.parseColor("#C0CAF5"));
    title.setTextSize(18);
    title.setTypeface(null, Typeface.BOLD);
    root.addView(title);

    // ===== วงล้อสี =====
    final ColorWheelView colorWheel = new ColorWheelView(this);
    LinearLayout.LayoutParams wheelParams = new LinearLayout.LayoutParams(wheelSize, wheelSize);
    wheelParams.gravity = Gravity.CENTER_HORIZONTAL;
    wheelParams.topMargin = (int) (12 * density);
    colorWheel.setLayoutParams(wheelParams);
    root.addView(colorWheel);

    // แสดงสี + Hex
    LinearLayout infoRow = new LinearLayout(this);
    infoRow.setOrientation(LinearLayout.HORIZONTAL);
    infoRow.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
    infoParams.topMargin = (int) (12 * density);
    infoRow.setLayoutParams(infoParams);

    final View colorDot = new View(this);
    LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
            (int) (36 * density), (int) (36 * density));
    colorDot.setLayoutParams(dotParams);
    colorDot.setBackground(createCircleBg("#00FF00"));
    infoRow.addView(colorDot);

    final TextView tvHex = new TextView(this);
    tvHex.setText("#FF00FF00");
    tvHex.setTextColor(Color.parseColor("#A9B1D6"));
    tvHex.setTextSize(15);
    tvHex.setPadding((int) (12 * density), 0, 0, 0);
    infoRow.addView(tvHex);
    root.addView(infoRow);

    // ===== ตัวแปรสี =====
    final float[] currentHue = {120f};
    final float[] currentValue = {1f};
    final int[] currentAlpha = {255};

    // ถ้าเป็นโหมด Edit → โหลดสีเดิม
    if (initialHex != null) {
        int c = parseHexColor(initialHex);
        if (c != 0) {
            float[] hsv = new float[3];
            Color.colorToHSV(c, hsv);
            currentHue[0] = hsv[0];
            currentValue[0] = hsv[2];
            currentAlpha[0] = Color.alpha(c);
            try {
                colorWheel.getClass().getMethod("setHue", float.class)
                        .invoke(colorWheel, currentHue[0]);
            } catch (Exception ignored) {
            }
        }
    }

    final Runnable updateColor = () -> {
        int rgb = Color.HSVToColor(new float[]{currentHue[0], 1f, currentValue[0]});
        int colorWithAlpha = (currentAlpha[0] << 24) | (rgb & 0x00FFFFFF);
        String hex = String.format("#%08X", colorWithAlpha);
        tvHex.setText(hex);
        colorDot.setBackground(createCircleBgWithAlpha(colorWithAlpha));
    };

    // เลือกสีจากวงล้อ (Hue)
    colorWheel.setOnColorChangeListener(color -> {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        currentHue[0] = hsv[0];
        updateColor.run();
    });

    // ===== ความสว่าง =====
    TextView tvBrightLabel = new TextView(this);
    tvBrightLabel.setText("ความสว่าง");
    tvBrightLabel.setTextColor(Color.parseColor("#565F89"));
    tvBrightLabel.setTextSize(12);
    tvBrightLabel.setPadding(0, (int) (10 * density), 0, (int) (2 * density));
    root.addView(tvBrightLabel);

    final TextView tvBrightPercent = new TextView(this);
    int brightPct = Math.round(currentValue[0] * 100);
    tvBrightPercent.setText(brightPct + "%");
    tvBrightPercent.setTextColor(Color.parseColor("#A9B1D6"));
    tvBrightPercent.setTextSize(12);
    root.addView(tvBrightPercent);

    android.widget.SeekBar brightSeek = new android.widget.SeekBar(this);
    brightSeek.setMax(100);
    brightSeek.setProgress(brightPct);
    root.addView(brightSeek);

    brightSeek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
            currentValue[0] = progress / 100f;
            tvBrightPercent.setText(progress + "%");
            updateColor.run();
        }

        @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
    });

    // ===== ความโปร่งใส =====
    TextView tvAlphaLabel = new TextView(this);
    tvAlphaLabel.setText("ความโปร่งใส");
    tvAlphaLabel.setTextColor(Color.parseColor("#565F89"));
    tvAlphaLabel.setTextSize(12);
    tvAlphaLabel.setPadding(0, (int) (10 * density), 0, (int) (2 * density));
    root.addView(tvAlphaLabel);

    final TextView tvAlphaPercent = new TextView(this);
    int alphaPct = Math.round(currentAlpha[0] / 255f * 100);
    tvAlphaPercent.setText(alphaPct + "%");
    tvAlphaPercent.setTextColor(Color.parseColor("#A9B1D6"));
    tvAlphaPercent.setTextSize(12);
    root.addView(tvAlphaPercent);

    android.widget.SeekBar alphaSeek = new android.widget.SeekBar(this);
    alphaSeek.setMax(100);
    alphaSeek.setProgress(alphaPct);
    root.addView(alphaSeek);

    alphaSeek.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
            currentAlpha[0] = (int) (progress / 100f * 255);
            tvAlphaPercent.setText(progress + "%");
            updateColor.run();
        }

        @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
    });

    updateColor.run();

    // ===== ปุ่ม Cancel / Apply =====
    LinearLayout btnRow = new LinearLayout(this);
    btnRow.setOrientation(LinearLayout.HORIZONTAL);
    btnRow.setGravity(Gravity.END);
    LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
    btnRowParams.topMargin = (int) (16 * density);
    btnRow.setLayoutParams(btnRowParams);

    TextView btnCancel = new TextView(this);
    btnCancel.setText("Cancel");
    btnCancel.setTextColor(Color.parseColor("#7AA2F7"));
    btnCancel.setTextSize(15);
    btnCancel.setPadding((int) (20 * density), (int) (12 * density),
            (int) (20 * density), (int) (12 * density));
    btnCancel.setOnClickListener(v -> dialog.dismiss());
    btnRow.addView(btnCancel);

    TextView btnApply = new TextView(this);
    btnApply.setText("Apply");
    btnApply.setTextColor(Color.parseColor("#7AA2F7"));
    btnApply.setTextSize(15);
    btnApply.setTypeface(null, Typeface.BOLD);
    btnApply.setPadding((int) (20 * density), (int) (12 * density),
            (int) (20 * density), (int) (12 * density));
    btnApply.setOnClickListener(v -> {
        String hex = tvHex.getText().toString();
        if (codeEditor != null && codeEditor.getCursor() != null) {
            if (replaceStartLine >= 0) {
                // โหมด Edit → แทนที่รหัสเดิม
                codeEditor.getText().delete(
                        replaceStartLine, replaceStartCol,
                        replaceEndLine, replaceEndCol
                );
                codeEditor.getText().insert(replaceStartLine, replaceStartCol, hex);
            } else {
                // โหมด Insert → แทรกที่ cursor
                codeEditor.getText().insert(
                        codeEditor.getCursor().getLeftLine(),
                        codeEditor.getCursor().getLeftColumn(),
                        hex
                );
            }
        }
        dialog.dismiss();
    });
    btnRow.addView(btnApply);
    root.addView(btnRow);

    dialog.setContentView(root);
    if (dialog.getWindow() != null) {
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(
                (int) (320 * density),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }
    dialog.show();
}
// Helper สำหรับสีที่มี alpha
private android.graphics.drawable.GradientDrawable createCircleBgWithAlpha(int color) {
    android.graphics.drawable.GradientDrawable gd =
            new android.graphics.drawable.GradientDrawable();
    gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
    gd.setColor(color);
    return gd;
}

private android.graphics.drawable.GradientDrawable createRoundedBg(String color, int radiusDp) {
    android.graphics.drawable.GradientDrawable gd =
            new android.graphics.drawable.GradientDrawable();
    gd.setColor(Color.parseColor(color));
    gd.setCornerRadius(radiusDp * getResources().getDisplayMetrics().density);
    return gd;
}

private android.graphics.drawable.GradientDrawable createCircleBg(String color) {
    android.graphics.drawable.GradientDrawable gd =
            new android.graphics.drawable.GradientDrawable();
    gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
    gd.setColor(Color.parseColor(color));
    return gd;
}

// จัดการ Logic ของ AI
private void handleAiAction(boolean isOptimize) {
    if (codeEditor == null || currentProject == null) return;
    java.io.File currentFile = currentProject.getCurrentOpenFile();
    if (isOptimize && currentFile == null) {
        showToast("⚠️ กรุณาเปิดไฟล์ที่ต้องการปรับปรุงก่อนครับ");
        return;
    }

    if (aiLayoutAnalyzer != null) aiLayoutAnalyzer.stopSpeaking();
    showFullPanelDialog(1);

    String fileName = (currentFile != null) ? currentFile.getName() : "UnknownFile.java";
    String code = codeEditor.getText().toString();
    String prompt = isOptimize ? CodeOptimizerManager.createOptimizePrompt(fileName, code) : null;

    updateAiOutput("🤖 *" + (isOptimize ? "กำลังสแกนวิเคราะห์เพื่อปรับปรุงโค้ด..." : "กำลังวิเคราะห์โค้ด...") + "*");

    AiLayoutAnalyzer.OnAnalysisListener listener = new AiLayoutAnalyzer.OnAnalysisListener() {
        @Override
        public void onStart() {} // ส่วนแสดงผลถูกเรียกจากบรรทัด updateAiOutput ด้านบนแล้ว
        @Override
        public void onSuccess(android.text.SpannableString result) {
            chatHistory += "\n\n🤖 **" + (isOptimize ? "ผลลัพธ์การปรับปรุง:" : "ผลวิเคราะห์:") + "**\n" + result.toString();
            updateAiOutput(chatHistory);
        }
        @Override
        public void onError(String error) {
            chatHistory += "\n\n❌ **Error:** " + error;
            updateAiOutput(chatHistory);
        }
    };

    if (isOptimize) aiLayoutAnalyzer.askAi(prompt, listener);
    else aiLayoutAnalyzer.analyzeCode(fileName, code, listener);
}

// ฟังก์ชันอัปเดตหน้าจอ WebView ที่ใช้ซ้ำได้
private void updateAiOutput(String markdownText) {
    runOnUiThread(() -> {
        android.webkit.WebView web = dialogPanelAdapter.getWebAiOutput();
        if (web != null) {
            web.getSettings().setJavaScriptEnabled(true);
            web.addJavascriptInterface(new WebAppInterface(this), "AndroidBridge");
            web.loadDataWithBaseURL(null, AiHtmlFormatter.convertMarkdownToHtml(markdownText), "text/html", "utf-8", null);
        }
    });
}

    private void findAndHighlight() {
        String query = etFind.getText().toString();
        String content = codeEditor.getText().toString();
        if (query.isEmpty()) return;

        int index = content.indexOf(query, lastSearchIndex);
        if (index == -1) { index = content.indexOf(query, 0); lastSearchIndex = 0; }

        if (index != -1) {
            soraSelectLinear(index, index + query.length());
            lastSearchIndex = index + query.length();
        } else {
            showToast("Not found");
        }
    }

    private void soraSelectLinear(int startIdx, int endIdx) {
        try {
            String text = codeEditor.getText().toString();
            int startLine = 0, startCol = 0, endLine = 0, endCol = 0, currentIdx = 0;
            String[] lines = text.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                int lineLen = lines[i].length() + 1; 
                if (currentIdx + lineLen > startIdx && startLine == 0 && startCol == 0) {
                    startLine = i; startCol = startIdx - currentIdx;
                }
                if (currentIdx + lineLen > endIdx) {
                    endLine = i; endCol = endIdx - currentIdx; break;
                }
                currentIdx += lineLen;
            }
            final int sL = startLine; final int sC = startCol;
            final int eL = endLine; final int eC = endCol;
            runOnUiThread(() -> {
                codeEditor.setSelectionRegion(sL, sC, eL, eC);
                codeEditor.jumpToLine(sL); 
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void replaceText() {
        String target = etFind.getText().toString();
        String replacement = etReplace.getText().toString();
        if (target.isEmpty()) return;
        String content = codeEditor.getText().toString();
        codeEditor.setText(content.replaceFirst(java.util.regex.Pattern.quote(target), replacement));
        showToast("Replaced");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        MenuItem previewItem = menu.findItem(R.id.action_preview);
        if (previewItem != null) {
            previewItem.setTitle(isPreviewMode ? "ดูโค้ด (Code)" : "ดูตัวอย่าง (Preview)");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_build) { startCloudBuildPipeline(); return true; }
        if (id == R.id.action_preview) { toggleXmlPreview(); return true; }
        
        // 🌟 จุดที่เพิ่มใหม่: ดักจับการกดปุ่ม Git Push
        if (id == R.id.action_git_push) { 
            if (currentProject != null) {
                pushChangesToGithub(currentProject.getProjectName());
            } else {
                showToast("⚠️ กรุณาเปิดโปรเจกต์ก่อนทำการ Push โค้ด");
            }
            return true;
        }

        if (id == R.id.action_ai_settings) {
            startActivity(new Intent(this, AiSettingsActivity.class));
            return true;
        }
        if (id == R.id.action_find_file) {
    showFileSearchDialog();
    return true;
}
        
        if (id == R.id.action_search) {
            searchBar.setVisibility(searchBar.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void toggleEditorTheme() {
    if (codeEditor == null) return;

    isLightEditorTheme = !isLightEditorTheme;
    if (isLightEditorTheme) {
        codeEditor.setColorScheme(new com.dev.ministudio.editor.NexusLightColorScheme());
        showToast("☀️ ธีมสว่าง");
    } else {
        codeEditor.setColorScheme(new com.dev.ministudio.editor.NexusColorScheme());
        showToast("🌙 ธีมมืด");
    }
    getSharedPreferences("AppSettings", MODE_PRIVATE)
            .edit()
            .putBoolean("editor_light_theme", isLightEditorTheme)
            .apply();
}

private void showFileSearchDialog() {
    if (currentProject == null) {
        showToast("ยังไม่ได้เปิดโปรเจกต์");
        return;
    }

    android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar);
    dialog.setContentView(R.layout.dialog_file_search);

if (dialog.getWindow() != null) {
    android.view.Window window = dialog.getWindow();
    window.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
    );
    androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true);
    window.setStatusBarColor(android.graphics.Color.parseColor("#1E1E1E"));
    window.setNavigationBarColor(android.graphics.Color.parseColor("#121212"));
}

// ดันแถบค้นหาลงมาใต้ status bar
View searchBarRoot = dialog.findViewById(R.id.searchBarRoot);
if (searchBarRoot != null) {
    int statusBarHeight = 0;
    int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
    if (resId > 0) {
        statusBarHeight = getResources().getDimensionPixelSize(resId);
    }
    searchBarRoot.setPadding(
            searchBarRoot.getPaddingLeft(),
            statusBarHeight,
            searchBarRoot.getPaddingRight(),
            searchBarRoot.getPaddingBottom()
    );
}

    android.widget.EditText etSearch = dialog.findViewById(R.id.etFileSearch);
    android.widget.TextView tvHint = dialog.findViewById(R.id.tvSearchHint);
    android.widget.ListView lvResults = dialog.findViewById(R.id.lvFileSearchResults);
    android.widget.ImageButton btnBack = dialog.findViewById(R.id.btnSearchBack);
    android.widget.ImageButton btnClear = dialog.findViewById(R.id.btnSearchClear);

    btnBack.setOnClickListener(v -> dialog.dismiss());
    btnClear.setOnClickListener(v -> etSearch.setText(""));

    java.util.List<java.io.File> resultFiles = new java.util.ArrayList<>();
    java.io.File root = new java.io.File(currentProject.getRootPath());
    final String rootPath = root.getAbsolutePath();

    android.widget.BaseAdapter adapter = new android.widget.BaseAdapter() {
        @Override public int getCount() { return resultFiles.size(); }
        @Override public Object getItem(int position) { return resultFiles.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_file_search_result, parent, false);
            }
            java.io.File file = resultFiles.get(position);

            android.widget.TextView tvName = convertView.findViewById(R.id.tvFileName);
            android.widget.TextView tvMeta = convertView.findViewById(R.id.tvFileMeta);
            android.widget.TextView tvPath = convertView.findViewById(R.id.tvFilePath);
            android.widget.ImageView imgIcon = convertView.findViewById(R.id.imgFileIcon);

            String name = file.getName();
            String q = etSearch.getText().toString().trim();
            // ไฮไลต์ส่วนที่ตรงกับคำค้น (ถ้ามี)
            if (!q.isEmpty() && name.toLowerCase().contains(q.toLowerCase())) {
                android.text.SpannableString span = new android.text.SpannableString(name);
                int start = name.toLowerCase().indexOf(q.toLowerCase());
                span.setSpan(
                        new android.text.style.BackgroundColorSpan(android.graphics.Color.parseColor("#FDD835")),
                        start, start + q.length(),
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                span.setSpan(
                        new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#212121")),
                        start, start + q.length(),
                        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                tvName.setText(span);
            } else {
                tvName.setText(name);
            }

            long size = file.length();
            String sizeStr;
            if (size < 1024) sizeStr = size + " B";
            else if (size < 1024 * 1024) sizeStr = String.format("%.2f KB", size / 1024.0);
            else sizeStr = String.format("%.2f MB", size / (1024.0 * 1024.0));

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault());
            String dateStr = sdf.format(new java.util.Date(file.lastModified()));
            tvMeta.setText(sizeStr + "    " + dateStr);

            String path = file.getAbsolutePath();
            if (path.startsWith(rootPath)) {
                path = path.substring(rootPath.length());
                if (path.startsWith("/")) path = path.substring(1);
            }
            // ตัดชื่อไฟล์ออก เหลือแค่โฟลเดอร์
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash >= 0) path = path.substring(0, lastSlash + 1);
            else path = "/";
            tvPath.setText(path);

            String lower = name.toLowerCase();
            if (lower.endsWith(".java")) {
                imgIcon.setColorFilter(android.graphics.Color.parseColor("#00E5FF"));
            } else if (lower.endsWith(".xml")) {
                imgIcon.setColorFilter(android.graphics.Color.parseColor("#FF6D00"));
            } else if (lower.endsWith(".gradle") || lower.endsWith(".properties")) {
                imgIcon.setColorFilter(android.graphics.Color.parseColor("#78909C"));
            } else {
                imgIcon.setColorFilter(android.graphics.Color.parseColor("#B0BEC5"));
            }

            return convertView;
        }
    };
    lvResults.setAdapter(adapter);

    android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    final Runnable[] searchTask = new Runnable[1];

    etSearch.addTextChangedListener(new android.text.TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(android.text.Editable s) {
            String q = s.toString().trim();
            btnClear.setVisibility(q.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);

            if (searchTask[0] != null) searchHandler.removeCallbacks(searchTask[0]);
            searchTask[0] = () -> {
                if (q.isEmpty()) {
                    resultFiles.clear();
                    adapter.notifyDataSetChanged();
                    tvHint.setText("ผลการค้นหา");
                    return;
                }
                tvHint.setText("กำลังค้นหา...");
                new Thread(() -> {
                    java.util.List<java.io.File> found =
                            com.dev.ministudio.fs.FileSystemManager.searchFilesByName(root, q);
                    runOnUiThread(() -> {
                        resultFiles.clear();
                        resultFiles.addAll(found);
                        adapter.notifyDataSetChanged();
                        tvHint.setText(found.isEmpty()
                                ? "ไม่พบไฟล์ที่ตรงกับ \"" + q + "\""
                                : "ผลการค้นหา · " + found.size() + " ไฟล์");
                    });
                }).start();
            };
            searchHandler.postDelayed(searchTask[0], 250);
        }
    });

    lvResults.setOnItemClickListener((parent, view, position, id) -> {
        if (position >= 0 && position < resultFiles.size()) {
            java.io.File file = resultFiles.get(position);
            dialog.dismiss();
            openFile(file);
            if (drawerLayout != null) drawerLayout.closeDrawers();
        }
    });

    dialog.show();
    etSearch.requestFocus();
    android.view.inputmethod.InputMethodManager imm =
            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
    if (imm != null) {
        imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    }
}

    private void triggerTreeRefresh(FileNode parentNode) { 
        if (projectTreeManager != null) projectTreeManager.refreshFileTree(); 
    }

 private void setupTabLogic() {
    tabAdapter = new TabAdapter(currentProject, new TabAdapter.OnTabInterface() {
        @Override
        public void onTabClick(File file) {
            openFile(file);
        }

        @Override
        public void onTabClose(File file, int position) {
            if (currentProject == null || file == null) return;

            // 1. เอาออกจากรายการแท็บ
            currentProject.removeFileFromTabs(file);

            // 2. ถ้าปิดไฟล์ที่กำลังเปิดอยู่ ต้องสลับไปไฟล์อื่น หรือว่าง
            File current = currentProject.getCurrentOpenFile();
            if (current != null && current.equals(file)) {
                java.util.List<File> opened = currentProject.getOpenedFiles();
                if (opened != null && !opened.isEmpty()) {
                    // เปิดแท็บข้างเคียง
                    int newIndex = Math.min(position, opened.size() - 1);
                    if (newIndex < 0) newIndex = 0;
                    openFile(opened.get(newIndex));
                } else {
                    // ไม่เหลือแท็บแล้ว
                    currentProject.setCurrentOpenFile(null);
                    if (codeEditor != null) codeEditor.setText("");
                    setEditorActiveState(false);
                    if (tvFilePath != null) tvFilePath.setText("No file open");
                    if (tvSaveStatus != null) tvSaveStatus.setText("");
                }
            }

            // 3. รีเฟรชแถบแท็บ
            if (tabAdapter != null) {
                tabAdapter.notifyDataSetChanged();
            }
        }
    });
    tabRecyclerView.setAdapter(tabAdapter);
}

    public void updateFilePathStatus(File file) {
    if (tvFilePath != null && file != null) {
        // ดึง Path เต็มๆ มาแสดงผล
        String fullPath = file.getAbsolutePath();
        
        // ถ้าต้องการตัดส่วนของ SDCARD หรือ Root ออกเพื่อความสวยงาม
        // สมมติว่าอยู่ใน /sdcard/MiniStudio/
        String displayPath = fullPath.replace("/sdcard/", ""); 
        
        tvFilePath.setText(displayPath);
        tvFilePath.setSelected(true); // เพิ่มให้ข้อความเลื่อนได้ถ้ามันยาวเกินหน้าจอ
    }
}

    public void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }
    
    private void appendColoredText(TextView tv, String text, int color) {
        if (tv == null) return;
        android.text.SpannableString spannable = new android.text.SpannableString(text);
        spannable.setSpan(new android.text.style.ForegroundColorSpan(color), 0, text.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.append(spannable);
        autoScrollTabContainer(tv);
    }

    private void autoScrollTabContainer(View innerTextView) {
        if (innerTextView == null) return;
        innerTextView.post(() -> {
            try {
                android.view.ViewParent currentParent = innerTextView.getParent();
                while (currentParent != null) {
                    if (currentParent instanceof ScrollView) {
                        ((ScrollView) currentParent).fullScroll(android.view.View.FOCUS_DOWN);
                        break;
                    }
                    currentParent = currentParent.getParent();
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // 🌟 Getters สำหรับเรียกจากภายนอก
    public ProjectModel getCurrentProject() { return currentProject; }
    public ProjectDialogManager getDialogManager() { return dialogManager; }
    public DrawerLayout getDrawerLayout() { return drawerLayout; }
    public CodeEditor getCodeEditor() { return codeEditor; }
    public TabAdapter getTabAdapter() { return tabAdapter; }
    public Handler getAutoSaveHandler() { return autoSaveHandler; }
    public Runnable getSaveRunnable() { return saveRunnable; }
    public PanelPagerAdapter getDialogPanelAdapter() { return dialogPanelAdapter; }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2026 && projectTreeManager != null) {
            projectTreeManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void setEditorActiveState(boolean isFileActive) {
        runOnUiThread(() -> {
            if (emptyStateView == null || codeEditor == null) return;
            if (isFileActive) {
                emptyStateView.setVisibility(View.GONE);
                codeEditor.setVisibility(View.VISIBLE);
            } else {
                codeEditor.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.VISIBLE);
                if (tvFilePath != null) tvFilePath.setText("No file open");
            }
        });
    }

    // 🤖 สะพานเชื่อมแบบรวมศูนย์ตัวจริงตัวเดียว (ปรับปรุงให้รองรับ JavaScript เรียกใช้งานได้ชัวร์)
    public class WebAppInterface {
        Context mContext;

        public WebAppInterface(Context c) {
            this.mContext = c;
        }

        // ปุ่ม 1: คัดลอกข้อความซอร์สโค้ดธรรมดาลงคลิปบอร์ด Android
        @android.webkit.JavascriptInterface
        public void copyToSystemClipboard(final String text) {
            runOnUiThread(() -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("MiniStudioCode", text);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    showToast("📋 คัดลอกโค้ดลงคลิปบอร์ดแล้วครับน้า!");
                }
            });
        }

        // ปุ่ม 2: วางโค้ดพุ่งเข้าหา Sora CodeEditor โดยตรง
        @android.webkit.JavascriptInterface
        public void insertCodeIntoEditor(final String codeFromAi) {
            runOnUiThread(() -> {
                if (codeEditor != null) {
                    codeEditor.setText(codeFromAi);
                    
                    // ปิดเสียง AI ทันทีเมื่อกดยอมรับโค้ดไปใช้งาน
                    if (aiLayoutAnalyzer != null) {
                        aiLayoutAnalyzer.stopSpeaking();
                    }
                    if (fullPanelDialog != null && fullPanelDialog.isShowing()) {
                        fullPanelDialog.dismiss();
                    }
                    showToast("✨ นำโค้ดเข้าสู่หน้าแก้ไขเรียบร้อยแล้วครับน้า!");
                }
            });
        }
    }

    // คลาสเมนูต้นไม้
    public static class MenuOption {
        public String title;
        public int iconRes;
        public MenuOption(String title, int iconRes) {
            this.title = title;
            this.iconRes = iconRes;
        }
    }

    @Override
    protected void onDestroy() {
        if (aiLayoutAnalyzer != null) {
            aiLayoutAnalyzer.shutdown(); 
        }
        super.onDestroy();
    }
// ฟังก์ชันกดปุ๊บ วาร์ปปั๊บ ไปยังตำแหน่งที่โค้ด Error (ฉบับปรับปรุงแก้อาการสัญลักษณ์หาย)
public void jumpToErrorLocation(String fileName, int lineNumber) {
    runOnUiThread(() -> {
        // 1. สั่งซ่อนแผงคอนโซลลงไปก่อนเพื่อคืนพื้นที่ให้หน้าจอแก้ไขโค้ด
        View consolePanel = findViewById(R.id.consolePanel);
        if (consolePanel != null) consolePanel.setVisibility(View.GONE);

        // 2. ลอจิกการสั่งเปิดไฟล์ .java ที่พังขึ้นกระดาน (อิงตามระบบเปิดไฟล์หลักของน้า)
        if (projectTreeManager != null && currentProject != null) {
            // เดินสายหาตำแหน่งไฟล์จริงในโปรเจกต์แล้วบังคับให้ระบบ Tab โหลดขึ้นมาทำงาน
            java.io.File fileToOpen = projectTreeManager.findFileInProject(currentProject.getRootPath(), fileName);
            if (fileToOpen != null && fileToOpen.exists()) {
                openFile(fileToOpen); // ใช้ฟังก์ชันเปิดไฟล์หลักของน้า
            }
        }

        // 3. ปรับโค้ดคำสั่งวาร์ปเคอร์เซอร์ให้ตรงกับ Sora Editor API ของเครื่องน้าครับ
        if (codeEditor != null) {
            int targetLine = Math.max(0, lineNumber - 1);
            // สั่งขยับตำแหน่งและเลื่อนหน้าจอฉบับตรงรุ่น
            codeEditor.getCursor().setLeft(targetLine, 0);
            codeEditor.getCursor().setRight(targetLine, 0);
            codeEditor.ensurePositionVisible(targetLine, 0);
            
            showToast("🔍 วาร์ปมาบรรทัดที่ " + lineNumber + " ให้แล้ว!");
        }
    });
}

private void pushChangesToGithub(String projectName) {
    if (projectName == null || projectName.isEmpty()) {
        showToast("⚠️ ไม่พบชื่อโปรเจกต์สำหรับทำการ Push");
        return;
    }

    this.pendingProjectName = projectName;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            showToast("⚠️ กรุณากดอนุญาตการแจ้งเตือน เพื่อให้เห็นแถบความคืบหน้านะครับ");
            return;
        }
    }

    startActualPushService(projectName);
}

@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 101) {
        if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            showToast("✅ อนุญาตสิทธิ์แล้ว กำลังเริ่มอัปโหลด...");
            if (!pendingProjectName.isEmpty()) {
                startActualPushService(pendingProjectName);
            }
        } else {
            showToast("❌ คุณปฏิเสธสิทธิ์การแจ้งเตือน ทำให้ไม่สามารถแสดงความคืบหน้าได้");
        }
    }
}

private void startActualPushService(String projectName) {
    File projectDir = new File("/sdcard/MiniStudio/" + projectName);
    if (!projectDir.exists()) {
        showToast("❌ ไม่พบโฟลเดอร์โปรเจกต์");
        return;
    }

    SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
    String username = prefs.getString("username", "");
    String token = prefs.getString("github_token", "");
    if (token.isEmpty()) token = prefs.getString("token", "");

    if (username.isEmpty() || token.isEmpty()) {
        showToast("❌ กรุณาตั้งค่า Username และ GitHub Token ก่อน");
        return;
    }

    String repoUrl = "https://github.com/" + username + "/" + projectName + ".git";

    Intent serviceIntent = new Intent(this, GitHubPushService.class);
    serviceIntent.putExtra("projectName", projectName);
    serviceIntent.putExtra("username", username);
    serviceIntent.putExtra("token", token);
    serviceIntent.putExtra("repoUrl", repoUrl);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(serviceIntent);
    } else {
        startService(serviceIntent);
    }

    Toast.makeText(this, "📥 เริ่มอัปโหลดแล้ว! รูดหน้าจอลงมาดู % บน Status Bar ได้เลยครับ", Toast.LENGTH_LONG).show();
}

 }
