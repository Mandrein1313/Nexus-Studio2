package com.dev.ministudio;

import com.dev.ministudio.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import com.dev.ministudio.fs.FileSystemManager;
import com.dev.ministudio.model.FileNode;

import java.io.File;

public class ProjectDialogManager {

    private final Context context;
    private final DialogActionListener listener;

    // Interface สำหรับส่งสัญญาณกลับไปจัดการและรีเฟรช Tree View ที่ MainActivity
    public interface DialogActionListener {
        void onTreeRefreshRequired(FileNode parentNode);
    }

    public ProjectDialogManager(Context context, DialogActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    // ตัวช่วยสร้างสไตล์กล่องรับข้อมูล (สี่เหลี่ยมขอบมน มีเส้นขอบบางสไตล์โปรแกรมเมอร์)
    private android.graphics.drawable.GradientDrawable createModernInputStyle() {
        android.graphics.drawable.GradientDrawable inputStyle = new android.graphics.drawable.GradientDrawable();
        inputStyle.setColor(android.graphics.Color.parseColor("#252526")); // สีกล่องสว่างกว่าพื้นหลังเล็กน้อย
        inputStyle.setCornerRadius((int) (8 * context.getResources().getDisplayMetrics().density)); // มุมโค้งมนกำลังดี
        inputStyle.setStroke((int) (1 * context.getResources().getDisplayMetrics().density), android.graphics.Color.parseColor("#3F3F46")); // เส้นขอบเทาบางๆ
        return inputStyle;
    }

    // 🟢 1. หน้าต่างสร้างไฟล์ใหม่ (เวอร์ชันยกระดับดีไซน์ พรีเมียมดาร์กโมด)
    public void showCreateFileDialog(File parentDir, FileNode parentNode) {
        com.google.android.material.bottomsheet.BottomSheetDialog inputDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_bottom_input, null);
        inputDialog.setContentView(dialogView);

        // 🌟 [แก้ไขจุดนี้] ทำการแปลงประเภทข้อมูล (Casting) เป็น View ให้ถูกต้อง ป้องกันคอมไพล์พัง
        if (dialogView.findViewById(R.id.tvInputTitle).getParent() instanceof View) {
            View sheetContainer = (View) dialogView.findViewById(R.id.tvInputTitle).getParent();
            sheetContainer.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvInputTitle);
        EditText etInput = dialogView.findViewById(R.id.etDialogInput);
        Button btnCancel = dialogView.findViewById(R.id.btnInputCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnInputConfirm);

        // จัดการฟอนต์และสีหัวข้อ
        tvTitle.setText("📄 สร้างไฟล์ใหม่");
        tvTitle.setTextColor(android.graphics.Color.WHITE);
        tvTitle.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));

        // แปลงโฉมช่องกรอก (EditText) ให้หล่อเหลาขึ้น
        int inputPadding = (int) (12 * context.getResources().getDisplayMetrics().density);
        etInput.setHint("ตัวอย่าง: index.html, main.js");
        etInput.setHintTextColor(android.graphics.Color.parseColor("#52525B"));
        etInput.setTextColor(android.graphics.Color.WHITE);
        etInput.setTextSize(14);
        etInput.setBackground(createModernInputStyle());
        etInput.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);

        // ปรับแต่งปุ่มยืนยันและยกเลิก
        btnCancel.setTextColor(android.graphics.Color.parseColor("#A1A1AA"));
        btnConfirm.setText("สร้างไฟล์");
        
        android.graphics.drawable.GradientDrawable confirmBtnBg = new android.graphics.drawable.GradientDrawable();
        confirmBtnBg.setColor(android.graphics.Color.parseColor("#248A3D")); // สีเขียว GitHub Success
        confirmBtnBg.setCornerRadius((int) (6 * context.getResources().getDisplayMetrics().density));
        btnConfirm.setBackground(confirmBtnBg);
        btnConfirm.setTextColor(android.graphics.Color.WHITE);

        btnCancel.setOnClickListener(v -> inputDialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String fileName = etInput.getText().toString().trim();
            if (!fileName.isEmpty()) {
                boolean success = FileSystemManager.createNewFile(parentDir, fileName);
                if (success) {
                    showToast("💾 สร้างไฟล์สำเร็จ");
                    if (listener != null) listener.onTreeRefreshRequired(parentNode);
                    inputDialog.dismiss();
                } else {
                    showToast("❌ ไม่สามารถสร้างไฟล์ได้ (ชื่อซ้ำหรือสิทธิ์ไม่พอ)");
                }
            }
        });

        inputDialog.show();
    }

    // 🟢 2. หน้าต่างสร้างโฟลเดอร์ใหม่ (เวอร์ชันยกระดับดีไซน์ พรีเมียมดาร์กโมด)
    public void showCreateFolderDialog(File parentDir, FileNode parentNode) {
        com.google.android.material.bottomsheet.BottomSheetDialog inputDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_bottom_input, null);
        inputDialog.setContentView(dialogView);

        // 🌟 [แก้ไขจุดนี้] ทำการแปลงประเภทข้อมูล (Casting) เป็น View ให้ถูกต้อง ป้องกันคอมไพล์พัง
        if (dialogView.findViewById(R.id.tvInputTitle).getParent() instanceof View) {
            View sheetContainer = (View) dialogView.findViewById(R.id.tvInputTitle).getParent();
            sheetContainer.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvInputTitle);
        EditText etInput = dialogView.findViewById(R.id.etDialogInput);
        Button btnCancel = dialogView.findViewById(R.id.btnInputCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnInputConfirm);

        tvTitle.setText("📁 สร้างโฟลเดอร์ใหม่");
        tvTitle.setTextColor(android.graphics.Color.WHITE);
        tvTitle.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));

        int inputPadding = (int) (12 * context.getResources().getDisplayMetrics().density);
        etInput.setHint("ตั้งชื่อโฟลเดอร์...");
        etInput.setHintTextColor(android.graphics.Color.parseColor("#52525B"));
        etInput.setTextColor(android.graphics.Color.WHITE);
        etInput.setTextSize(14);
        etInput.setBackground(createModernInputStyle());
        etInput.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);

        btnCancel.setTextColor(android.graphics.Color.parseColor("#A1A1AA"));
        btnConfirm.setText("สร้าง");
        
        android.graphics.drawable.GradientDrawable confirmBtnBg = new android.graphics.drawable.GradientDrawable();
        confirmBtnBg.setColor(android.graphics.Color.parseColor("#248A3D"));
        confirmBtnBg.setCornerRadius((int) (6 * context.getResources().getDisplayMetrics().density));
        btnConfirm.setBackground(confirmBtnBg);
        btnConfirm.setTextColor(android.graphics.Color.WHITE);

        btnCancel.setOnClickListener(v -> inputDialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String folderName = etInput.getText().toString().trim();
            if (!folderName.isEmpty()) {
                boolean success = FileSystemManager.createNewFolder(parentDir, folderName);
                if (success) {
                    showToast("💾 สร้างโฟลเดอร์สำเร็จ");
                    if (listener != null) listener.onTreeRefreshRequired(parentNode);
                    inputDialog.dismiss();
                } else {
                    showToast("❌ ไม่สามารถสร้างโฟลเดอร์ได้ (ชื่อซ้ำหรือสิทธิ์ไม่พอ)");
                }
            }
        });

        inputDialog.show();
    }

    // 🟢 3. หน้าต่างเปลี่ยนชื่อไฟล์/โฟลเดอร์ (เวอร์ชันยกระดับดีไซน์ พรีเมียมดาร์กโมด)
    public void showRenameDialog(File targetFile, FileNode targetNode) {
        com.google.android.material.bottomsheet.BottomSheetDialog inputDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_bottom_input, null);
        inputDialog.setContentView(dialogView);

        // 🌟 [แก้ไขจุดนี้] ทำการแปลงประเภทข้อมูล (Casting) เป็น View ให้ถูกต้อง ป้องกันคอมไพล์พัง
        if (dialogView.findViewById(R.id.tvInputTitle).getParent() instanceof View) {
            View sheetContainer = (View) dialogView.findViewById(R.id.tvInputTitle).getParent();
            sheetContainer.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvInputTitle);
        EditText etInput = dialogView.findViewById(R.id.etDialogInput);
        Button btnCancel = dialogView.findViewById(R.id.btnInputCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnInputConfirm);

        tvTitle.setText("✏️ เปลี่ยนชื่อ");
        tvTitle.setTextColor(android.graphics.Color.WHITE);
        tvTitle.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));

        int inputPadding = (int) (12 * context.getResources().getDisplayMetrics().density);
        etInput.setText(targetFile.getName());
        etInput.setSelectAllOnFocus(true);
        etInput.setHintTextColor(android.graphics.Color.parseColor("#52525B"));
        etInput.setTextColor(android.graphics.Color.WHITE);
        etInput.setTextSize(14);
        etInput.setBackground(createModernInputStyle());
        etInput.setPadding(inputPadding, inputPadding, inputPadding, inputPadding);

        btnCancel.setTextColor(android.graphics.Color.parseColor("#A1A1AA"));
        btnConfirm.setText("ตกลง");
        
        android.graphics.drawable.GradientDrawable confirmBtnBg = new android.graphics.drawable.GradientDrawable();
        confirmBtnBg.setColor(android.graphics.Color.parseColor("#248A3D"));
        confirmBtnBg.setCornerRadius((int) (6 * context.getResources().getDisplayMetrics().density));
        btnConfirm.setBackground(confirmBtnBg);
        btnConfirm.setTextColor(android.graphics.Color.WHITE);

        btnCancel.setOnClickListener(v -> inputDialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String newName = etInput.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(targetFile.getName())) {
                boolean success = FileSystemManager.renameFileOrFolder(targetFile, newName);
                if (success) {
                    showToast("💾 เปลี่ยนชื่อสำเร็จ");
                    if (listener != null) listener.onTreeRefreshRequired(targetNode);
                    inputDialog.dismiss();
                } else {
                    showToast("❌ เปลี่ยนชื่อล้มเหลว (ชื่อซ้ำกัน)");
                }
            }
        });

        inputDialog.show();
    }

    // 🟢 4. หน้าต่างยืนยันการลบสไตล์ดาร์กโมดสุดพรีเมียม ขอบมนเหลี่ยมเฉียบคม
    public void showDeleteConfirmationDialog(String targetName, final Runnable onDeleteConfirmed) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_confirm, null);
        
        TextView tvDialogMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnDialogConfirm);
        
        tvDialogMessage.setText("คุณต้องการลบ " + targetName + " ใช่หรือไม่?");
        tvDialogMessage.setTextColor(android.graphics.Color.WHITE);
        
        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setView(dialogView);
        
        if (dialog.getWindow() != null) {
            android.graphics.drawable.GradientDrawable dialogBg = new android.graphics.drawable.GradientDrawable();
            dialogBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
            dialogBg.setCornerRadius((int) (14 * context.getResources().getDisplayMetrics().density));
            dialog.getWindow().setBackgroundDrawable(dialogBg);
        }
        
        btnCancel.setTextColor(android.graphics.Color.parseColor("#A1A1AA"));
        
        android.graphics.drawable.GradientDrawable confirmDeleteBg = new android.graphics.drawable.GradientDrawable();
        confirmDeleteBg.setColor(android.graphics.Color.parseColor("#D32F2F"));
        confirmDeleteBg.setCornerRadius((int) (6 * context.getResources().getDisplayMetrics().density));
        btnConfirm.setBackground(confirmDeleteBg);
        btnConfirm.setTextColor(android.graphics.Color.WHITE);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDeleteConfirmed != null) {
                onDeleteConfirmed.run();
            }
        });
        
        dialog.show();
    }

    // 🆕 5. หน้าต่างแสดงตัวอย่างรูปภาพ (Image Viewer) เวอร์ชันปรับปรุงสัดส่วนอัตโนมัติ + เพิ่มปุ่มปิด
    public void showImageViewerDialog(File imageFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        android.widget.LinearLayout mainLayout = new android.widget.LinearLayout(context);
        mainLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);
        mainLayout.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));
        mainLayout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        TextView tvTitle = new TextView(context);
        tvTitle.setText(imageFile.getName());
        tvTitle.setTextColor(android.graphics.Color.WHITE);
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setGravity(android.view.Gravity.CENTER);
        tvTitle.setPadding(0, 0, 0, 24);
        mainLayout.addView(tvTitle);

        android.widget.ScrollView scrollView = new android.widget.ScrollView(context);
        android.widget.LinearLayout.LayoutParams scrollParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1.0f
        );
        scrollParams.setMargins(0, 0, 0, 24);
        scrollView.setLayoutParams(scrollParams);

        android.widget.ImageView imageView = new android.widget.ImageView(context);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);

        try {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                
                android.widget.LinearLayout.LayoutParams imgParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                imageView.setLayoutParams(imgParams);
                scrollView.addView(imageView);
                mainLayout.addView(scrollView);
            } else {
                TextView tvError = new TextView(context);
                tvError.setText("ไม่สามารถโหลดรูปภาพได้");
                tvError.setTextColor(android.graphics.Color.RED);
                tvError.setGravity(android.view.Gravity.CENTER);
                mainLayout.addView(tvError);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        final AlertDialog dialog = builder.setView(mainLayout).create();

        Button btnClose = new Button(context, null, 0, android.R.style.Widget_Material_Button_Borderless);
        btnClose.setText("ปิดหน้าต่าง");
        btnClose.setTextColor(android.graphics.Color.parseColor("#FF5252"));
        btnClose.setTextSize(14);
        btnClose.setAllCaps(false);
        
        android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnClose.setLayoutParams(btnParams);
        
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(android.graphics.Color.parseColor("#2D2D30"));
        btnBg.setCornerRadius(12);
        btnClose.setBackground(btnBg);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        mainLayout.addView(btnClose);

        if (dialog.getWindow() != null) {
            android.graphics.drawable.GradientDrawable dialogBg = new android.graphics.drawable.GradientDrawable();
            dialogBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
            dialogBg.setCornerRadius((int) (14 * context.getResources().getDisplayMetrics().density));
            dialog.getWindow().setBackgroundDrawable(dialogBg);
        }
        
        dialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
