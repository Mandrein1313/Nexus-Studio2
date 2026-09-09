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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PanelPagerAdapter extends RecyclerView.Adapter<PanelPagerAdapter.ViewHolder> {

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

    private final List<LogLine> allLogLines = new ArrayList<>();
    private final int currentFilterType = 0;
    private final boolean isAutoScroll = true;
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
            tvConsoleView = holder.itemView.findViewById(R.id.tvConsole);
            consoleScrollView = holder.itemView.findViewById(R.id.consoleScrollView);

            View btnRun = holder.itemView.findViewById(R.id.btnConsoleRun);
            View btnStop = holder.itemView.findViewById(R.id.btnConsoleStop);
            View btnClear = holder.itemView.findViewById(R.id.btnConsoleClear);
            TextView tvMeta = holder.itemView.findViewById(R.id.tvConsoleMeta);

            if (tvConsoleView != null) {
                ((TextView) tvConsoleView).setMovementMethod(
                        android.text.method.LinkMovementMethod.getInstance());
            }

            // ===== Run =====
            if (btnRun != null) {
                btnRun.setOnClickListener(v -> {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).startCloudBuildPipeline();
                    }
                });
            }

            // ===== Stop =====
            if (btnStop != null) {
                btnStop.setOnClickListener(v -> {
                    if (tvConsoleView != null) {
                        ((TextView) tvConsoleView).append("\n⏹ Stopped by user\n");
                    }
                    Toast.makeText(context, "หยุดแล้ว", Toast.LENGTH_SHORT).show();
                });
            }

            // ===== Clear =====
            if (btnClear != null) {
                btnClear.setOnClickListener(v -> {
                    allLogLines.clear();
                    if (tvConsoleView != null) {
                        ((TextView) tvConsoleView).setText("");
                    }
                });
            }

            // ===== Meta =====
            if (tvMeta != null) {
                String meta = "minSdk 24 · java";
                if (context instanceof MainActivity) {
                    MainActivity act = (MainActivity) context;
                    if (act.getCurrentProject() != null) {
                        meta = act.getCurrentProject().getProjectName() + " · " + meta;
                    }
                }
                tvMeta.setText(meta);
            }

        } else {
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
                    webAiOutput.addJavascriptInterface(
                            mainActivity.new WebAppInterface(context), "AndroidBridge");
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

    private void renderFilteredLogs() {
        if (tvConsoleView == null) return;
        TextView tv = (TextView) tvConsoleView;
        tv.setText("");

        for (LogLine line : allLogLines) {
            if (currentFilterType == 0
                    || (currentFilterType == 1 && line.type == 1)
                    || (currentFilterType == 2 && line.type == 2)) {
                tv.append(makeErrorClickable(line.text, line.color));
            }
        }

        if (isAutoScroll && consoleScrollView != null) {
            consoleScrollView.post(() -> consoleScrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    public void postNewLog(String text, int color) {
        String lower = text.toLowerCase();
        int type = 0;
        if (lower.contains("error:") || lower.contains("failed")) {
            type = 1;
        } else if (lower.contains("warning:") || lower.contains("deprecated")) {
            type = 2;
        }

        allLogLines.add(new LogLine(text, color, type));

        if (currentFilterType == 0 || currentFilterType == type) {
            if (tvConsoleView != null && context instanceof MainActivity) {
                ((MainActivity) context).runOnUiThread(() -> {
                    TextView tv = (TextView) tvConsoleView;
                    tv.append(makeErrorClickable(text, color));
                    if (isAutoScroll && consoleScrollView != null) {
                        consoleScrollView.post(() ->
                                consoleScrollView.fullScroll(View.FOCUS_DOWN));
                    }
                });
            }
        }
    }

    private android.text.SpannableString makeErrorClickable(String text, int defaultColor) {
        android.text.SpannableString spannable = new android.text.SpannableString(text);
        spannable.setSpan(
                new android.text.style.ForegroundColorSpan(defaultColor),
                0, text.length(),
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        Pattern pattern = Pattern.compile("([a-zA-Z0-9_]+\\.java):(\\d+)");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            final String fileName = matcher.group(1);
            final int lineNumber = Integer.parseInt(matcher.group(2));
            int start = matcher.start();
            int end = matcher.end();

            spannable.setSpan(new android.text.style.UnderlineSpan(),
                    start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(
                    new android.text.style.ForegroundColorSpan(
                            android.graphics.Color.parseColor("#4FC3F7")),
                    start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            spannable.setSpan(new android.text.style.ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).jumpToErrorLocation(fileName, lineNumber);
                    }
                }

                @Override
                public void updateDrawState(@NonNull android.text.TextPaint ds) {
                    ds.setUnderlineText(true);
                }
            }, start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public android.widget.TextView getTvConsole() {
        return (android.widget.TextView) tvConsoleView;
    }

    public WebView getWebAiOutput() {
        return webAiOutput;
    }

    public android.widget.TextView getTvAiOutput() {
        return null;
    }

    public EditText getEtAiInput() {
        return etAiInput;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
        }
    }
}
