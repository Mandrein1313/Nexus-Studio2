package com.dev.ministudio;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * จัดการค้นหา / แทนที่ใน CodeEditor (Sora)
 */
public class EditorSearchManager {

    private final Activity activity;
    private final CodeEditor codeEditor;

    private View searchBar;
    private EditText etFind;
    private EditText etReplace;
    private TextView tvSearchCount;
    private CheckBox cbCaseSensitive;

    private int searchMatchIndex = -1;
    private final ArrayList<int[]> searchMatches = new ArrayList<>(); // [start, end]

    public EditorSearchManager(Activity activity, CodeEditor codeEditor) {
        this.activity = activity;
        this.codeEditor = codeEditor;
    }

    /** ผูก view จาก layout แล้วตั้ง listener */
    public void bindViews(View root) {
        searchBar = root.findViewById(R.id.searchBar);
        etFind = root.findViewById(R.id.etFind);
        etReplace = root.findViewById(R.id.etReplace);
        tvSearchCount = root.findViewById(R.id.tvSearchCount);
        cbCaseSensitive = root.findViewById(R.id.cbCaseSensitive);

        View btnNext = root.findViewById(R.id.btnSearchNext);
        if (btnNext == null) btnNext = root.findViewById(R.id.btnNext);

        View btnPrev = root.findViewById(R.id.btnSearchPrev);
        View btnClose = root.findViewById(R.id.btnCloseSearch);
        View btnReplace = root.findViewById(R.id.btnReplace);
        View btnReplaceAll = root.findViewById(R.id.btnReplaceAll);

        if (btnNext != null) btnNext.setOnClickListener(v -> findNext());
        if (btnPrev != null) btnPrev.setOnClickListener(v -> findPrev());
        if (btnReplace != null) btnReplace.setOnClickListener(v -> replaceOne());
        if (btnReplaceAll != null) btnReplaceAll.setOnClickListener(v -> replaceAll());
        if (btnClose != null) btnClose.setOnClickListener(v -> hide());

        if (etFind != null) {
            etFind.setOnEditorActionListener((tv, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    findNext();
                    return true;
                }
                findNext();
                return true;
            });
            etFind.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override
                public void afterTextChanged(Editable s) {
                    rebuildSearchMatches();
                    if (!searchMatches.isEmpty()) {
                        searchMatchIndex = 0;
                        selectCurrentMatch();
                    } else {
                        updateSearchCountLabel();
                    }
                }
            });
        }

        if (cbCaseSensitive != null) {
            cbCaseSensitive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                rebuildSearchMatches();
                if (!searchMatches.isEmpty()) {
                    searchMatchIndex = 0;
                    selectCurrentMatch();
                } else {
                    updateSearchCountLabel();
                }
            });
        }
    }

    public void toggle() {
        if (searchBar == null) return;
        if (searchBar.getVisibility() == View.VISIBLE) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        if (searchBar == null) return;
        searchBar.setVisibility(View.VISIBLE);
        if (etFind != null) {
            etFind.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etFind, InputMethodManager.SHOW_IMPLICIT);
            }
            rebuildSearchMatches();
        }
    }

    public void hide() {
        if (searchBar != null) searchBar.setVisibility(View.GONE);
        searchMatches.clear();
        searchMatchIndex = -1;
        updateSearchCountLabel();
    }

    public boolean isVisible() {
        return searchBar != null && searchBar.getVisibility() == View.VISIBLE;
    }

    private boolean isCaseSensitive() {
        return cbCaseSensitive != null && cbCaseSensitive.isChecked();
    }

    private void rebuildSearchMatches() {
        searchMatches.clear();
        searchMatchIndex = -1;

        if (codeEditor == null || etFind == null) {
            updateSearchCountLabel();
            return;
        }

        String query = etFind.getText().toString();
        if (query.isEmpty()) {
            updateSearchCountLabel();
            return;
        }

        String content = codeEditor.getText().toString();
        String src = isCaseSensitive() ? content : content.toLowerCase(Locale.ROOT);
        String q = isCaseSensitive() ? query : query.toLowerCase(Locale.ROOT);

        int from = 0;
        while (true) {
            int idx = src.indexOf(q, from);
            if (idx < 0) break;
            searchMatches.add(new int[]{idx, idx + query.length()});
            from = idx + Math.max(1, q.length());
        }
        updateSearchCountLabel();
    }

    private void updateSearchCountLabel() {
        if (tvSearchCount == null) return;
        if (searchMatches.isEmpty()) {
            String q = etFind != null ? etFind.getText().toString() : "";
            tvSearchCount.setText(q.isEmpty() ? "" : "0/0");
            tvSearchCount.setTextColor(Color.parseColor("#565F89"));
        } else {
            int display = searchMatchIndex < 0 ? 0 : (searchMatchIndex + 1);
            tvSearchCount.setText(display + "/" + searchMatches.size());
            tvSearchCount.setTextColor(Color.parseColor("#7AA2F7"));
        }
    }

    public void findNext() {
        if (codeEditor == null) {
            toast("ยังไม่ได้เปิดไฟล์");
            return;
        }
        if (searchMatches.isEmpty()) rebuildSearchMatches();
        if (searchMatches.isEmpty()) {
            String q = etFind != null ? etFind.getText().toString() : "";
            toast("ไม่พบ \"" + q + "\"");
            updateSearchCountLabel();
            return;
        }
        searchMatchIndex = (searchMatchIndex + 1) % searchMatches.size();
        selectCurrentMatch();
    }

    public void findPrev() {
        if (codeEditor == null) return;
        if (searchMatches.isEmpty()) rebuildSearchMatches();
        if (searchMatches.isEmpty()) {
            toast("ไม่พบ");
            return;
        }
        searchMatchIndex--;
        if (searchMatchIndex < 0) searchMatchIndex = searchMatches.size() - 1;
        selectCurrentMatch();
    }

    private void selectCurrentMatch() {
        if (searchMatchIndex < 0 || searchMatchIndex >= searchMatches.size()) return;
        int[] m = searchMatches.get(searchMatchIndex);
        selectLinear(m[0], m[1]);
        updateSearchCountLabel();
    }

    public void replaceOne() {
        if (codeEditor == null || etFind == null || etReplace == null) return;
        String target = etFind.getText().toString();
        String replacement = etReplace.getText().toString();
        if (target.isEmpty()) return;

        if (searchMatches.isEmpty()) rebuildSearchMatches();
        if (searchMatches.isEmpty()) {
            toast("ไม่พบคำที่ต้องการแทนที่");
            return;
        }
        if (searchMatchIndex < 0) searchMatchIndex = 0;

        int[] m = searchMatches.get(searchMatchIndex);
        int[] start = indexToLineCol(m[0]);
        int[] end = indexToLineCol(m[1]);
        codeEditor.getText().delete(start[0], start[1], end[0], end[1]);
        codeEditor.getText().insert(start[0], start[1], replacement);

        rebuildSearchMatches();
        if (!searchMatches.isEmpty()) {
            searchMatchIndex = Math.min(searchMatchIndex, searchMatches.size() - 1);
            selectCurrentMatch();
        }
        toast("แทนที่แล้ว");
    }

    public void replaceAll() {
        if (codeEditor == null || etFind == null || etReplace == null) return;
        String target = etFind.getText().toString();
        String replacement = etReplace.getText().toString();
        if (target.isEmpty()) return;

        String content = codeEditor.getText().toString();
        int count;
        String newContent;

        if (isCaseSensitive()) {
            count = 0;
            int from = 0;
            while ((from = content.indexOf(target, from)) >= 0) {
                count++;
                from += target.length();
            }
            newContent = content.replace(target, replacement);
        } else {
            Pattern p = Pattern.compile(
                    Pattern.quote(target),
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = p.matcher(content);
            StringBuffer sb = new StringBuffer();
            count = 0;
            while (matcher.find()) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                count++;
            }
            matcher.appendTail(sb);
            newContent = sb.toString();
        }

        if (count == 0) {
            toast("ไม่พบคำที่ต้องการแทนที่");
            return;
        }
        codeEditor.setText(newContent);
        rebuildSearchMatches();
        toast("แทนที่ทั้งหมด " + count + " จุด");
    }

    private int[] indexToLineCol(int index) {
        String text = codeEditor.getText().toString();
        int line = 0, col = 0, current = 0;
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineLen = lines[i].length() + (i < lines.length - 1 ? 1 : 0);
            if (current + lineLen > index || i == lines.length - 1) {
                line = i;
                col = Math.min(Math.max(0, index - current), lines[i].length());
                break;
            }
            current += lineLen;
        }
        return new int[]{line, col};
    }

    private void selectLinear(int startIdx, int endIdx) {
        try {
            int[] s = indexToLineCol(startIdx);
            int[] e = indexToLineCol(endIdx);
            activity.runOnUiThread(() -> {
                codeEditor.setSelectionRegion(s[0], s[1], e[0], e[1]);
                codeEditor.jumpToLine(s[0]);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toast(String msg) {
        activity.runOnUiThread(() ->
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
    }
}