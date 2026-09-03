package com.dev.ministudio.editor;

import android.graphics.Color;
import android.text.Editable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {

    private boolean isHighlighting = false;

    // ==================== สีสวย ทันสมัย ====================
    private final int COLOR_KEYWORD      = Color.parseColor("#FF79C6");   // ชมพูม่วง
    private final int COLOR_MODIFIER     = Color.parseColor("#FF79C6");
    private final int COLOR_CLASS        = Color.parseColor("#8BE9FD");   // ฟ้า
    private final int COLOR_METHOD       = Color.parseColor("#50FA7B");   // เขียว
    private final int COLOR_STRING       = Color.parseColor("#F1FA8C");   // เหลือง
    private final int COLOR_NUMBER       = Color.parseColor("#BD93F9");   // ม่วง
    private final int COLOR_COMMENT      = Color.parseColor("#6272A4");   // เท้า
    private final int COLOR_TAG          = Color.parseColor("#FF79C6");   // XML Tag
    private final int COLOR_ATTR         = Color.parseColor("#8BE9FD");   // XML Attribute
    private final int COLOR_IMPORT       = Color.parseColor("#BD93F9");
    private final int COLOR_ANNOTATION   = Color.parseColor("#FFB86C");   // ส้ม
    private final int COLOR_RESOURCE     = Color.parseColor("#FF5555");
    private final int COLOR_GRADLE_KEY   = Color.parseColor("#FF9E64");   // ส้มสว่าง (เฉพาะ Gradle)

    public void highlight(Editable s, File currentOpenedFile) {

        if (isHighlighting) return;
        isHighlighting = true;

        // ลบ span เก่า
        Object[] spans = s.getSpans(0, s.length(), ForegroundColorSpan.class);
        for (Object span : spans) {
            s.removeSpan(span);
        }

        String fileName = (currentOpenedFile != null) ? currentOpenedFile.getName().toLowerCase() : "";

        // ===================== JAVA =====================
        if (fileName.endsWith(".java") || fileName.endsWith(".kt")) {
            highlightJava(s);
        }
        // ===================== XML =====================
        else if (fileName.endsWith(".xml")) {
            highlightXml(s);
        }
        // ===================== GRADLE =====================
        else if (fileName.endsWith(".gradle")) {
            highlightGradle(s);
        }
        // ===================== YAML =====================
        else if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            highlightYaml(s);
        }

        isHighlighting = false;
    }

    private void highlightJava(Editable s) {
        highlightPattern(s, "\\b(package|class|interface|enum|extends|implements|static|final|void|new|if|else|for|while|do|switch|case|default|try|catch|finally|return|throw|break|continue|this|super)\\b", COLOR_KEYWORD);
        highlightPattern(s, "\\b(public|private|protected|abstract|synchronized|volatile|transient)\\b", COLOR_MODIFIER);
        highlightPattern(s, "\\b(String|int|long|float|double|boolean|char|byte|short|void|Object|List|ArrayList|Map|HashMap|File|Context|Intent|Bundle|View|Activity|Fragment|Handler|Runnable|Thread)\\b", COLOR_CLASS);
        highlightPattern(s, "\\bimport\\b", COLOR_IMPORT);
        highlightPattern(s, "@[a-zA-Z_][a-zA-Z0-9_]*", COLOR_ANNOTATION);
        highlightPattern(s, "\"([^\"\\\\]|\\\\.)*\"", COLOR_STRING);
        highlightPattern(s, "\\b\\d+\\.?\\d*[fFL]?\\b", COLOR_NUMBER);
        highlightPattern(s, "//.*|/\\*(?:.|[\\n\\r])*?\\*/", COLOR_COMMENT);
        highlightPattern(s, "\\b(?!if|for|while|switch|catch|try|else|new|return)[a-zA-Z_][a-zA-Z0-9_]*(?=\\s*\\()", COLOR_METHOD);
        highlightPattern(s, "\\bR\\.[a-zA-Z0-9_\\.]+", COLOR_RESOURCE);
    }

    private void highlightXml(Editable s) {
        highlightPattern(s, "</?[a-zA-Z][a-zA-Z0-9._:-]*", COLOR_TAG);
        highlightPattern(s, "\\b[a-zA-Z][a-zA-Z0-9._:-]*(?=\\s*=)", COLOR_ATTR);
        highlightPattern(s, "\"[^\"]*\"", COLOR_STRING);
    }

    private void highlightGradle(Editable s) {
        // Gradle Keywords
        highlightPattern(s, "\\b(plugins|dependencies|android|repositories|buildscript|allprojects|tasks|task|ext|group|version|apply|id|implementation|api|testImplementation|androidTestImplementation|compileSdk|minSdk|targetSdk|namespace|buildTypes|defaultConfig|compileOptions|packagingOptions)\\b", COLOR_GRADLE_KEY);

        // Common strings and versions
        highlightPattern(s, "\"[^\"]*\"", COLOR_STRING);
        highlightPattern(s, "\\b\\d+\\.?\\d*\\b", COLOR_NUMBER);

        // Comments
        highlightPattern(s, "//.*|/\\*(?:.|[\\n\\r])*?\\*/", COLOR_COMMENT);
    }

    private void highlightYaml(Editable s) {
        highlightPattern(s, "\\b(true|false|null)\\b", COLOR_KEYWORD);
        highlightPattern(s, "\"[^\"]*\"", COLOR_STRING);
        highlightPattern(s, "\\b\\d+\\.?\\d*\\b", COLOR_NUMBER);
        highlightPattern(s, "#.*", COLOR_COMMENT);
    }

    private void highlightPattern(Editable s, String regex, int color) {
        Matcher matcher = Pattern.compile(regex).matcher(s);
        while (matcher.find()) {
            s.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), 0);
        }
    }
}
