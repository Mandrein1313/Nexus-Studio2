package com.dev.ministudio.editor;

import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

/**
 * ธีม Tokyo Night โทนม่วง สำหรับ Nexus Studio
 * ใช้เฉพาะ color ID ที่มีใน Sora 0.24.x
 */
public class NexusColorScheme extends EditorColorScheme {

    @Override
    public void applyDefault() {
        super.applyDefault();

        // ===== พื้นหลัง =====
        setColor(WHOLE_BACKGROUND, 0xFF1A1B26);
        setColor(TEXT_NORMAL, 0xFFA9B1D6);
        setColor(CURRENT_LINE, 0xFF2A2F48);           // เด่นขึ้นเล็กน้อย
        setColor(LINE_NUMBER, 0xFF7AA2F7);            // เลขบรรทัดสีฟ้าเด่น
        setColor(LINE_NUMBER_BACKGROUND, 0xFF1A1B26);
        setColor(LINE_DIVIDER, 0xFF7AA2F7);           // เส้นข้างเลขบรรทัด — ฟ้าชัด

        // ===== การเลือกข้อความ =====
        setColor(SELECTED_TEXT_BACKGROUND, 0xFF3D59A1); // แถบเลือกเด่นขึ้น
        setColor(SELECTION_INSERT, 0xFFC0CAF5);
        setColor(SELECTION_HANDLE, 0xFFBB9AF7);        // จุดจับ cursor ม่วง

        // ===== ไวยากรณ์ =====
        setColor(KEYWORD, 0xFFBB9AF7);
        setColor(IDENTIFIER_NAME, 0xFF7DCFFF);
        setColor(IDENTIFIER_VAR, 0xFFC0CAF5);
        setColor(LITERAL, 0xFF9ECE6A);
        setColor(OPERATOR, 0xFF89DDFF);
        setColor(COMMENT, 0xFF565F89);
        setColor(ANNOTATION, 0xFFE0AF68);

        // ===== วงเล็บ / block / เส้นบอกบรรทัด =====
        setColor(BLOCK_LINE, 0xFF3B4261);
        setColor(BLOCK_LINE_CURRENT, 0xFFBB9AF7);      // เส้นบล็อกบรรทัดปัจจุบัน — ม่วงเด่น
        setColor(MATCHED_TEXT_BACKGROUND, 0xFF3D59A1);
        setColor(UNDERLINE, 0xFFE0AF68);               // เส้นใต้ — ทองชัด

        // ===== scrollbar =====
        setColor(SCROLL_BAR_THUMB, 0xFF3B4261);
        setColor(SCROLL_BAR_THUMB_PRESSED, 0xFFBB9AF7);
        setColor(SCROLL_BAR_TRACK, 0xFF1A1B26);
    }
}