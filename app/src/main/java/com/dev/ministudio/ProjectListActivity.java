package com.dev.ministudio;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
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

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.util.ArrayList;

public class ProjectListActivity extends AppCompatActivity {
    private ArrayList<String> projects = new ArrayList<>();
    private DrawerLayout drawerLayout;
    private FloatingActionsMenu fabMenu;
    private FloatingActionButton fabCreate;
    private FloatingActionButton fabGithub;

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
        } catch (Exception ignored) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ตั้งค่าขอบหน้าจอสำหรับ Android 15+
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(Color.parseColor("#0D0E14"));
        getWindow().setNavigationBarColor(Color.parseColor("#0D0E14"));
        setContentView(R.layout.activity_project_list);

        // ผูก View Container สำหรับรายการโปรเจกต์
        projectRowsContainer = findViewById(R.id.projectRowsContainer);
        tvNoProjects = null;

        // Toolbar + DrawerLayout Setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        if (drawerLayout != null && toolbar != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar,
                    android.R.string.ok, android.R.string.cancel);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        // ปุ่ม New project & Choose template
        View btnNew = findViewById(R.id.btnNewProject);
        if (btnNew != null) {
            btnNew.setOnClickListener(v ->
                    startActivity(new Intent(this, NewProjectActivity.class)));
        }

        View btnChoose = findViewById(R.id.btnChooseTemplate);
        if (btnChoose != null) {
            btnChoose.setOnClickListener(v ->
                    startActivity(new Intent(this, NewProjectActivity.class)));
        }

        // NavigationView เมนูซ้าย
        NavigationView navView = findViewById(R.id.nav_view);
        if (navView != null) {
            // คำนวณความสูง Status bar เพื่อตั้งค่า Padding ให้เมนู
            int statusBarHeight = 0;
            int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resId > 0) {
                statusBarHeight = getResources().getDimensionPixelSize(resId);
            }
            navView.setPadding(0, statusBarHeight, 0, 0);

            navView.setNavigationItemSelectedListener(item -> {
    int id = item.getItemId();
    if (id == R.id.nav_import_github) {
        importFromGitHub();
    } else if (id == R.id.nav_github_settings) {
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

        // ตรวจสอบสิทธิ์การเข้าถึงไฟล์และแจ้งเตือน
        checkPermissions();

        // โหลดข้อมูลโปรเจกต์
        refreshProjectList();
        updateProjectEmptyState();

        // ตรวจสอบการตั้งค่า GitHub ครั้งแรก
        SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("is_github_setup", false)) {
            new android.os.Handler().postDelayed(this::showGitHubSettingsDialog, 600);
        }

        // ลงทะเบียน BroadcastReceiver รับสถานะการ Clone
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
                startActivity(new Intent(this, NewProjectActivity.class));
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
        etUrl.setTextColor(Color.WHITE);

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

        View emptyState = findViewById(R.id.emptyState);
        if (projectRowsContainer != null) {
            projectRowsContainer.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
        if (emptyState != null) {
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (tvNoProjects != null) {
            tvNoProjects.setVisibility(View.GONE);
        }
    }

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

        View emptyState = findViewById(R.id.emptyState);
        if (projects.isEmpty()) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            return;
        }
        if (emptyState != null) emptyState.setVisibility(View.GONE);

        float d = getResources().getDisplayMetrics().density;

        for (int i = 0; i < projects.size(); i++) {
            final String name = projects.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackgroundResource(R.drawable.bg_project_card);
            int pad = (int) (16 * d);
            card.setPadding(pad, pad, pad, pad);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = (int) (10 * d);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView tvName = new TextView(this);
            tvName.setText(name);
            tvName.setTextColor(Color.parseColor("#C0CAF5"));
            tvName.setTextSize(16);
            tvName.setTypeface(null, Typeface.BOLD);
            textCol.addView(tvName);

            TextView tvMeta = new TextView(this);
            tvMeta.setText(readProjectMeta(name));
            tvMeta.setTextColor(Color.parseColor("#565F89"));
            tvMeta.setTextSize(12);
            tvMeta.setPadding(0, (int) (4 * d), 0, 0);
            textCol.addView(tvMeta);

            card.addView(textCol, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView btnDelete = new TextView(this);
            btnDelete.setText("🗑");
            btnDelete.setTextSize(18);
            btnDelete.setPadding((int) (12 * d), (int) (8 * d), (int) (4 * d), (int) (8 * d));
            btnDelete.setOnClickListener(v -> confirmDeleteProject(name));
            card.addView(btnDelete);

            card.setOnClickListener(v -> {
                Intent intent = new Intent(ProjectListActivity.this, MainActivity.class);
                intent.putExtra("projectName", name);
                startActivity(intent);
            });

            projectRowsContainer.addView(card, cardLp);
        }
    }

    private String readProjectMeta(String projectName) {
        try {
            File root = new File("/sdcard/MiniStudio/" + projectName);
            File gradle = new File(root, "app/build.gradle");
            if (gradle.exists()) {
                String text = new String(java.nio.file.Files.readAllBytes(gradle.toPath()), "UTF-8");
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("applicationId\\s*[\"']([^\"']+)[\"']")
                        .matcher(text);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Exception ignored) {}
        return "Android project";
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
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) (24 * getResources().getDisplayMetrics().density);
        mainLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        mainLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));

        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setGravity(Gravity.CENTER_VERTICAL);
        titleLayout.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("⚙️ ตั้งค่าบัญชี GitHub Sync");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleLayout.addView(tvTitle);
        mainLayout.addView(titleLayout);

        TextView tvDesc = new TextView(this);
        tvDesc.setText("ข้อมูลนี้จะถูกบันทึกเพื่อใช้ส่งซอร์สโค้ดโปรเจกต์ขึ้นไปบิวด์บนคลาวด์อัตโนมัติ");
        tvDesc.setTextColor(Color.parseColor("#8E8E93"));
        tvDesc.setTextSize(13);
        tvDesc.setLineSpacing(0, 1.2f);
        tvDesc.setPadding(0, 0, 0, (int) (20 * getResources().getDisplayMetrics().density));
        mainLayout.addView(tvDesc);

        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxParams.bottomMargin = (int) (14 * getResources().getDisplayMetrics().density);

        GradientDrawable inputStyle = new GradientDrawable();
        inputStyle.setColor(Color.parseColor("#252526"));
        inputStyle.setCornerRadius((int) (8 * getResources().getDisplayMetrics().density));
        inputStyle.setStroke((int) (1 * getResources().getDisplayMetrics().density), Color.parseColor("#3F3F46"));

        int inputPadding = (int) (12 * getResources().getDisplayMetrics().density);

        TextView labelUsername = new TextView(this);
        labelUsername.setText("GitHub Username");
        labelUsername.setTextColor(Color.parseColor("#D4D4D8"));
        labelUsername.setTextSize(13);
        labelUsername.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelUsername);

        final EditText etUsername = new EditText(this);
        etUsername.setHint("ระบุชื่อผู้ใช้ GitHub");
        etUsername.setHintTextColor(Color.parseColor("#52525B"));
        etUsername.setText(savedUsername);
        etUsername.setTextColor(Color.WHITE);
        etUsername.setTextSize(14);
        etUsername.setBackground(inputStyle.getConstantState().newDrawable());
        etUsername.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        mainLayout.addView(etUsername, boxParams);

        TextView labelEmail = new TextView(this);
        labelEmail.setText("GitHub Email");
        labelEmail.setTextColor(Color.parseColor("#D4D4D8"));
        labelEmail.setTextSize(13);
        labelEmail.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelEmail);

        final EditText etEmail = new EditText(this);
        etEmail.setHint("ระบุอีเมลที่ผูกกับ GitHub");
        etEmail.setHintTextColor(Color.parseColor("#52525B"));
        etEmail.setText(savedEmail);
        etEmail.setTextColor(Color.WHITE);
        etEmail.setTextSize(14);
        etEmail.setBackground(inputStyle.getConstantState().newDrawable());
        etEmail.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        mainLayout.addView(etEmail, boxParams);

        TextView labelToken = new TextView(this);
        labelToken.setText("Personal Access Token (Classic)");
        labelToken.setTextColor(Color.parseColor("#D4D4D8"));
        labelToken.setTextSize(13);
        labelToken.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelToken);

        final EditText etToken = new EditText(this);
        etToken.setHint("วางโทเค็นสิทธิ์เข้าถึง (ghp_...)");
        etToken.setHintTextColor(Color.parseColor("#52525B"));
        etToken.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etToken.setText(savedToken);
        etToken.setTextColor(Color.WHITE);
        etToken.setTextSize(14);
        etToken.setBackground(inputStyle.getConstantState().newDrawable());
        etToken.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        mainLayout.addView(etToken, boxParams);

        final AlertDialog dialog = builder.setView(mainLayout).create();

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.END);
        LinearLayout.LayoutParams btnLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLayoutParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        buttonLayout.setLayoutParams(btnLayoutParams);

        android.widget.Button btnCancel = new android.widget.Button(this, null, 0, android.R.style.Widget_Material_Button_Borderless);
        btnCancel.setText("ยกเลิก");
        btnCancel.setTextColor(Color.parseColor("#A1A1AA"));
        btnCancel.setTextSize(14);
        btnCancel.setAllCaps(false);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        buttonLayout.addView(btnCancel);

        android.widget.Button btnSave = new android.widget.Button(this, null, 0, android.R.style.Widget_Material_Button_Borderless);
        btnSave.setText("บันทึกข้อมูล");
        btnSave.setTextColor(Color.WHITE);
        btnSave.setTextSize(14);
        btnSave.setAllCaps(false);
        
        GradientDrawable saveBtnBg = new GradientDrawable();
        saveBtnBg.setColor(Color.parseColor("#248A3D"));
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
            GradientDrawable dialogBg = new GradientDrawable();
            dialogBg.setColor(Color.parseColor("#1E1E1E"));
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
