package com.dev.ministudio;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

/**
 * ค้นหา / แทนที่ + ประวัติการค้นหา สำหรับ Sora CodeEditor
 */
public class EditorSearchManager {

    private static final int SEARCH_HIGHLIGHT = 0xFFE0AF68; // ทอง — คำที่ค้นเจอ
    private static final int NORMAL_SELECTION = 0xFF364A82; // ฟ้า — เลือกข้อความปกติ

    private static final String PREFS_SEARCH = "EditorSearchHistory";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_HISTORY = 12;

    private final Activity activity;
    private final CodeEditor codeEditor;

    private View searchBar;
    private EditText etFind;
    private EditText etReplace;
    private TextView tvSearchCount;
    private CheckBox cbCaseSensitive;

    private ListView lvSearchHistory;
    private ArrayAdapter<String> historyAdapter;
    private final ArrayList<String> searchHistory = new ArrayList<>();

    private int searchMatchIndex = -1;
    private final ArrayList<int[]> searchMatches = new ArrayList<>(); // [start, end]

    public EditorSearchManager(Activity activity, CodeEditor codeEditor) {
        this.activity = activity;
        this.codeEditor = codeEditor;
    }

    public void bindViews(View root) {
        searchBar = root.findViewById(R.id.searchBar);
        etFind = root.findViewById(R.id.etFind);
        etReplace = root.findViewById(R.id.etReplace);
        tvSearchCount = root.findViewById(R.id.tvSearchCount);
        cbCaseSensitive = root.findViewById(R.id.cbCaseSensitive);

        View btnNext = root.findViewById(R.id.btnSearchNext);
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
                findNext();
                return true;
            });
            etFind.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override
                public void afterTextChanged(Editable s) {
                    // ประวัติ: ช่องว่าง + มีประวัติ → โชว์
                    if (s.length() == 0 && !searchHistory.isEmpty() && etFind.hasFocus()) {
                        showHistoryPopup();
                    } else {
                        hideHistoryPopup();
                    }

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

        setupSearchHistory();
    }

    // ===================== จัดการโฟกัส Editor =====================

    private void setEditorFocusEnabled(boolean enabled) {
        if (codeEditor == null) return;
        codeEditor.setFocusable(enabled);
        codeEditor.setFocusableInTouchMode(enabled);
        if (!enabled) {
            codeEditor.clearFocus();
        }
    }

    // ===================== ประวัติการค้นหา =====================

    private void setupSearchHistory() {
        loadHistory();
        if (searchBar == null || !(searchBar.getParent() instanceof ViewGroup)) return;

        float density = activity.getResources().getDisplayMetrics().density;

        lvSearchHistory = new ListView(activity);
        lvSearchHistory.setBackgroundColor(Color.parseColor("#1F2335"));
        lvSearchHistory.setDivider(new ColorDrawable(Color.parseColor("#292E42")));
        lvSearchHistory.setDividerHeight(1);
        lvSearchHistory.setVisibility(View.GONE);

        ViewGroup parent = (ViewGroup) searchBar.getParent();
        int idx = parent.indexOfChild(searchBar);
        parent.addView(lvSearchHistory, idx + 1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (180 * density)
        ));

        historyAdapter = new ArrayAdapter<String>(
                activity, android.R.layout.simple_list_item_1, searchHistory) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.parseColor("#C0CAF5"));
                tv.setTextSize(14);
                tv.setPadding(32, 28, 32, 28);
                return tv;
            }
        };
        lvSearchHistory.setAdapter(historyAdapter);

        lvSearchHistory.setOnItemClickListener((p, v, position, id) -> {
            if (position < 0 || position >= searchHistory.size()) return;
            String q = searchHistory.get(position);
            if (etFind != null) {
                etFind.setText(q);
                etFind.setSelection(q.length());
            }
            hideHistoryPopup();
            rebuildSearchMatches();
            if (!searchMatches.isEmpty()) {
                searchMatchIndex = 0;
                selectCurrentMatch();
            } else {
                findNext();
            }
        });

        lvSearchHistory.setOnItemLongClickListener((p, v, position, id) -> {
            if (position >= 0 && position < searchHistory.size()) {
                searchHistory.remove(position);
                saveHistory();
                historyAdapter.notifyDataSetChanged();
                if (searchHistory.isEmpty()) hideHistoryPopup();
                toast("ลบประวัติแล้ว");
            }
            return true;
        });

        View.OnFocusChangeListener searchFocusListener = (v, hasFocus) -> {
            if (hasFocus) {
                setEditorFocusEnabled(false);
                if (v == etFind && etFind.getText().toString().isEmpty() && !searchHistory.isEmpty()) {
                    showHistoryPopup();
                }
            } else {
                if (v == etFind) {
                    etFind.postDelayed(this::hideHistoryPopup, 200);
                }
            }
        };

        if (etFind != null) etFind.setOnFocusChangeListener(searchFocusListener);
        if (etReplace != null) etReplace.setOnFocusChangeListener(searchFocusListener);
    }

    private void showHistoryPopup() {
        if (lvSearchHistory == null || searchHistory.isEmpty()) return;
        if (historyAdapter != null) historyAdapter.notifyDataSetChanged();
        lvSearchHistory.setVisibility(View.VISIBLE);
    }

    private void hideHistoryPopup() {
        if (lvSearchHistory != null) {
            lvSearchHistory.setVisibility(View.GONE);
        }
    }

    private void addToHistory(String query) {
        if (query == null) return;
        query = query.trim();
        if (query.isEmpty()) return;

        searchHistory.remove(query);
        searchHistory.add(0, query);
        while (searchHistory.size() > MAX_HISTORY) {
            searchHistory.remove(searchHistory.size() - 1);
        }
        saveHistory();
        if (historyAdapter != null) historyAdapter.notifyDataSetChanged();
    }

    private void loadHistory() {
        searchHistory.clear();
        String raw = activity.getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
                .getString(KEY_HISTORY, "");
        if (raw == null || raw.isEmpty()) return;
        for (String s : raw.split("\u0001")) {
            if (!s.isEmpty()) searchHistory.add(s);
        }
    }

    private void saveHistory() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < searchHistory.size(); i++) {
            if (i > 0) sb.append('\u0001');
            sb.append(searchHistory.get(i));
        }
        activity.getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HISTORY, sb.toString())
                .apply();
    }

    // ===================== เปิด / ปิดแถบ =====================

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

        // ตัดโฟกัส editor ไม่ให้แย่งรับปุ่มจากคีย์บอร์ด
        setEditorFocusEnabled(false);

        if (etFind != null) {
            etFind.setFocusable(true);
            etFind.setFocusableInTouchMode(true);
            etFind.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etFind, InputMethodManager.SHOW_IMPLICIT);
            }
            rebuildSearchMatches();
            if (etFind.getText().toString().isEmpty() && !searchHistory.isEmpty()) {
                showHistoryPopup();
            }
        }
    }

    public void hide() {
        if (searchBar == null) return;
        searchBar.setVisibility(View.GONE);
        hideHistoryPopup();
        searchMatches.clear();
        searchMatchIndex = -1;
        updateSearchCountLabel();

        // คืนโฟกัสให้ editor
        setEditorFocusEnabled(true);

        if (codeEditor != null) {
            codeEditor.getColorScheme().setColor(
                    EditorColorScheme.SELECTED_TEXT_BACKGROUND,
                    NORMAL_SELECTION
            );
            try {
                codeEditor.requestFocus();
                int line = codeEditor.getCursor().getLeftLine();
                int col = codeEditor.getCursor().getLeftColumn();
                codeEditor.setSelection(line, col);
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isVisible() {
        return searchBar != null && searchBar.getVisibility() == View.VISIBLE;
    }

    // ===================== ค้นหา / แทนที่ =====================

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

        if (etFind != null) addToHistory(etFind.getText().toString());
        hideHistoryPopup();

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
                codeEditor.getColorScheme().setColor(
                        EditorColorScheme.SELECTED_TEXT_BACKGROUND,
                        SEARCH_HIGHLIGHT
                );
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
