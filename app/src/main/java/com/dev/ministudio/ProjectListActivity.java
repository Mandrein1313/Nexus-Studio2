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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import java.net.URL;
import java.io.InputStream;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.Spinner;
import android.graphics.drawable.GradientDrawable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import android.content.IntentFilter;

public class ProjectListActivity extends AppCompatActivity {
    private ArrayList<String> projects = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private DrawerLayout drawerLayout;
    private FloatingActionsMenu fabMenu;
    private FloatingActionButton fabCreate;
    private FloatingActionButton fabGithub;
    private final android.content.BroadcastReceiver cloneReceiver = new android.content.BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        refreshProjectList();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
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
   getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#1A1B26"));;
    setContentView(R.layout.activity_project_list);

    ListView listView = findViewById(R.id.projectListView);
    Toolbar toolbar = findViewById(R.id.toolbar);
    drawerLayout = findViewById(R.id.drawer_layout);

    fabMenu = findViewById(R.id.multiple_actions);
    fabCreate = findViewById(R.id.action_create);
    fabGithub = findViewById(R.id.action_github);

    setSupportActionBar(toolbar);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar, android.R.string.ok, android.R.string.cancel);
    drawerLayout.addDrawerListener(toggle);
    toggle.syncState();
    com.google.android.material.navigation.NavigationView navView = findViewById(R.id.nav_view);
