package com.dev.ministudio.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Dialog ยืนยันออกจากหน้าแก้ไขกลับไปหน้ารายการโปรเจกต์ โทน Tokyo Night
 */
public final class ExitConfirmDialog {

    private ExitConfirmDialog() {}

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        float d = activity.getResources().getDisplayMetrics().density;

        GradientDrawable dialogBg = new GradientDrawable();
        dialogBg.setColor(Color.parseColor("#1F2335"));
        dialogBg.setCornerRadius(16 * d);
        dialogBg.setStroke((int) (1 * d), Color.parseColor("#292E42"));

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (24 * d), (int) (22 * d), (int) (24 * d), (int) (18 * d));
        root.setBackground(dialogBg);

        TextView icon = new TextView(activity);
        icon.setText("🚪");
        icon.setTextSize(28);
        icon.setGravity(Gravity.CENTER);
        icon.setPadding(0, 0, 0, (int) (8 * d));
        root.addView(icon);

        TextView title = new TextView(activity);
        title.setText("กลับหน้ารายการโปรเจกต์");
        title.setTextColor(Color.parseColor("#C0CAF5"));
        title.setTextSize(18);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView msg = new TextView(activity);
        msg.setText("ต้องการออกจากตัวแก้ไข\nแล้วกลับไปหน้า Projects หรือไม่?");
        msg.setTextColor(Color.parseColor("#565F89"));
        msg.setTextSize(14);
        msg.setGravity(Gravity.CENTER);
        msg.setLineSpacing(0, 1.25f);
        msg.setPadding(0, (int) (12 * d), 0, (int) (22 * d));
        root.addView(msg);

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);

        Button btnCancel = new Button(activity);
        btnCancel.setText("อยู่ต่อ");
        btnCancel.setAllCaps(false);
        btnCancel.setTextColor(Color.parseColor("#A9B1D6"));
        btnCancel.setTextSize(14);
        GradientDrawable cancelBg = new GradientDrawable();
        cancelBg.setColor(Color.parseColor("#24283B"));
        cancelBg.setCornerRadius(10 * d);
        btnCancel.setBackground(cancelBg);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, (int) (44 * d), 1f);
        cancelLp.rightMargin = (int) (8 * d);
        btnCancel.setLayoutParams(cancelLp);

        Button btnExit = new Button(activity);
        btnExit.setText("กลับ");
        btnExit.setAllCaps(false);
        btnExit.setTextColor(Color.parseColor("#1A1B26"));
        btnExit.setTextSize(14);
        GradientDrawable exitBg = new GradientDrawable();
        exitBg.setColor(Color.parseColor("#F7768E"));
        exitBg.setCornerRadius(10 * d);
        btnExit.setBackground(exitBg);
        LinearLayout.LayoutParams exitLp = new LinearLayout.LayoutParams(0, (int) (44 * d), 1f);
        exitLp.leftMargin = (int) (8 * d);
        btnExit.setLayoutParams(exitLp);

        buttons.addView(btnCancel);
        buttons.addView(btnExit);
        root.addView(buttons);

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            // กลับไปหน้า Projects (ไม่ปิดแอป)
            Intent intent = new Intent(activity, com.dev.ministudio.ProjectListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(intent);
            activity.finish();
        });

        dialog.show();
    }
}
