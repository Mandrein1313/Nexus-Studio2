package com.dev.ministudio;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class NewProjectActivity extends AppCompatActivity {

    private RecyclerView rvTemplates;
    private View configRoot;
    private ProjectTemplate selectedTemplate;
    private String selectedLanguage = "Java";

    private final List<ProjectTemplate> templates = Arrays.asList(
            new ProjectTemplate("no_activity", "No Activity", "โปรเจกต์ว่าง ไม่มี Activity", 0xFF546E7A),
            new ProjectTemplate("empty", "Empty project", "MainActivity + layout ว่าง", 0xFF00897B),
            new ProjectTemplate("basic", "Basic Project", "MainActivity + FAB", 0xFF00897B),
            new ProjectTemplate("nav_drawer", "Navigation drawer", "Drawer + NavigationView", 0xFF00897B),
            new ProjectTemplate("bottom_nav", "Bottom Navigation", "BottomNav + Fragments", 0xFF5C6BC0),
            new ProjectTemplate("tabs", "Tabbed Activity", "TabLayout + ViewPager2", 0xFF26A69A)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.parseColor("#0D0E14"));
        getWindow().setNavigationBarColor(Color.parseColor("#0D0E14"));
        setContentView(R.layout.activity_new_project);

        rvTemplates = findViewById(R.id.rvTemplates);
        rvTemplates.setLayoutManager(new GridLayoutManager(this, 2));
        rvTemplates.setAdapter(new TemplateAdapter());

        findViewById(R.id.btnExit).setOnClickListener(v -> finish());
    }

    /** จัดการการกดปุ่ม Back ของเครื่อง */
    @Override
    public void onBackPressed() {
        if (configRoot != null && configRoot.getParent() != null) {
            showGridScreen();
        } else {
            super.onBackPressed();
        }
    }

    /** เรียกเมื่อกดเลือก Template */
    private void openTemplateConfig(ProjectTemplate template) {
        this.selectedTemplate = template;
        this.selectedLanguage = "Java";
        showConfigScreen();
    }

    private void showConfigScreen() {
        if (selectedTemplate == null) return;

        final float d = getResources().getDisplayMetrics().density;

        // ซ่อน grid, ปุ่ม exit และหัวข้อหลัก
        if (rvTemplates != null) rvTemplates.setVisibility(View.GONE);
        View exitBtn = findViewById(R.id.btnExit);
        if (exitBtn != null) exitBtn.setVisibility(View.GONE);
        View tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setVisibility(View.GONE);

        // สร้างหน้า config ทั้งจอ
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0D0E14"));
        root.setPadding((int) (20 * d), (int) (12 * d), (int) (20 * d), (int) (20 * d));
        configRoot = root;

        // ===== ปุ่มกลับ =====
        TextView btnBack = new TextView(this);
        btnBack.setText("←  Templates");
        btnBack.setTextColor(Color.parseColor("#A9B1D6"));
        btnBack.setTextSize(15);
        btnBack.setPadding(0, (int) (8 * d), 0, (int) (16 * d));
        btnBack.setOnClickListener(v -> showGridScreen());
        root.addView(btnBack);

        // ===== แถว Preview + ข้อมูล Template =====
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        // การ์ด Preview
        FrameLayout previewCard = new FrameLayout(this);
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#1A1B26"));
        cardBg.setCornerRadius(14 * d);
        cardBg.setStroke((int) d, Color.parseColor("#292E42"));
        previewCard.setBackground(cardBg);

        View topBar = new View(this);
        topBar.setBackgroundColor(selectedTemplate.previewColor);
        previewCard.addView(topBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (26 * d)));

        LinearLayout previewContent = new LinearLayout(this);
        previewContent.setOrientation(LinearLayout.VERTICAL);
        previewContent.setBackgroundColor(Color.WHITE);
        FrameLayout.LayoutParams contentLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        contentLp.topMargin = (int) (26 * d);
        previewCard.addView(previewContent, contentLp);
        drawPreview(previewContent, selectedTemplate.id);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                (int) (110 * d), (int) (130 * d));
        headerRow.addView(previewCard, cardLp);

        // ข้อความด้านขวา
        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        infoCol.setPadding((int) (16 * d), 0, 0, 0);

        TextView tvName = new TextView(this);
        tvName.setText(selectedTemplate.name);
        tvName.setTextColor(Color.parseColor("#C0CAF5"));
        tvName.setTextSize(20);
        tvName.setTypeface(null, Typeface.BOLD);
        infoCol.addView(tvName);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(selectedTemplate.description);
        tvDesc.setTextColor(Color.parseColor("#565F89"));
        tvDesc.setTextSize(13);
        tvDesc.setPadding(0, (int) (4 * d), 0, (int) (6 * d));
        infoCol.addView(tvDesc);

        TextView tvFiles = new TextView(this);
        tvFiles.setText(getFileCountText(selectedTemplate.id));
        tvFiles.setTextColor(Color.parseColor("#7AA2F7"));
        tvFiles.setTextSize(12);
        infoCol.addView(tvFiles);

        headerRow.addView(infoCol, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(headerRow);

        // ระยะห่าง
        View spacer1 = new View(this);
        root.addView(spacer1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (28 * d)));

        // ===== Application name =====
        root.addView(makeLabel("Application name"));
        final EditText etAppName = makeInput("MyApplication");
        etAppName.setText("MyApplication");
        root.addView(etAppName, fieldLp(d));

        // ===== Package name =====
        root.addView(makeLabel("Package name"));
        final EditText etPackage = makeInput("com.example.myapplication");
        etPackage.setText("com.example.myapplication");
        root.addView(etPackage, fieldLp(d));

        // Auto change package name on app name edit
        etAppName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String clean = s.toString().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                if (!clean.isEmpty()) etPackage.setText("com.example." + clean);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // ===== Language =====
        root.addView(makeLabel("Language"));
        LinearLayout langRow = new LinearLayout(this);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        langRow.setPadding(0, (int) (6 * d), 0, (int) (4 * d));

        final TextView btnJava = makeLangButton("Java", true, d);
        final TextView btnKotlin = makeLangButton("Kotlin", false, d);

        btnJava.setOnClickListener(v -> {
            selectedLanguage = "Java";
            styleLangButton(btnJava, true, d);
            styleLangButton(btnKotlin, false, d);
        });
        btnKotlin.setOnClickListener(v -> {
            selectedLanguage = "Kotlin";
            styleLangButton(btnJava, false, d);
            styleLangButton(btnKotlin, true, d);
        });

        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(
                0, (int) (42 * d), 1f);
        half.rightMargin = (int) (8 * d);
        langRow.addView(btnJava, half);
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(
                0, (int) (42 * d), 1f);
        langRow.addView(btnKotlin, half2);
        root.addView(langRow);

        // ===== Minimum SDK =====
        View spacer2 = new View(this);
        root.addView(spacer2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (16 * d)));

        root.addView(makeLabel("Minimum SDK"));
        final Spinner spinSdk = new Spinner(this);
        GradientDrawable sdkBg = new GradientDrawable();
        sdkBg.setColor(Color.parseColor("#1A1B26"));
        sdkBg.setCornerRadius(12 * d);
        sdkBg.setStroke((int) d, Color.parseColor("#3B4261"));
        spinSdk.setBackground(sdkBg);
        spinSdk.setPadding((int) (14 * d), (int) (10 * d), (int) (14 * d), (int) (10 * d));

        String[] sdks = {
                "API 21 (Lollipop)",
                "API 23 (Marshmallow)",
                "API 24 (Nougat)",
                "API 26 (Oreo)",
                "API 29 (Android 10)",
                "API 33 (Android 13)"
        };
        ArrayAdapter<String> sdkAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, sdks) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.parseColor("#C0CAF5"));
                tv.setTextSize(14);
                return tv;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Color.parseColor("#C0CAF5"));
                tv.setBackgroundColor(Color.parseColor("#1A1B26"));
                int p = (int) (14 * d);
                tv.setPadding(p, p, p, p);
                return tv;
            }
        };
        sdkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinSdk.setAdapter(sdkAdapter);
        spinSdk.setSelection(2); // ค่าเริ่มต้น API 24
        LinearLayout.LayoutParams sdkLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sdkLp.topMargin = (int) (6 * d);
        root.addView(spinSdk, sdkLp);

        // spacer ดันปุ่ม Create project ลงด้านล่างสุด
        View flex = new View(this);
        root.addView(flex, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // ===== ปุ่ม Create project =====
        TextView btnCreate = new TextView(this);
        btnCreate.setText("Create project");
        btnCreate.setTextColor(Color.WHITE);
        btnCreate.setTextSize(16);
        btnCreate.setTypeface(null, Typeface.BOLD);
        btnCreate.setGravity(Gravity.CENTER);
        GradientDrawable createBg = new GradientDrawable();
        createBg.setColor(Color.parseColor("#00897B"));
        createBg.setCornerRadius(14 * d);
        btnCreate.setBackground(createBg);
        btnCreate.setPadding(0, (int) (16 * d), 0, (int) (16 * d));

        btnCreate.setOnClickListener(v -> {
            String appName = etAppName.getText().toString().trim();
            String pkg = etPackage.getText().toString().trim();
            String sdkStr = (String) spinSdk.getSelectedItem();

            if (appName.isEmpty()) {
                Toast.makeText(this, "กรุณาใส่ชื่อแอป", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pkg.isEmpty() || !pkg.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+")) {
                Toast.makeText(this, "Package name ไม่ถูกต้อง", Toast.LENGTH_SHORT).show();
                return;
            }
            if (new File("/sdcard/MiniStudio/" + appName).exists()) {
                Toast.makeText(this, "มีโปรเจกต์ชื่อนี้อยู่แล้ว", Toast.LENGTH_SHORT).show();
                return;
            }

            int minSdk = 24;
            try {
                minSdk = Integer.parseInt(sdkStr.replaceAll("[^0-9]", "").substring(0, 2));
            } catch (Exception ignored) {}

            createAndOpen(appName, pkg, selectedTemplate.id, selectedLanguage, minSdk);
        });

        root.addView(btnCreate, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ใส่เข้า Window content
        ViewGroup parent = (ViewGroup) findViewById(android.R.id.content);
        if (configRoot != null && configRoot.getParent() != null) {
            ((ViewGroup) configRoot.getParent()).removeView(configRoot);
        }
        parent.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void showGridScreen() {
        if (configRoot != null && configRoot.getParent() != null) {
            ((ViewGroup) configRoot.getParent()).removeView(configRoot);
            configRoot = null;
        }
        if (rvTemplates != null) rvTemplates.setVisibility(View.VISIBLE);
        View exitBtn = findViewById(R.id.btnExit);
        if (exitBtn != null) exitBtn.setVisibility(View.VISIBLE);
        View tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setVisibility(View.VISIBLE);
    }

    private void createAndOpen(String projectName, String packageName,
                               String templateId, String language, int minSdk) {
        Toast.makeText(this, "กำลังสร้างโปรเจกต์...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            boolean ok = ProjectGenerator.create(projectName, packageName, templateId, language, minSdk);
            runOnUiThread(() -> {
                if (ok) {
                    Toast.makeText(this, "✅ สร้าง \"" + projectName + "\" สำเร็จ", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("projectName", projectName);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "❌ สร้างโปรเจกต์ไม่สำเร็จ", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    // ===== Helper UI Components =====
    private TextView makeLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#A9B1D6"));
        tv.setTextSize(13);
        tv.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        return tv;
    }

    private EditText makeInput(String hint) {
        float d = getResources().getDisplayMetrics().density;
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(Color.parseColor("#565F89"));
        et.setTextColor(Color.parseColor("#C0CAF5"));
        et.setTextSize(15);
        et.setSingleLine(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1A1B26"));
        bg.setCornerRadius(12 * d);
        bg.setStroke((int) d, Color.parseColor("#3B4261"));
        et.setBackground(bg);
        et.setPadding((int) (16 * d), (int) (14 * d), (int) (16 * d), (int) (14 * d));
        return et;
    }

    private LinearLayout.LayoutParams fieldLp(float d) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (16 * d);
        return lp;
    }

    private TextView makeLangButton(String text, boolean selected, float d) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setGravity(Gravity.CENTER);
        btn.setTextSize(14);
        styleLangButton(btn, selected, d);
        return btn;
    }

    private void styleLangButton(TextView btn, boolean selected, float d) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12 * d);
        if (selected) {
            bg.setColor(Color.parseColor("#0D4F4A"));
            bg.setStroke((int) (1.5f * d), Color.parseColor("#00897B"));
            btn.setTextColor(Color.parseColor("#80CBC4"));
        } else {
            bg.setColor(Color.parseColor("#1A1B26"));
            bg.setStroke((int) d, Color.parseColor("#3B4261"));
            btn.setTextColor(Color.parseColor("#565F89"));
        }
        btn.setBackground(bg);
    }

    private String getFileCountText(String templateId) {
        switch (templateId) {
            case "no_activity": return "8 files";
            case "empty": return "12 files";
            case "basic": return "17 files";
            case "nav_drawer": return "22 files";
            case "bottom_nav": return "24 files";
            case "tabs": return "20 files";
            default: return "15 files";
        }
    }

    // ===== Adapter =====
    private class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_project_template, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            ProjectTemplate t = templates.get(position);
            h.tvName.setText(t.name);
            h.topBar.setBackgroundColor(t.previewColor);

            h.previewContent.removeAllViews();
            drawPreview(h.previewContent, t.id);

            h.itemView.setOnClickListener(v -> openTemplateConfig(t));
        }

        @Override
        public int getItemCount() {
            return templates.size();
        }

        class VH extends RecyclerView.ViewHolder {
            View topBar;
            LinearLayout previewContent;
            TextView tvName;

            VH(View itemView) {
                super(itemView);
                topBar = itemView.findViewById(R.id.previewTopBar);
                previewContent = itemView.findViewById(R.id.previewContent);
                tvName = itemView.findViewById(R.id.tvTemplateName);
            }
        }
    }

    private void drawPreview(LinearLayout content, String id) {
        float d = getResources().getDisplayMetrics().density;
        content.setBackgroundColor(Color.WHITE);

        switch (id) {
            case "no_activity":
                TextView dash = new TextView(this);
                dash.setText("┄┄┄");
                dash.setTextColor(0xFFBDBDBD);
                dash.setTextSize(18);
                dash.setGravity(Gravity.CENTER);
                content.addView(dash, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                break;

            case "empty":
                break;

            case "basic":
                FrameLayout fabHolder = new FrameLayout(this);
                fabHolder.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                View fab = new View(this);
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.OVAL);
                gd.setColor(0xFFFFC107);
                fab.setBackground(gd);
                FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(
                        (int) (28 * d), (int) (28 * d));
                fp.gravity = Gravity.BOTTOM | Gravity.END;
                fp.setMargins(0, 0, (int) (10 * d), (int) (10 * d));
                fabHolder.addView(fab, fp);
                content.addView(fabHolder);
                break;

            case "nav_drawer":
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                View drawer = new View(this);
                drawer.setBackgroundColor(0xFFEEEEEE);
                row.addView(drawer, new LinearLayout.LayoutParams(
                        (int) (40 * d), ViewGroup.LayoutParams.MATCH_PARENT));

                LinearLayout lines = new LinearLayout(this);
                lines.setOrientation(LinearLayout.VERTICAL);
                lines.setPadding((int) (8 * d), (int) (12 * d), 0, 0);
                for (int i = 0; i < 4; i++) {
                    View line = new View(this);
                    line.setBackgroundColor(0xFFBDBDBD);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            (int) (50 * d), (int) (6 * d));
                    lp.bottomMargin = (int) (8 * d);
                    lines.addView(line, lp);
                }
                row.addView(lines, new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.MATCH_PARENT, 1f));
                content.addView(row);
                break;

            case "bottom_nav":
                LinearLayout col = new LinearLayout(this);
                col.setOrientation(LinearLayout.VERTICAL);
                col.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                View space = new View(this);
                col.addView(space, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

                LinearLayout bottom = new LinearLayout(this);
                bottom.setOrientation(LinearLayout.HORIZONTAL);
                bottom.setBackgroundColor(0xFFEEEEEE);
                bottom.setGravity(Gravity.CENTER);
                for (int i = 0; i < 3; i++) {
                    View dot = new View(this);
                    GradientDrawable c = new GradientDrawable();
                    c.setShape(GradientDrawable.OVAL);
                    c.setColor(0xFF9E9E9E);
                    dot.setBackground(c);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            (int) (14 * d), (int) (14 * d));
                    lp.setMargins((int) (10 * d), (int) (8 * d), (int) (10 * d), (int) (8 * d));
                    bottom.addView(dot, lp);
                }
                col.addView(bottom, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) (36 * d)));
                content.addView(col);
                break;

            case "tabs":
                LinearLayout tabCol = new LinearLayout(this);
                tabCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout tabs = new LinearLayout(this);
                tabs.setOrientation(LinearLayout.HORIZONTAL);
                tabs.setBackgroundColor(0xFFE0F2F1);
                for (int i = 0; i < 3; i++) {
                    View tab = new View(this);
                    tab.setBackgroundColor(i == 0 ? 0xFF00897B : 0xFFB2DFDB);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            0, (int) (8 * d), 1f);
                    lp.setMargins((int) (4 * d), (int) (10 * d), (int) (4 * d), 0);
                    tabs.addView(tab, lp);
                }
                tabCol.addView(tabs, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) (28 * d)));
                content.addView(tabCol, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                break;
        }
    }
}
