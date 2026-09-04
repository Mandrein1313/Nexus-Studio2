package com.dev.ministudio;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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

    private void showConfigDialog(ProjectTemplate template) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_project_config);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        EditText etName = dialog.findViewById(R.id.etProjectName);
        EditText etPkg = dialog.findViewById(R.id.etPackageName);
        etName.setText("MyApp");
        etPkg.setText("com.example.myapp");

        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String pkg = etPkg.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "กรุณาใส่ชื่อโปรเจกต์", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pkg.isEmpty() || !pkg.contains(".")) {
                Toast.makeText(this, "Package Name ไม่ถูกต้อง", Toast.LENGTH_SHORT).show();
                return;
            }
            // อนุญาตเฉพาะ a-z A-Z 0-9 . _
            if (!pkg.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+")) {
                Toast.makeText(this, "Package Name รูปแบบไม่ถูกต้อง", Toast.LENGTH_SHORT).show();
                return;
            }

            File dest = new File("/sdcard/MiniStudio/" + name);
            if (dest.exists()) {
                Toast.makeText(this, "มีโปรเจกต์ชื่อนี้อยู่แล้ว", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            createAndOpen(name, pkg, template.id);
        });

        dialog.show();
    }

    private void createAndOpen(String projectName, String packageName, String templateId) {
        Toast.makeText(this, "กำลังสร้างโปรเจกต์...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            boolean ok = ProjectGenerator.create(projectName, packageName, templateId);
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

            // วาด preview ง่าย ๆ ตาม template
            h.previewContent.removeAllViews();
            drawPreview(h.previewContent, t.id);

            h.itemView.setOnClickListener(v -> showConfigDialog(t));
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
                // จุดประ
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
                // ว่าง ๆ
                break;

            case "basic":
                // FAB มุมขวาล่าง
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
                // แถบซ้าย + รายการ
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