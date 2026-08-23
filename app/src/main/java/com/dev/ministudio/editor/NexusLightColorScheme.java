package com.dev.ministudio.editor;

import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

/**
 * ธีมสว่างแบบ GitHub Light สำหรับ Nexus Studio
 */
public class NexusLightColorScheme extends EditorColorScheme {

    @Override
    public void applyDefault() {
        super.applyDefault();

        // ===== พื้นหลัง =====
        setColor(WHOLE_BACKGROUND, 0xFFFFFFFF);
        setColor(TEXT_NORMAL, 0xFF24292F);
        setColor(CURRENT_LINE, 0xFFFAFBFC);
        setColor(LINE_NUMBER, 0xFF8C959F);
        setColor(LINE_NUMBER_BACKGROUND, 0xFFFFFFFF);
        setColor(LINE_DIVIDER, 0xFFD0D7DE);

        // ===== การเลือกข้อความ =====
        setColor(SELECTED_TEXT_BACKGROUND, 0xFFADD6FF);
        setColor(SELECTION_INSERT, 0xFF0969DA);
        setColor(SELECTION_HANDLE, 0xFF0969DA);

        // ===== ไวยากรณ์ =====
        setColor(KEYWORD, 0xFFCF222E);          // แดง — public, class, if
        setColor(IDENTIFIER_NAME, 0xFF8250DF);  // ม่วง — ชื่อเมธอด
        setColor(IDENTIFIER_VAR, 0xFF24292F);   // เทาเข้ม — ตัวแปร
        setColor(LITERAL, 0xFF0A3069);          // น้ำเงินเข้ม — string / ตัวเลข
        setColor(OPERATOR, 0xFFCF222E);         // แดง — { } ( ) ;
        setColor(COMMENT, 0xFF6E7781);          // เทา — // comment
        setColor(ANNOTATION, 0xFF116329);       // เขียว — @Override

        // ===== วงเล็บ / block =====
        setColor(BLOCK_LINE, 0xFFD0D7DE);
        setColor(BLOCK_LINE_CURRENT, 0xFF0969DA);
        setColor(MATCHED_TEXT_BACKGROUND, 0xFFFFF8C5);
        setColor(UNDERLINE, 0xFF0969DA);

        // ===== scrollbar =====
        setColor(SCROLL_BAR_THUMB, 0xFFD0D7DE);
        setColor(SCROLL_BAR_THUMB_PRESSED, 0xFF8C959F);
        setColor(SCROLL_BAR_TRACK, 0xFFFFFFFF);
    }
}
