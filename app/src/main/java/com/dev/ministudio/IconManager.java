package com.dev.ministudio;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IconManager {

    // 1. เปลี่ยนมาสแกนไฟล์จากโฟลเดอร์ในเครื่องแทนการดึงจาก R.drawable
    public static List<String> getAllIconPaths(Context context) {
        List<String> iconPaths = new ArrayList<>();
        
        // สมมติว่าน้าเก็บไอคอนที่แตกมาไว้ในโฟลเดอร์นี้
        File iconFolder = new File(context.getExternalFilesDir(null), "icons");
        
        if (iconFolder.exists() && iconFolder.isDirectory()) {
            File[] files = iconFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    // กรองเอาเฉพาะไฟล์ .png หรือ .jpg
                    if (file.getName().endsWith(".png") || file.getName().endsWith(".jpg")) {
                        iconPaths.add(file.getAbsolutePath());
                    }
                }
            }
        }
        return iconPaths;
    }

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_SELECTED_ICON_PATH = "selected_icon_path";

    // 2. เปลี่ยนจากการเก็บ int resId มาเป็น String path
    public static void saveSelectedIconPath(Context context, String path) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
               .edit().putString(KEY_SELECTED_ICON_PATH, path).apply();
    }

    // ฟังก์ชันสำหรับดึงค่า Path ที่บันทึกไว้ไปใช้งาน
    public static String getSelectedIconPath(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                      .getString(KEY_SELECTED_ICON_PATH, null);
    }
}
