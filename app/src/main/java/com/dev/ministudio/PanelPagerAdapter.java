package com.dev.ministudio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PanelPagerAdapter extends RecyclerView.Adapter<PanelPagerAdapter.ViewHolder> {

    // โครงสร้างคลาสสำหรับจำลองเก็บข้อความ Log แต่ละบรรทัดแยกตามกลุ่มประเภท
    public static class LogLine {
        public String text;
        public int color;
        public int type; // 0=ทั่วไป, 1=Error, 2=Warning

        public LogLine(String text, int color, int type) {
            this.text = text;
            this.color = color;
            this.type = type;
        }
    }

    private final Context context;
    private View tvConsoleView; 
    private WebView webAiOutput; 
    private EditText etAiInput;
    private ImageView btnSendAi;
    private ImageView btnStopAiVoice; 

    // ตัวแปรเพิ่มใหม่สำหรับคุมแผงควบคุมและคัดกรอง Log
    private final List<LogLine> allLogLines = new ArrayList<>();
    private int currentFilterType = 0; // 0=All, 1=Errors, 2=Warnings
    private boolean isAutoScroll = true;
    private ScrollView consoleScrollView;

    public PanelPagerAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_console, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.layout_ai, parent, false);
        }
        return new ViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == 0) {
            // 🛠️ ผูกตำแหน่งหน้าแผงคอนโซลเวอร์ชันอัจฉริยะชุดใหม่
            tvConsoleView = holder.itemView.findViewById(R.id.tvConsole);
            consoleScrollView = holder.itemView.findViewById(R.id.consoleScrollView);
            
            TabLayout filterTabs = holder.itemView.findViewById(R.id.consoleFilterTabs);
            ImageView btnClear = holder.itemView.findViewById(R.id.btnConsoleClear);
            ImageView btnScroll = holder.itemView.findViewById(R.id.btnConsoleScrollDown);

            // อนุญาตให้ TextView ในคอนโซลตรวจจับและกดคลิกลิงก์ Error ได้
            if (tvConsoleView != null) {
                ((TextView) tvConsoleView).setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            }

            // ตั้งค่าหัวแท็บสลับดูตามสถานะล็อก
            if (filterTabs != null && filterTabs.getTabCount() == 0) {
                filterTabs.addTab(filterTabs.newTab().setText("All Logs"));
                filterTabs.addTab(filterTabs.newTab().setText("Errors"));
                filterTabs.addTab(filterTabs.newTab().setText("Warnings"));
                
                filterTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        currentFilterType = tab.getPosition();
                        renderFilteredLogs(); // รีเฟรชวาดกระดานใหม่เมื่อเปลี่ยนแท็บ
                    }
                    @Override public void onTabUnselected(TabLayout.Tab tab) {}
                    @Override public void onTabReselected(TabLayout.Tab tab) {}
                });
            }

            // ปุ่มล้าง Log ในถังเก็บทั้งหมด
            if (btnClear != null) {
                btnClear.setOnClickListener(v -> {
                    allLogLines.clear();
                    if (tvConsoleView != null) {
                        ((TextView) tvConsoleView).setText("");
                    }
                });
            }

            // ปุ่มสลับโหมดล็อกหน้าจอเลื่อนลงล่างอัตโนมัติ (Auto Scroll)
            if (btnScroll != null) {
                // เซ็ตสีปุ่มเริ่มต้นตามสถานะจริง
                btnScroll.setColorFilter(isAutoScroll ? android.graphics.Color.parseColor("#007ACC") : android.graphics.Color.parseColor("#8C8C8C"));
                btnScroll.setOnClickListener(v -> {
                    isAutoScroll = !isAutoScroll;
                    btnScroll.setColorFilter(isAutoScroll ? android.graphics.Color.parseColor("#007ACC") : android.graphics.Color.parseColor("#8C8C8C"));
                    if (isAutoScroll && consoleScrollView != null) {
                        consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
                    }
                });
            }

        } else {
            // 🤖 ส่วนควบคุมหน้าต่างถาม-ตอบบอท AI ตัวเดิมของน้าทั้งหมด (คงเดิมไว้ 100%)
            webAiOutput = holder.itemView.findViewById(R.id.webAiOutput); 
            etAiInput = holder.itemView.findViewById(R.id.etAiInput);
            btnSendAi = holder.itemView.findViewById(R.id.btnSendAi);
            btnStopAiVoice = holder.itemView.findViewById(R.id.btnStopAiVoice); 
            
            if (webAiOutput != null) {
                webAiOutput.getSettings().setJavaScriptEnabled(true);
                webAiOutput.getSettings().setDomStorageEnabled(true);
                webAiOutput.setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"));
                
                if (context instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) context;
                    webAiOutput.removeJavascriptInterface("AndroidBridge");
                    webAiOutput.addJavascriptInterface(mainActivity.new WebAppInterface(context), "AndroidBridge");
                }
            }

            if (btnStopAiVoice != null) {
                btnStopAiVoice.setOnClickListener(v -> {
                    if (context instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) context;
                        if (mainActivity.aiLayoutAnalyzer != null) {
                            mainActivity.aiLayoutAnalyzer.stopSpeaking();
                            Toast.makeText(context, "🤫 หยุดเล่นเสียงชั่วคราว", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            if (btnSendAi != null) {
                btnSendAi.setOnClickListener(v -> {
                    if (context instanceof MainActivity) {
                        try {
                            ((MainActivity) context).handleAiQuery();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    /**
     * ➕ [เพิ่มใหม่]: ฟังก์ชันพ่นประวัติ Log ทั้งถังออกทางหน้าจอตามแท็บกรองที่กดเลือกไว้
     */
    private void renderFilteredLogs() {
        if (tvConsoleView == null) return;
        TextView tv = (TextView) tvConsoleView;
        tv.setText(""); // ล้างจอเพื่อพ่นเรียงบรรทัดใหม่
        
        for (LogLine line : allLogLines) {
            if (currentFilterType == 0 || 
               (currentFilterType == 1 && line.type == 1) || 
               (currentFilterType == 2 && line.type == 2)) {
                
                // ใช้ตัวแปลงเพื่อทำลิงก์ให้โค้ดส่วนที่ Error สามารถกดคลิกได้
                tv.append(makeErrorClickable(line.text, line.color));
            }
        }
        
        if (isAutoScroll && consoleScrollView != null) {
            consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    /**
     * ➕ [เพิ่มใหม่]: ฟังก์ชันปลายทางรับสัญญาณยิงพ่น Log สดๆ ผ่านระบบคัดกรอง
     */
    public void postNewLog(String text, int color) {
        String lower = text.toLowerCase();
        int type = 0; // ทั่วไป
        if (lower.contains("error:") || lower.contains("failed")) {
            type = 1; // แยกเข้าถังกลุ่ม Error
        } else if (lower.contains("warning:") || lower.contains("deprecated")) {
            type = 2; // แยกเข้าถังกลุ่ม Warning
        }

        allLogLines.add(new LogLine(text, color, type));

        // ถ้าข้อความที่เด้งเข้ามา ตรงกับหน้าแท็บปัจจุบันที่ผู้ใช้กำลังส่องดูอยู่ ให้ปริ้นสดออกจอเลยครับน้า
        if (currentFilterType == 0 || currentFilterType == type) {
            if (tvConsoleView != null) {
                final int finalType = type;
                if (context instanceof MainActivity) {
                    ((MainActivity) context).runOnUiThread(() -> {
                        TextView tv = (TextView) tvConsoleView;
                        tv.append(makeErrorClickable(text, color));
                        
                        if (isAutoScroll && consoleScrollView != null) {
                            consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
                        }
                    });
                }
            }
        }
    }

    /**
     * ➕ [เพิ่มใหม่]: ฟังก์ชัน Regex คัดกรองตรวจจับท่อนรหัสพังเพื่อทำเป็นลิงก์วาร์ปกลับไปหน้าบอร์ดเขียนโค้ด
     */
    private android.text.SpannableString makeErrorClickable(String text, int defaultColor) {
        android.text.SpannableString spannable = new android.text.SpannableString(text);
        spannable.setSpan(new android.text.style.ForegroundColorSpan(defaultColor), 0, text.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // ดักจับรูปแบบพวก ชื่อไฟล์.java:เลขบรรทัด (เช่น MainActivity.java:45)
        Pattern pattern = Pattern.compile("([a-zA-Z0-9_]+\\.java):(\\d+)");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            final String fileName = matcher.group(1);
            final int lineNumber = Integer.parseInt(matcher.group(2));

            int start = matcher.start();
            int end = matcher.end();
            
            // ปรับสีท่อนลิงก์เป็นสีฟ้าสะดุดตาพร้อมขีดเส้นใต้
            spannable.setSpan(new android.text.style.UnderlineSpan(), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#4FC3F7")), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // ลอจิกการกดคลิกเมื่อผู้ใช้นิ้วจิ้มตรงข้อความ Error ลิงก์
            spannable.setSpan(new android.text.style.ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    if (context instanceof MainActivity) {
                        // สะกิดฟังก์ชันหลักใน MainActivity ให้สั่งทำงานวาร์ปหน้าแก้ไขไปหาจุดพังทันที!
                        ((MainActivity) context).jumpToErrorLocation(fileName, lineNumber);
                    }
                }
                @Override
                public void updateDrawState(@NonNull android.text.TextPaint ds) {
                    // ป้องกันไม่ให้แอนดรอยด์บังคับลิงก์เปลี่ยนเป็นสีน้ำเงินของระบบ
                    ds.setUnderlineText(true);
                }
            }, start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    @Override
    public int getItemCount() { return 2; }

    @Override
    public int getItemViewType(int position) { return position; }

    public android.widget.TextView getTvConsole() { return (android.widget.TextView) tvConsoleView; }
    public WebView getWebAiOutput() { return webAiOutput; }
    public android.widget.TextView getTvAiOutput() { return null; }
    public EditText getEtAiInput() { return etAiInput; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView, int viewType) { super(itemView); }
    }
}