if (navView != null) {
    // ดันเมนูลงมาใต้ status bar
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
        drawerLayout.closeDrawers();
        return true;
    });
}

    // 1. ตั้งค่าปุ่ม Fab ผ่านเมธอดแยก (สะอาดและดูดีขึ้น)
    setupFabButtons();

    // 2. โหลดข้อมูลต่างๆ
    checkPermissions();
    refreshProjectList();

    // 3. ตั้งค่า Adapter
    adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, projects) {
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView text = (TextView) view.findViewById(android.R.id.text1);
            text.setTextColor(android.graphics.Color.WHITE);
            text.setTextSize(18);
            text.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.sym_def_app_icon, 0, 0, 0);
            text.setCompoundDrawablePadding(30);
            return view;
        }
    };
    listView.setAdapter(adapter);

    listView.setOnItemClickListener((parent, view, position, id) -> {
        Intent intent = new Intent(ProjectListActivity.this, MainActivity.class);
        intent.putExtra("projectName", projects.get(position));
        startActivity(intent);
    });

    listView.setOnItemLongClickListener((parent, view, position, id) -> {
        String projectName = projects.get(position);
        File projectDir = new File("/sdcard/MiniStudio/" + projectName);

        new AlertDialog.Builder(ProjectListActivity.this)
            .setTitle("ลบโปรเจกต์")
            .setMessage("คุณต้องการลบ " + projectName + " ใช่หรือไม่?")
            .setPositiveButton("ลบ", (dialog, which) -> {
                deleteRecursive(projectDir);

                if (!projectDir.exists()) {
                    projects.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(ProjectListActivity.this, "ลบโปรเจกต์ " + projectName + " เรียบร้อยครับ", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProjectListActivity.this, "ไม่สามารถลบไฟล์ได้ โปรดตรวจสอบสิทธิ์", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("ยกเลิก", null)
            .show();
        return true;
    });

    // 4. ระบบตรวจสอบ GitHub
    SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
    if (!prefs.getBoolean("is_github_setup", false)) {
        new android.os.Handler().postDelayed(this::showGitHubSettingsDialog, 600);
    }

    // เพิ่มตัวรับแจ้งเตือนเมื่อ Service ทำงานเสร็จ
    IntentFilter filter = new IntentFilter(GitHubCloneService.ACTION_CLONE_COMPLETE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(cloneReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    } else {
        registerReceiver(cloneReceiver, filter);
    }
}

    private void setupFabButtons() {
        fabCreate.setOnClickListener(v -> {
            showCreateProjectDialog(); // เรียกหน้าต่างสร้างโปรเจกต์
            fabMenu.collapse();
        });

        fabGithub.setOnClickListener(v -> {
            importFromGitHub(); // เรียกฟังก์ชันนำเข้า (น้าไปเขียนต่อด้านล่างครับ)
            fabMenu.collapse();
        });
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
            
            // 💡 แกะชื่อ Repo จาก URL อัตโนมัติ (เช่น https://github.com/aaa/bbb.git -> bbb)
            String projectName = extractRepoName(url);
            if (projectName == null || projectName.isEmpty()) {
                projectName = "Import_" + System.currentTimeMillis();
            }
            
            downloadAndImportProject(url, projectName);
        })
        .setNegativeButton("ยกเลิก", null)
        .show();
}

// 🟢 วางฟังก์ชันนี้เพิ่มเข้าไปได้เลยครับ
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


  // --- เมธอดส่วนที่เหลือคงเดิม ---
    private void refreshProjectList() {
        projects.clear();
        File root = new File("/sdcard/MiniStudio");
        if (!root.exists()) root.mkdirs();
        File[] files = root.listFiles();
        if (files != null) {
            for (File f : files) if (f.isDirectory()) projects.add(f.getName());
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        // เมื่อผู้ใช้ตอบรับสิทธิ์แจ้งเตือนแล้ว พอกลับมาหน้านี้ ค่อยเช็กสิทธิ์ไฟล์ต่อ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                checkFilePermission();
            }
        } else {
            checkFilePermission();
        }
    }

    private void checkPermissions() {
        // 1. ขอสิทธิ์การแจ้งเตือนก่อน (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                return; // หยุดตรงนี้ก่อน เพื่อให้ผู้ใช้กดป๊อบอัพอนุญาต
            }
        }
        // 2. ถ้าสิทธิ์แจ้งเตือนผ่านแล้ว ค่อยเช็กสิทธิ์เข้าถึงไฟล์
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

    // 🟢 หน้าต่างสร้างโปรเจกต์แบบ Advance เพิ่มตัวเลือก Language และ Minimum SDK (ดีไซน์พรีเมียมดาร์กโมด)
    private void showCreateProjectDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        // 1. คอนเทนเนอร์หลัก
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) (24 * getResources().getDisplayMetrics().density);
        mainLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        mainLayout.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));

        // 2. แถวหัวข้อไดอะล็อกพร้อมไอคอน
        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        titleLayout.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🚀 New Project");
        tvTitle.setTextColor(android.graphics.Color.WHITE);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        titleLayout.addView(tvTitle);
        mainLayout.addView(titleLayout);

        // 3. คำอธิบายรายละเอียด
        TextView tvDesc = new TextView(this);
        tvDesc.setText("กำหนดค่าโครงสร้างและสภาพแวดล้อมสำหรับโปรเจกต์ใหม่ของคุณ");
        tvDesc.setTextColor(android.graphics.Color.parseColor("#8E8E93"));
        tvDesc.setTextSize(13);
        tvDesc.setLineSpacing(0, 1.2f);
        tvDesc.setPadding(0, 0, 0, (int) (20 * getResources().getDisplayMetrics().density));
        mainLayout.addView(tvDesc);

        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        boxParams.bottomMargin = (int) (14 * getResources().getDisplayMetrics().density);

        // สไตล์สำหรับช่องกรอกและ Dropdown (สีเทาเข้ม ขอบมน เส้นขอบบาง)
        android.graphics.drawable.GradientDrawable inputStyle = new android.graphics.drawable.GradientDrawable();
        inputStyle.setColor(android.graphics.Color.parseColor("#252526"));
        inputStyle.setCornerRadius((int) (8 * getResources().getDisplayMetrics().density));
        inputStyle.setStroke((int) (1 * getResources().getDisplayMetrics().density), android.graphics.Color.parseColor("#3F3F46"));

        int inputPadding = (int) (12 * getResources().getDisplayMetrics().density);

        // --- ช่องกรอกที่ 1: Project Name ---
        TextView labelProjectName = new TextView(this);
        labelProjectName.setText("Project Name");
        labelProjectName.setTextColor(android.graphics.Color.parseColor("#D4D4D8"));
        labelProjectName.setTextSize(13);
        labelProjectName.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelProjectName);

        final EditText etProjectName = new EditText(this);
        etProjectName.setHint("e.g., MyGame");
        etProjectName.setHintTextColor(android.graphics.Color.parseColor("#52525B"));
        etProjectName.setTextColor(android.graphics.Color.WHITE);
        etProjectName.setTextSize(14);
        etProjectName.setSingleLine(true);
        etProjectName.setBackground(inputStyle.getConstantState().newDrawable());
        etProjectName.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        mainLayout.addView(etProjectName, boxParams);

        // --- ช่องกรอกที่ 2: Package Name ---
        TextView labelPackageName = new TextView(this);
        labelPackageName.setText("Package Name");
        labelPackageName.setTextColor(android.graphics.Color.parseColor("#D4D4D8"));
        labelPackageName.setTextSize(13);
        labelPackageName.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelPackageName);

        final EditText etPackageName = new EditText(this);
        etPackageName.setHint("e.g., com.dev.mygame");
        etPackageName.setHintTextColor(android.graphics.Color.parseColor("#52525B"));
        etPackageName.setTextColor(android.graphics.Color.WHITE);
        etPackageName.setTextSize(14);
        etPackageName.setSingleLine(true);
        etPackageName.setBackground(inputStyle.getConstantState().newDrawable());
        etPackageName.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        mainLayout.addView(etPackageName, boxParams);

        // ระบบแปลงชื่อ Package อัตโนมัติ
        etProjectName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String name = s.toString().trim().toLowerCase().replaceAll("[^a-z0-9]", "");
                if (!name.isEmpty()) {
                    etPackageName.setText("com.example." + name);
                } else {
                    etPackageName.setText("");
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // --- ช่องเลือกที่ 3: Language (Spinner) ---
        TextView labelLanguage = new TextView(this);
        labelLanguage.setText("Language");
        labelLanguage.setTextColor(android.graphics.Color.parseColor("#D4D4D8"));
        labelLanguage.setTextSize(13);
        labelLanguage.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelLanguage);

        final android.widget.Spinner spinLanguage = new android.widget.Spinner(this);
        spinLanguage.setBackground(inputStyle.getConstantState().newDrawable());
        spinLanguage.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);
        
        String[] languages = {"Java", "Kotlin"};
        ArrayAdapter<String> langAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, languages) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(android.graphics.Color.WHITE);
                tv.setTextSize(14);
                return tv;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(android.graphics.Color.WHITE);
                tv.setBackgroundColor(android.graphics.Color.parseColor("#252526"));
                tv.setPadding(30, 30, 30, 30);
                return tv;
            }
        };
        spinLanguage.setAdapter(langAdapter);
        mainLayout.addView(spinLanguage, boxParams);

        // --- ช่องเลือกที่ 4: Minimum SDK (Spinner) ---
        TextView labelMinSdk = new TextView(this);
        labelMinSdk.setText("Minimum SDK");
        labelMinSdk.setTextColor(android.graphics.Color.parseColor("#D4D4D8"));
        labelMinSdk.setTextSize(13);
        labelMinSdk.setPadding(0, 0, 0, (int) (6 * getResources().getDisplayMetrics().density));
        mainLayout.addView(labelMinSdk);

        final android.widget.Spinner spinMinSdk = new android.widget.Spinner(this);
        spinMinSdk.setBackground(inputStyle.getConstantState().newDrawable());
        spinMinSdk.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);

        String[] sdkOptions = {
                "API 21: Android 5.0 (Lollipop)",
                "API 23: Android 6.0 (Marshmallow)",
                "API 26: Android 8.0 (Oreo)",
                "API 29: Android 10.0",
                "API 33: Android 13.0"
        };
        ArrayAdapter<String> sdkAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sdkOptions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(android.graphics.Color.WHITE);
                tv.setTextSize(14);
                return tv;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(android.graphics.Color.WHITE);
                tv.setBackgroundColor(android.graphics.Color.parseColor("#252526"));
                tv.setPadding(30, 30, 30, 30);
                return tv;
            }
        };
        spinMinSdk.setAdapter(sdkAdapter);
        spinMinSdk.setSelection(1); // เลือก API 23 เป็นค่าเริ่มต้นตามสไตล์ IDE ทั่วไป
        mainLayout.addView(spinMinSdk, boxParams);


        final androidx.appcompat.app.AlertDialog dialog = builder.setView(mainLayout).create();

        // 4. แถบปุ่มกดด้านล่าง (CANCEL / CREATE)
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(android.view.Gravity.END);
        LinearLayout.LayoutParams btnLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLayoutParams.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
        buttonLayout.setLayoutParams(btnLayoutParams);

        // ปุ่มยกเลิก
        android.widget.Button btnCancel = new android.widget.Button(this, null, 0, android.R.style.Widget_Material_Button_Borderless);
        btnCancel.setText("CANCEL");
        btnCancel.setTextColor(android.graphics.Color.parseColor("#A1A1AA"));
        btnCancel.setTextSize(14);
        btnCancel.setAllCaps(true);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        buttonLayout.addView(btnCancel);

        // ปุ่มสร้างโปรเจกต์
        android.widget.Button btnCreate = new android.widget.Button(this, null, 0, android.R.style.Widget_Material_Button_Borderless);
        btnCreate.setText("CREATE");
        btnCreate.setTextColor(android.graphics.Color.WHITE);
        btnCreate.setTextSize(14);
        btnCreate.setAllCaps(true);
        
        android.graphics.drawable.GradientDrawable createBtnBg = new android.graphics.drawable.GradientDrawable();
        createBtnBg.setColor(android.graphics.Color.parseColor("#248A3D"));
        createBtnBg.setCornerRadius((int) (6 * getResources().getDisplayMetrics().density));
        btnCreate.setBackground(createBtnBg);
        
        LinearLayout.LayoutParams createBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, (int) (40 * getResources().getDisplayMetrics().density));
        createBtnParams.leftMargin = (int) (12 * getResources().getDisplayMetrics().density);
        btnCreate.setLayoutParams(createBtnParams);
        btnCreate.setPadding((int) (20 * getResources().getDisplayMetrics().density), 0, (int) (20 * getResources().getDisplayMetrics().density), 0);    

        btnCreate.setOnClickListener(v -> {
            String name = etProjectName.getText().toString().trim();
            String packageName = etPackageName.getText().toString().trim();
            String selectedLang = spinLanguage.getSelectedItem().toString(); // "Java" หรือ "Kotlin"
            String selectedSdk = spinMinSdk.getSelectedItem().toString();   // "API 23: Android..."

            if (name.isEmpty()) {
                Toast.makeText(ProjectListActivity.this, "❌ กรุณากรอกชื่อโปรเจกต์", Toast.LENGTH_SHORT).show();
                return;
            }
            if (packageName.isEmpty() || !packageName.contains(".") || packageName.endsWith(".")) {
                Toast.makeText(ProjectListActivity.this, "❌ รูปแบบ Package Name ไม่ถูกต้อง", Toast.LENGTH_LONG).show();
                return;
            }

            // 🌟 ส่งค่าตัวแปร Language และ SDK ไปประมวลผลต่อที่ระบบสร้างโปรเจกต์หลังบ้าน
            createNewProject(name, packageName, selectedLang, selectedSdk);
            
            refreshProjectList(); 
            adapter.notifyDataSetChanged();
            Toast.makeText(ProjectListActivity.this, "Project Created (" + selectedLang + ")!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        buttonLayout.addView(btnCreate);
        mainLayout.addView(buttonLayout);

        if (dialog.getWindow() != null) {
            android.graphics.drawable.GradientDrawable dialogBg = new android.graphics.drawable.GradientDrawable();
            dialogBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
            dialogBg.setCornerRadius((int) (14 * getResources().getDisplayMetrics().density));
            dialog.getWindow().setBackgroundDrawable(dialogBg);
        }

        dialog.show();
    }

    // 🌟 เมธอดเวอร์ชันอัปเกรด: รับค่าตัวแปรภาษาและ SDK มาจำแนกเขียนโค้ดและสร้างโฟลเดอร์จริง
    // 🌟 เมธอดเวอร์ชันอัปเกรด: รับค่าตัวแปรภาษาและ SDK มาจำแนกเขียนโค้ดและสร้างโฟลเดอร์จริง
    private void createNewProject(String projectName, String packageName, String language, String minSdkVersionString) {
        String rootPath = "/sdcard/MiniStudio/" + projectName;
        
        // 1. นำค่าภาษามาสลับโฟลเดอร์ Source Code ให้ตรงตามไวยากรณ์ (src/main/java หรือ src/main/kotlin)
        String langFolder = language.toLowerCase(); // คืนค่าเป็น "java" หรือ "kotlin"
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

        // 2. นำข้อความ SDK มาแกะเอาเฉพาะตัวเลข API ด้วย Regular Expression
        String minSdkDigits = minSdkVersionString.replaceAll("[^0-9]", "");
        int minSdk = Integer.parseInt(minSdkDigits.length() > 2 ? minSdkDigits.substring(0, 2) : minSdkDigits);

        // 3. สร้างไฟล์ AndroidManifest.xml
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

        // 4. สร้าง Resource Files (Layout, Strings, Colors, Styles)
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
        
        // 5. คัดแยกการเจนไฟล์ซอร์สโค้ดเริ่มต้น
                // 5. คัดแยกการเจนไฟล์ซอร์สโค้ดเริ่มต้น (พร้อมฝัง Crash Handler)[span_1](start_span)[span_1](end_span)
        if ("Kotlin".equals(language)) {[span_2](start_span)[span_2](end_span)
            String kotlinCode = "package " + packageName + "\n\n" +[span_3](start_span)[span_3](end_span)
                "import android.app.Activity\n" +[span_4](start_span)[span_4](end_span)
                "import android.os.Bundle\n" +[span_5](start_span)[span_5](end_span)
                "import " + packageName + ".R\n\n" + 
                "class MainActivity : Activity() {\n" +[span_6](start_span)[span_6](end_span)
                "    override fun onCreate(savedInstanceState: Bundle?) {\n" +[span_7](start_span)[span_7](end_span)
                "        super.onCreate(savedInstanceState)\n\n" +
                
                "        // --- CRASH HANDLER (ดักจับ App Crash ลงไฟล์) ---\n" +
                "        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->\n" +
                "            try {\n" +
                "                val logDir = getExternalFilesDir(null)\n" +
                "                val crashLog = java.io.File(logDir, \"crash.log\")\n" +
                "                val writer = java.io.FileWriter(crashLog, true)\n" +
                "                writer.write(\"--- CRASH REPORT: \${java.util.Date()} ---\\n\")\n" +
                "                throwable.printStackTrace(java.io.PrintWriter(writer))\n" +
                "                writer.write(\"\\n----------------------------------------\\n\")\n" +
                "                writer.close()\n" +
                "            } catch (ignored: Exception) {}\n" +
                "            android.os.Process.killProcess(android.os.Process.myPid())\n" +
                "        }\n\n" +
                
                "        setContentView(R.layout.activity_main)\n" +[span_8](start_span)[span_8](end_span)
                "    }\n" +[span_9](start_span)[span_9](end_span)
                "}";[span_10](start_span)[span_10](end_span)
            writeFile(sourceDirPath + "/MainActivity.kt", kotlinCode);[span_11](start_span)[span_11](end_span)
        } else {
            String javaCode = "package " + packageName + ";\n\n" +[span_12](start_span)[span_12](end_span)
                "import android.app.Activity;\n" +[span_13](start_span)[span_13](end_span)
                "import android.os.Bundle;\n" +[span_14](start_span)[span_14](end_span)
                "import " + packageName + ".R;\n\n" + 
                "public class MainActivity extends Activity {\n" +[span_15](start_span)[span_15](end_span)
                "    @Override\n" +[span_16](start_span)[span_16](end_span)
                "    protected void onCreate(Bundle savedInstanceState) { \n" +[span_17](start_span)[span_17](end_span)
                "        super.onCreate(savedInstanceState);\n\n" +
                
                "        // --- CRASH HANDLER (ดักจับ App Crash ลงไฟล์) ---\n" +
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
                
                "        setContentView(R.layout.activity_main);\n" +[span_18](start_span)[span_18](end_span)
                "    }\n" +[span_19](start_span)[span_19](end_span)
                "}";[span_20](start_span)[span_20](end_span)
            writeFile(sourceDirPath + "/MainActivity.java", javaCode);[span_21](start_span)[span_21](end_span)
        }


        // 6. ส่งโครงสร้างและเตรียมค่าสำหรับส่งไปคอมไพล์บน GitHub CI/CD
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

        // --- GitHub Username ---
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
        // --- GitHub Email ---
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
        // --- Personal Access Token ---
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

    // 💡 ดึง Token จาก SharedPreferences ส่งติดไปด้วย
    SharedPreferences prefs = getSharedPreferences("GitHubPrefs", Context.MODE_PRIVATE);
    String token = prefs.getString("token", "");

    Intent serviceIntent = new Intent(this, GitHubCloneService.class);
    serviceIntent.putExtra("githubUrl", githubUrl);
    serviceIntent.putExtra("projectName", finalProjectName);
    serviceIntent.putExtra("token", token); // ⚡ เพิ่มบรรทัดนี้ส่ง Token ไปให้ Service หลังบ้าน
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(serviceIntent);
    } else {
        startService(serviceIntent);
    }

    Toast.makeText(this, "🚀 เริ่มการ Clone หลังบ้านเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show();
}

// ฟังก์ชันช่วยเช็คว่าดาวน์โหลดได้จริงไหม
private boolean attemptDownload(String urlString, File outputFile) {
    try {
        java.net.URL url = new java.net.URL(urlString);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        if (conn.getResponseCode() == 200) {
            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);
            }
            return true;
        }
    } catch (Exception e) {
        return false;
    }
    return false;
}
// ตรวจสอบชื่อไฟล์ขยะ
private boolean isUnwanted(String name) {
    String lower = name.toLowerCase();
    return lower.equals(".git") || 
           lower.equals(".gradle") || 
           lower.equals(".idea") || 
           lower.equals("build") || 
           lower.equals(".gitignore") || // เพิ่ม
           lower.endsWith(".iml") || 
           lower.endsWith(".md") ||      // เพิ่ม (ไฟล์ README)
           lower.startsWith("temp");
}
// เพิ่มฟังก์ชันนี้เข้าไปในคลาสครับ
private boolean isProjectIgnored(String folderName) {
    // รายชื่อโฟลเดอร์หรือชื่อโปรเจกต์ที่ไม่ต้องการให้ยุ่ง
    String[] ignoredItems = {".git", ".gradle", ".idea", "build", "SystemBackup", "Drafts"};
    
    for (String item : ignoredItems) {
        if (folderName.equalsIgnoreCase(item)) {
            return true; // ถ้าเจอชื่อพวกนี้ ให้ข้ามไปเลย
        }
    }
    return false;
}



}
