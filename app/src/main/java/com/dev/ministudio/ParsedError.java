package com.dev.ministudio;

public class ParsedError {
    public String file;     // ที่อยู่ไฟล์ เช่น app/src/main/java/...
    public int line;        // บรรทัดที่พัง
    public int column;      // ตัวอักษรตัวที่พัง (คอลัมน์)
    public String type;      // ประเภท (เช่น JAVA_ERROR)
    public String message;   // ข้อความเตือน เช่น cannot find symbol

    public ParsedError(String file, int line, int column, String type, String message) {
        this.file = file;
        this.line = line;
        this.column = column;
        this.type = type;
        this.message = message;
    }
}
