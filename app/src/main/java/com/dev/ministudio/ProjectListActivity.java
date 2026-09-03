package com.dev.ministudio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.graphics.drawable.GradientDrawable;
import android.content.IntentFilter;

public class ProjectListActivity extends AppCompatActivity {
    private ArrayList<String> projects = new ArrayList<>();
    private DrawerLayout drawerLayout;
    private FloatingActionsMenu fabMenu;
    private FloatingActionButton fabCreate;
    private FloatingActionButton fabGithub;

    // View สำหรับการแสดงรายการแบบแถวตามข้อกำหนดใหม่
    private LinearLayout projectRowsContainer;
    private TextView tvNoProjects;

    private final android.content.BroadcastReceiver cloneReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshProjectList();
            updateProjectEmptyState();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(cloneReceiver);
        } catch (Exception e) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // กันเนื้อหาไม่ให้ทับ status bar / navigation bar (Android 15+)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#1A1B26"));
        getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#1A1B26"));
        setContentView(R.layout.activity_project_list);

        // ผูก View Container และ Views ใหม่
        projectRowsContainer = findViewById(R.id.projectRowsContainer);
        tvNoProjects = findViewById(R.id.tvNoProjects);

        // ผูก Event Click รายการแถวเมนูการตั้งค่าแบบใหม่
        View rowNewProject = findViewById(R.id.rowNewProject);
        if (rowNewProject != null) rowNewProject.setOnClickListener(v -> showCreateProjectDialog());

        View rowImportGithub = findViewById(R.id.rowImportGithub);
        if (rowImportGithub != null) rowImportGithub.setOnClickListener(v -> importFromGitHub());

        View rowAiSettings = findViewById(R.id.rowAiSettings);
        if (rowAiSettings != null) rowAiSettings.setOnClickListener(v ->
                startActivity(new Intent(this, AiSettingsActivity.class)));

        View rowGithubSettings = findViewById(R.id.rowGithubSettings);
        if (rowGithubSettings != null) rowGithubSettings.setOnClickListener(v -> showGitHubSettingsDialog());

        View rowToggleTheme = findViewById(R.id.rowToggleTheme);
        if (rowToggleTheme != null) rowToggleTheme.setOnClickListener(v -> toggleEditorThemePref());

        View rowAbout = findViewById(R.id.rowAbout);
        if (rowAbout != null) rowAbout.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Nexus Studio")
                        .setMessage("Mobile Android IDE\nเขียน แก้ บิลด์แอปได้จากมือถือ")
                        .setPositiveButton("ตกลง", null)
                        .show());

        Toolbar toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);

        fabMenu = findViewById(R.id.multiple_actions);
        fabCreate = findViewById(R.id.action_create);
        fabGithub = findViewById(R.id.action_github);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (drawerLayout != null && toolbar != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar, android.R.string.ok, android.R.string.cancel);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        com.google.android.material.navigation.NavigationView navView = findViewById(R.id.nav_view);
        if (navView != null) {
            int statusBarHeight = 0;
            int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resId > 0) {
                statusBarHeight = getResources().getDimensionPixelSize(resId);
            }
            navView.setPadding(0, statusBarHeight, 0, 0);

            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_github_settings) {
                    showGitHubSettingsDialog();
                } else if (id == R.id.nav_ai_settings) {
                    startActivity(new Intent(this, AiSettingsActivity.class));
                } else if (id == R.id.nav_toggle_theme) {
                    toggleEditorThemePref();
                } else if (id == R.id.nav_about) {
                    new AlertDialog.Builder(this)
                            .setTitle("Nexus Studio")
                            .setMessage("Mobile Android IDE\nเขียน แก้ บิลด์แอปได้จากมือถือ")
                            .setPositiveButton("ตกลง", null)
                            .show();
                }
                if (drawerLayout != null) drawerLayout.closeDrawers();
                return true;
            });
        }

        setupFabButtons();
        checkPermissions();

        // โหลดข้อมูลและแสดงผลแถวโปรเจกต์
        refreshProjectList();
        updateProjectEmptyState();

        SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("is_github_setup", false)) {
            new android.os.Handler().postDelayed(this::showGitHubSettingsDialog, 600);
        }

        IntentFilter filter = new IntentFilter(GitHubCloneService.ACTION_CLONE_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(cloneReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(cloneReceiver, filter);
        }
    }

    private void setupFabButtons() {
        if (fabCreate != null) {
            fabCreate.setOnClickListener(v -> {
                showCreateProjectDialog();
                if (fabMenu != null) fabMenu.collapse();
            });
        }

        if (fabGithub != null) {
            fabGithub.setOnClickListener(v -> {
                importFromGitHub();
                if (fabMenu != null) fabMenu.collapse();
            });
        }
    }

    private void importFromGitHub() {
        final EditText etUrl = new EditText(this);
        etUrl.setHint("https://github.com/user/repository.git");
        etUrl.setPadding(40, 40, 40, 40);
        etUrl.setTextColor(android.graphics.Color.WHITE);

        new AlertDialog.Builder(this)
            .setTitle("นำเข้าโปรเจกต์จาก GitHub")
            .setMessage("ระบบจะดึงเฉพาะ Commit ล่าสุด (Shallow Clone) เพื่อความรวดเร็ว")
            .setView(etUrl)
            .setPositiveButton("ดาวน์โหลด", (dialog, which) -> {
                String url = etUrl.getText().toString().trim();
                if (url.isEmpty()) {
                    Toast.makeText(this, "กรุณาใส่ลิงก์ก่อนครับ", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String projectName = extractRepoName(url);
                if (projectName == null || projectName.isEmpty()) {
                    projectName = "Import_" + System.currentTimeMillis();
                }
                
                downloadAndImportProject(url, projectName);
            })
            .setNegativeButton("ยกเลิก", null)
            .show();
    }

    private String extractRepoName(String url) {
        try {
            String cleanUrl = url.trim();
            if (cleanUrl.endsWith("/")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
            }
            if (cleanUrl.endsWith(".git")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 4);
            }
            return cleanUrl.substring(cleanUrl.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return null;
        }
    }

    private void updateProjectEmptyState() {
        boolean empty = projects == null || projects.isEmpty();
        if (projectRowsContainer != null) {
            projectRowsContainer.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
        if (tvNoProjects != null) {
            tvNoProjects.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    // ========== Project Management ==========

    private void refreshProjectList() {
        projects.clear();
        File root = new File("/sdcard/MiniStudio");
        if (!root.exists()) root.mkdirs();
        File[] files = root.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory() && !isProjectIgnored(f.getName())) {
                    projects.add(f.getName());
                }
            }
        }
        renderProjectRows();
    }

    private void renderProjectRows() {
        if (projectRowsContainer == null) return;
        projectRowsContainer.removeAllViews();

        if (projects.isEmpty()) {
            if (tvNoProjects != null) tvNoProjects.setVisibility(View.VISIBLE);
            return;
        }
        if (tvNoProjects != null) tvNoProjects.setVisibility(View.GONE);

        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < projects.size(); i++) {
            final String name = projects.get(i);

            TextView row = new TextView(this);
            row.setText("📁    " + name);
            row.setTextColor(Color.parseColor("#C0CAF5"));
            row.setTextSize(15);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, (int) (14 * d), 0, (int) (14 * d));

            TypedValue out = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, out, true);
            row.setBackgroundResource(out.resourceId);

            row.setOnClickListener(v -> {
                Intent intent = new Intent(ProjectListActivity.this, MainActivity.class);
                intent.putExtra("projectName", name);
                startActivity(intent);
            });

            row.setOnLongClickListener(v -> {
                confirmDeleteProject(name);
                return true;
            });

            projectRowsContainer.addView(row);
        }
    }

    private void confirmDeleteProject(String projectName) {
        File projectDir = new File("/sdcard/MiniStudio/" + projectName);
        new AlertDialog.Builder(this)
                .setTitle("ลบโปรเจกต์")
                .setMessage("ลบ \"" + projectName + "\" หรือไม่?")
                .setPositiveButton("ลบ", (d, w) -> {
                    deleteRecursive(projectDir);
                    refreshProjectList();
                    updateProjectEmptyState();
                    Toast.makeText(this, "ลบแล้ว", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ยกเลิก", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProjectList();
        updateProjectEmptyState();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                checkFilePermission();
            }
        } else {
            checkFilePermission();
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                return;
            }
        }
        checkFilePermission();
    }

    private void checkFilePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    // ========== Create Project Dialog ==========

    private void showCreateProjectDialog() {
        final float density = getResources().getDisplayMetrics().density;
        int dp = (int) (1 * density);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#1F2335"));
        inputBg.setCornerRadius(12 * density);
        inputBg.setStroke(dp, Color.parseColor("#3B4261"));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#24283B"));
        cardBg.setCornerRadius(16 * density);
        cardBg.setStroke(dp, Color.parseColor("#3B4261"));

        GradientDrawable createBtnBg = new GradientDrawable();
        createBtnBg.setColor(Color.parseColor("#7AA2F7"));
        createBtnBg.setCornerRadius(12 * density);

        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setColor(Color.parseColor("#1A1B26"));
        dialogBg.setCornerRadius(20 * density);

        int pad = (int) (16 * density);
        int fieldPad = (int) (14 * density);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.parseColor("#1A1B26"));
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("✨ สร้างโปรเจกต์ใหม่");
        tvTitle.setTextColor(Color.parseColor("#C0CAF5"));
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        root.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("กำหนดชื่อ แพ็กเกจ ภาษา และ Minimum SDK");
        tvDesc.setTextColor(Color.parseColor("#565F89"));
        tvDesc.setTextSize(13);
        tvDesc.setPadding(0, (int) (4 * density), 0, (int) (18 * density));
        root.addView(tvDesc);

        LinearLayout cardInfo = new LinearLayout(this);
        cardInfo.setOrientation(LinearLayout.VERTICAL);
        cardInfo.setPadding(pad, pad, pad, pad);
        cardInfo.setBackground(cardBg);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = (int) (12 * density);

        TextView lbName = sectionLabel("ชื่อโปรเจกต์");
        cardInfo.addView(lbName);

        final EditText etProjectName = styledEdit(inputBg, fieldPad, "เช่น MyGame");
        cardInfo.addView(etProjectName, fieldParams(density));

        TextView lbPkg = sectionLabel("Package Name");
        lbPkg.setPadding(0, (int) (10 * density), 0, (int) (6 * density));
        cardInfo.addView(lbPkg);

        final EditText etPackageName = styledEdit(inputBg, fieldPad, "com.example.mygame");
        cardInfo.addView(etPackageName, fieldParams(density));

        etProjectName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String name = s.toString().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                etPackageName.setText(name.isEmpty() ? "" : "com.example." + name);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        root.addView(cardInfo, cardLp);

        LinearLayout cardEnv = new LinearLayout(this);
        cardEnv.setOrientation(LinearLayout.VERTICAL);
        cardEnv.setPadding(pad, pad, pad, pad);
        cardEnv.setBackground(cardBg.getConstantState() != null
                ? cardBg.getConstantState().newDrawable() : cardBg);

        TextView lbLang = sectionLabel("ภาษา");
        cardEnv.addView(lbLang);

        final Spinner spinLanguage = styledSpinner(inputBg, fieldPad,
                new String[]{"Java", "Kotlin"});
        cardEnv.addView(spinLanguage, fieldParams(density));

        TextView lbSdk = sectionLabel("Minimum SDK");
        lbSdk.setPadding(0, (int) (10 * density), 0, (int) (6 * density));
        cardEnv.addView(lbSdk);

        final Spinner spinMinSdk = styledSpinner(inputBg, fieldPad, new String[]{
                "API 21 · Android 5.0 (Lollipop)",
                "API 23 · Android 6.0 (Marshmallow)",
                "API 26 · Android 8.0 (Oreo)",
                "API 29 · Android 10",
                "API 33 · Android 13"
        });
        spinMinSdk.setSelection(1);
        cardEnv.addView(spinMinSdk, fieldParams(density));

        root.addView(cardEnv, cardLp);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(android.view.Gravity.END);
        buttons.setPadding(0, (int) (8 * density), 0, 0);

        Button btnCancel = new Button(this, null, 0, android.R.style.Widget_Material_Button_Borderless);
        btnCancel.setText("ยกเลิก");
        btnCancel.setAllCaps(false);
        btnCancel.setTextColor(Color.parseColor("#A9B1D6"));
        btnCancel.setTextSize(14);
        buttons.addView(btnCancel);

        Button btnCreate = new Button(this);
        btnCreate.setText("สร้างโปรเจกต์");
        btnCreate.setAllCaps(false);
        btnCreate.setTextColor(Color.parseColor("#1A1B26"));
        btnCreate.setTextSize(14);
        btnCreate.setTypeface(Typeface.DEFAULT_BOLD);
        btnCreate.setBackground(createBtnBg);
        btnCreate.setPadding((int) (20 * density), 0, (int) (20 * density), 0);
        LinearLayout.LayoutParams createLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) (44 * density));
        createLp.leftMargin = (int) (8 * density);
        buttons.addView(btnCreate, createLp);

        root.addView(buttons);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(dialogBg);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnCreate.setOnClickListener(v -> {
            String projectName = etProjectName.getText().toString().trim();
            String packageName = etPackageName.getText().toString().trim();
            String language = (String) spinLanguage.getSelectedItem();
            String minSdk = (String) spinMinSdk.getSelectedItem();

            if (projectName.isEmpty()) {
                Toast.makeText(this, "กรุณาใส่ชื่อโปรเจกต์", Toast.LENGTH_SHORT).show();
                return;
            }
            if (packageName.isEmpty()) {
                Toast.makeText(this, "กรุณาใส่ Package Name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (new File("/sdcard/MiniStudio/" + projectName).exists()) {
                Toast.makeText(this, "มีโปรเจกต์ชื่อนี้อยู่แล้ว", Toast.LENGTH_SHORT).show();
                return;
            }

            createNewProject(projectName, packageName,
                    language != null ? language : "Java",
                    minSdk != null ? minSdk : "API 23");
            refreshProjectList();
            updateProjectEmptyState();
            Toast.makeText(this, "สร้างโปรเจกต์ " + projectName + " แล้ว", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#A9B1D6"));
        tv.setTextSize(12);
        tv.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        return tv;
    }

    private EditText styledEdit(GradientDrawable bg, int pad, String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(Color.parseColor("#565F89"));
        et.setTextColor(Color.parseColor("#C0CAF5"));
        et.setTextSize(14);
        et.setSingleLine(true);
        et.setBackground(bg.getConstantState() != null
                ? bg.getConstantState().newDrawable() : bg);
        et.setPadding(pad, pad, pad, pad);
        return et;
    }

    private Spinner styledSpinner(GradientDrawable bg, int pad, String[] items) {
        Spinner sp = new Spinner(this);
        sp.setBackground(bg.getConstantState() != null
                ? bg.getConstantState().newDrawable() : bg);
        sp.setPadding(pad, pad / 2, pad, pad / 2);

        ArrayAdapter<String> ad = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, items) {
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
                tv.setBackgroundColor(Color.parseColor("#24283B"));
                int p = (int) (14 * getResources().getDisplayMetrics().density);
                tv.setPadding(p, p, p, p);
                return tv;
            }
        };
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        return sp;
    }

    private LinearLayout.LayoutParams fieldParams(float density) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (4 * density);
        return lp;
    }

    // ========== Create Project ==========

    private void createNewProject(String projectName, String packageName, String language, String minSdkVersionString) {
        String rootPath = "/sdcard/MiniStudio/" + projectName;
        
        String langFolder = language.toLowerCase();
        String sourceDirPath = rootPath + "/app/src/main/" + langFolder + "/" + packageName.replace(".", "/");
        
        String[] folders = {
            sourceDirPath,
            rootPath + "/app/src/main/res/layout",
            rootPath + "/app/src/main/res/values",
            rootPath + "/app/src/main/res/drawable",
            rootPath + "/app/src/main/res/mipmap-hdpi",
            rootPath + "/app/src/main/res/mipmap-mdpi",
            rootPath + "/app/src/main/res/mipmap-xhdpi",
            rootPath + "/app/src/main/res/mipmap-xxhdpi",
            rootPath + "/app/src/main/res/mipmap-xxxhdpi"
        };

        for (String path : folders) {
            File f = new File(path);
            if (!f.exists()) f.mkdirs();
        }

        String minSdkDigits = minSdkVersionString.replaceAll("[^0-9]", "");
        int minSdk = Integer.parseInt(minSdkDigits.length() > 2 ? minSdkDigits.substring(0, 2) : minSdkDigits);

        String manifest = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
            "    <application \n" +
            "        android:label=\"" + projectName + "\"\n" +
            "        android:theme=\"@style/AppTheme\">\n" + 
            "        <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
            "            <intent-filter>\n" +
            "                <action android:name=\"android.intent.action.MAIN\" />\n" +
            "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
            "            </intent-filter>\n" +
            "        </activity>\n" +
            "    </application>\n" +
            "</manifest>";
        writeFile(rootPath + "/app/src/main/AndroidManifest.xml", manifest);

        String layout = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    android:gravity=\"center\" \n" +
            "    android:orientation=\"vertical\">\n" +
            "    <TextView\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:text=\"Hello MiniStudio (" + language + ")!\" />\n" +
            "</LinearLayout>";
        writeFile(rootPath + "/app/src/main/res/layout/activity_main.xml", layout);

        String stringsXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
            "    <string name=\"app_name\">" + projectName + "</string>\n</resources>";
        writeFile(rootPath + "/app/src/main/res/values/strings.xml", stringsXml);

        String colorsXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
            "    <color name=\"purple_500\">#FF6200EE</color>\n" +
            "    <color name=\"purple_700\">#FF3700B3</color>\n" +
            "    <color name=\"teal_200\">#FF03DAC5</color>\n" +
            "</resources>";
        writeFile(rootPath + "/app/src/main/res/values/colors.xml", colorsXml);

        String stylesXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" +
            "    <style name=\"AppTheme\" parent=\"Theme.MaterialComponents.DayNight.NoActionBar\">\n" +
            "        <item name=\"colorPrimary\">@color/purple_500</item>\n" +
            "    </style>\n</resources>";
        writeFile(rootPath + "/app/src/main/res/values/styles.xml", stylesXml);
        
        if ("Kotlin".equals(language)) {
            String kotlinCode = "package " + packageName + "\n\n" +
                "import android.app.Activity\n" +
                "import android.os.Bundle\n" +
                "import " + packageName + ".R\n\n" + 
                "class MainActivity : Activity() {\n" +
                "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
                "        super.onCreate(savedInstanceState)\n\n" +
                "        // --- CRASH HANDLER ---\n" +
                "        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->\n" +
                "            try {\n" +
                "                val logDir = getExternalFilesDir(null)\n" +
                "                val crashLog = java.io.File(logDir, \"crash.log\")\n" +
                "                val writer = java.io.FileWriter(crashLog, true)\n" +
                "                writer.write(\"--- CRASH REPORT: \\${java.util.Date()} ---\\n\")\n" +
                "                throwable.printStackTrace(java.io.PrintWriter(writer))\n" +
                "                writer.write(\"\\n----------------------------------------\\n\")\n" +
                "                writer.close()\n" +
                "            } catch (ignored: Exception) {}\n" +
                "            android.os.Process.killProcess(android.os.Process.myPid())\n" +
                "        }\n\n" +
                "        setContentView(R.layout.activity_main)\n" +
                "    }\n" +
                "}";
            writeFile(sourceDirPath + "/MainActivity.kt", kotlinCode);
        } else {
            String javaCode = "package " + packageName + ";\n\n" +
                "import android.app.Activity;\n" +
                "import android.os.Bundle;\n" +
                "import " + packageName + ".R;\n\n" + 
                "public class MainActivity extends Activity {\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) { \n" +
                "        super.onCreate(savedInstanceState);\n\n" +
                "        // --- CRASH HANDLER ---\n" +
                "        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {\n" +
                "            try {\n" +
                "                java.io.File logDir = getExternalFilesDir(null);\n" +
                "                java.io.File crashLog = new java.io.File(logDir, \"crash.log\");\n" +
                "                java.io.FileWriter writer = new java.io.FileWriter(crashLog, true);\n" +
                "                writer.write(\"--- CRASH REPORT: \" + new java.util.Date() + \" ---\\n\");\n" +
                "                throwable.printStackTrace(new java.io.PrintWriter(writer));\n" +
                "                writer.write(\"\\n----------------------------------------\\n\");\n" +
                "                writer.close();\n" +
                "            } catch (Exception ignored) {}\n" +
                "            android.os.Process.killProcess(android.os.Process.myPid());\n" +
                "        });\n\n" +
                "        setContentView(R.layout.activity_main);\n" +
                "    }\n" +
                "}";
            writeFile(sourceDirPath + "/MainActivity.java", javaCode);
        }

        BuildEnvironmentManager envManager = new BuildEnvironmentManager(this);
        envManager.prepareGitHubWorkflow(rootPath, projectName, packageName, language, minSdk);
    }

    private void writeFile(String path, String content) {
        try {
            File file = new File(path);
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes("UTF-8"));
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null || !fileOrDirectory.exists()) return;
        if (isProjectIgnored(fileOrDirectory.getName())) {
            return; 
        }
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_project_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_global_github_settings) {
            showGitHubSettingsDialog();
            return true;
        }
        if (id == R.id.action_toggle_theme) {
            toggleEditorThemePref();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleEditorThemePref() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isLight = prefs.getBoolean("editor_light_theme", false);
        isLight = !isLight;
        prefs.edit().putBoolean("editor_light_theme", isLight).apply();

        Toast.makeText(this,
                isLight ? "☀️ ธีมสว่าง (ใช้ตอนเปิดโปรเจกต์)" : "🌙 ธีมมืด (ใช้ตอนเปิดโปรเจกต์)",
                Toast.LENGTH_SHORT).show();
    }

    private void showGitHubSettingsDialog() {
        SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
        String savedUsername = prefs.getString("username", "");
        String savedEmail = prefs.getString("email", "");
        String savedToken = prefs.getString("token", "");

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) (24 * getResources().getDisplayMetrics().density);
        mainLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        mainLayout.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));

        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        titleLayout.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚙️ ตั้งค่าบัญชี GitHub Sync");
        tvTitle.setTextColor(android.graphics.Color.WHITE);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        titleLayout.addView(tvTitle);
        mainLayout.addView(titleLayout);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("ข้อมูลนี้จะถูกบันทึกเพื่อใช้ส่งซอร์สโค้ดโปรเจกต์ขึ้นไปบิวด์บนคลาวด์อัตโนมัติ");
        tvDesc.setTextColor(android.graphics.Color.parseColor("#8E8E93"));
        tvDesc.setTextSize(13);
        tvDesc.setLineSpacing(0, 1.2f);
        tvDesc.setPadding(0, 0, 0, (int) (20 * getResources().getDisplayMetrics().density));
        mainLayout.addView(tvDesc);

        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxParams.bottomMargin = (int) (14 * getResources().getDisplayMetrics().density);

        android.graphics.drawable.GradientDrawable inputStyle = new android.graphics.drawable.GradientDrawable();
        inputStyle.setColor(android.graphics.Color.parseColor("#252526"));
        inputStyle.setCornerRadius((int) (8 * getResources().getDisplayMetrics().density));
        inputStyle.setStroke((int) (1 * getResources().getDisplayMetrics().density), android.graphics.Color.parseColor("#3F3F46"));

        int inputPadding = (int) (12 * getResources().getDisplayMetrics().density);

        TextView labelUsername = new TextView(this);
        labelUsername.setText("GitHub Username");
        labelUsername.setTextColor(android.graphics.Color.parseColor("#D4D4D8"));
        labelUsername.setTextSize(13);
        labelUsername.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelUsername);

        final EditText etUsername = new EditText(this);
        etUsername.setHint("ระบุชื่อผู้ใช้ GitHub");
        etUsername.setHintTextColor(android.graphics.Color.parseColor("#52525B"));
        etUsername.setText(savedUsername);
        etUsername.setTextColor(android.graphics.Color.WHITE);
        etUsername.setTextSize(14);
        etUsername.setBackground(inputStyle.getConstantState().newDrawable());
        etUsername.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        mainLayout.addView(etUsername, boxParams);

        TextView labelEmail = new TextView(this);
        labelEmail.setText("GitHub Email");
        labelEmail.setTextColor(android.graphics.Color.parseColor("#D4D4D8"));
        labelEmail.setTextSize(13);
        labelEmail.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelEmail);

        final EditText etEmail = new EditText(this);
        etEmail.setHint("ระบุอีเมลที่ผูกกับ GitHub");
        etEmail.setHintTextColor(android.graphics.Color.parseColor("#52525B"));
        etEmail.setText(savedEmail);
        etEmail.setTextColor(android.graphics.Color.WHITE);
        etEmail.setTextSize(14);
        etEmail.setBackground(inputStyle.getConstantState().newDrawable());
        etEmail.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        mainLayout.addView(etEmail, boxParams);

        TextView labelToken = new TextView(this);
        labelToken.setText("Personal Access Token (Classic)");
        labelToken.setTextColor(android.graphics.Color.parseColor("#D4D4D8"));
        labelToken.setTextSize(13);
        labelToken.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelToken);

        final EditText etToken = new EditText(this);
        etToken.setHint("วางโทเค็นสิทธิ์เข้าถึง (ghp_...)");
        etToken.setHintTextColor(android.graphics.Color.parseColor("#52525B"));
        etToken.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etToken.setText(savedToken);
        etToken.setTextColor(android.graphics.Color.WHITE);
        etToken.setTextSize(14);
        etToken.setBackground(inputStyle.getConstantState().newDrawable());
        etToken.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        mainLayout.addView(etToken, boxParams);

        final androidx.appcompat.app.AlertDialog dialog = builder.setView(mainLayout).create();

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams btnLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLayoutParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        buttonLayout.setLayoutParams(btnLayoutParams);

        android.widget.Button btnCancel = new android.widget.Button(this, null, 0, android.R.style.Widget_Material_Button_Borderless);
        btnCancel.setText("ยกเลิก");
        btnCancel.setTextColor(android.graphics.Color.parseColor("#A1A1AA"));
        btnCancel.setTextSize(14);
        btnCancel.setAllCaps(false);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        buttonLayout.addView(btnCancel);

        android.widget.Button btnSave = new android.widget.Button(this, null, 0, android.R.style.Widget_Material_Button_Borderless);
        btnSave.setText("บันทึกข้อมูล");
        btnSave.setTextColor(android.graphics.Color.WHITE);
        btnSave.setTextSize(14);
        btnSave.setAllCaps(false);
        
        android.graphics.drawable.GradientDrawable saveBtnBg = new android.graphics.drawable.GradientDrawable();
        saveBtnBg.setColor(android.graphics.Color.parseColor("#248A3D"));
        saveBtnBg.setCornerRadius((int) (6 * getResources().getDisplayMetrics().density));
        btnSave.setBackground(saveBtnBg);
        
        LinearLayout.LayoutParams saveBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) (40 * getResources().getDisplayMetrics().density));
        saveBtnParams.leftMargin = (int) (12 * getResources().getDisplayMetrics().density);
        btnSave.setLayoutParams(saveBtnParams);
        btnSave.setPadding((int) (16 * getResources().getDisplayMetrics().density), 0, (int) (16 * getResources().getDisplayMetrics().density), 0);    
		
        btnSave.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String token = etToken.getText().toString().trim();

            if (username.isEmpty() || token.isEmpty()) {
                Toast.makeText(this, "❌ กรุณากรอก Username และ Token", Toast.LENGTH_LONG).show();
                return;
            }

            prefs.edit()
                .putString("username", username)
                .putString("email", email)
                .putString("token", token)
                .putBoolean("is_github_setup", true)
                .apply();

            Toast.makeText(this, "💾 บันทึกการตั้งค่าสำเร็จ", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        buttonLayout.addView(btnSave);
        mainLayout.addView(buttonLayout);

        if (dialog.getWindow() != null) {
            android.graphics.drawable.GradientDrawable dialogBg = new android.graphics.drawable.GradientDrawable();
            dialogBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
            dialogBg.setCornerRadius((int) (14 * getResources().getDisplayMetrics().density));
            dialog.getWindow().setBackgroundDrawable(dialogBg);
        }

        dialog.show();
    }

    private void downloadAndImportProject(String githubUrl, String projectName) {
        File targetDir = new File("/sdcard/MiniStudio/" + projectName);
        String finalProjectName = projectName;
        int counter = 1;
        while (targetDir.exists()) {
            finalProjectName = projectName + "_" + counter;
            targetDir = new File("/sdcard/MiniStudio/" + finalProjectName);
            counter++;
        }

        SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        Intent serviceIntent = new Intent(this, GitHubCloneService.class);
        serviceIntent.putExtra("githubUrl", githubUrl);
        serviceIntent.putExtra("projectName", finalProjectName);
        serviceIntent.putExtra("token", token);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toast.makeText(this, "🚀 เริ่มการ Clone หลังบ้านเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show();
    }

    private boolean isProjectIgnored(String folderName) {
        String[] ignoredItems = {".git", ".gradle", ".idea", "build", "SystemBackup", "Drafts"};
        
        for (String item : ignoredItems) {
            if (folderName.equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }
}
