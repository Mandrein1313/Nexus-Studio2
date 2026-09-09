package com.dev.ministudio;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.Stack;

/**
 * XML Layout Preview สำหรับ Nexus Studio
 * - รองรับ Dynamic Layout Rendering Dynamic Params
 * - แก้ไข bugs เกี่ยวกับ LayoutParams Crash และ Resource Referencing
 */
public class XmlPreviewManager {

    private final Context context;
    private final float density;

    public XmlPreviewManager(Context context) {
        this.context = context;
        this.density = context.getResources().getDisplayMetrics().density;
    }

    public View inflateXml(String xmlContent) {
        if (TextUtils.isEmpty(xmlContent) || xmlContent.trim().isEmpty()) {
            return createErrorView("XML ว่างเปล่า");
        }

        try {
            // ทำความสะอาด comment และ declaration
            String cleaned = xmlContent
                    .replaceAll("(?s)<!--.*?-->", "")
                    .trim();

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false); // ปิดเพื่อความรวดเร็วและยืดหยุ่นต่อ custom namespace
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(cleaned));

            View rootView = null;
            Stack<ViewGroup> parentStack = new Stack<>();
            Stack<Boolean> isGroupStack = new Stack<>();

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = getCleanTagName(parser.getName());

                    // ข้าม tag ที่ไม่ใช่ layout
                    if ("include".equalsIgnoreCase(tagName) || "merge".equalsIgnoreCase(tagName)
                            || "resources".equalsIgnoreCase(tagName) || "color".equalsIgnoreCase(tagName)
                            || "string".equalsIgnoreCase(tagName) || "dimen".equalsIgnoreCase(tagName)
                            || "style".equalsIgnoreCase(tagName) || "item".equalsIgnoreCase(tagName)) {
                        isGroupStack.push(false);
                        eventType = parser.next();
                        continue;
                    }

                    View view = createViewFromTag(tagName);
                    if (view != null) {
                        ViewGroup parent = parentStack.isEmpty() ? null : parentStack.peek();
                        applyAttributes(view, parent, parser);

                        if (rootView == null) {
                            rootView = view;
                        } else if (parent != null) {
                            try {
                                parent.addView(view);
                            } catch (Exception e) {
                                // ป้องกัน crash หาก parent ไม่รองรับการ add child
                            }
                        }

                        boolean isGroup = view instanceof ViewGroup
                                && !(view instanceof Toolbar)
                                && !(view instanceof ListView)
                                && !(view instanceof RecyclerView)
                                && !(view instanceof Spinner);

                        if (isGroup) {
                            parentStack.push((ViewGroup) view);
                        }
                        isGroupStack.push(isGroup);
                    } else {
                        isGroupStack.push(false);
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (!isGroupStack.isEmpty() && Boolean.TRUE.equals(isGroupStack.pop())) {
                        if (!parentStack.isEmpty()) {
                            parentStack.pop();
                        }
                    }
                }
                eventType = parser.next();
            }

            if (rootView == null) {
                return createErrorView("ไม่พบ Root View\n(รองรับเฉพาะ layout XML)");
            }

            return wrapPreview(rootView);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorView("XML ผิดพลาด:\n" + e.getMessage());
        }
    }

    private View wrapPreview(View content) {
        FrameLayout frame = new FrameLayout(context);
        frame.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        frame.setBackgroundColor(Color.parseColor("#2A2B3D"));
        frame.setPadding(dp(12), dp(12), dp(12), dp(12));

        FrameLayout device = new FrameLayout(context);
        GradientDrawable card = new GradientDrawable();
        card.setColor(Color.WHITE);
        card.setCornerRadius(dp(12));
        device.setBackground(card);
        device.setElevation(dp(4));

        FrameLayout.LayoutParams deviceLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        device.setLayoutParams(deviceLp);
        device.setPadding(dp(4), dp(4), dp(4), dp(4));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ViewGroup.LayoutParams contentLp = content.getLayoutParams();
        if (contentLp == null) {
            contentLp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        content.setLayoutParams(contentLp);

        scroll.addView(content);
        device.addView(scroll);
        frame.addView(device);
        return frame;
    }

    private View createErrorView(String message) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(32), dp(24), dp(32));
        box.setBackgroundColor(Color.parseColor("#1A1B26"));

        TextView title = new TextView(context);
        title.setText("Preview Error");
        title.setTextColor(Color.parseColor("#BB9AF7"));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView errorView = new TextView(context);
        errorView.setText(message);
        errorView.setTextColor(Color.parseColor("#FF8A80"));
        errorView.setTextSize(13);
        errorView.setGravity(Gravity.CENTER);
        errorView.setPadding(0, dp(12), 0, 0);

        box.addView(title);
        box.addView(errorView);
        return box;
    }

    private String getCleanTagName(String tag) {
        if (tag == null) return "";
        if (tag.contains(".")) {
            tag = tag.substring(tag.lastIndexOf('.') + 1);
        }
        return tag;
    }

    private View createViewFromTag(String tagName) {
        switch (tagName) {
            case "LinearLayout":
                return new LinearLayout(context);
            case "FrameLayout":
                return new FrameLayout(context);
            case "RelativeLayout":
                return new RelativeLayout(context);
            case "ConstraintLayout":
                return new ConstraintLayout(context);
            case "ScrollView": {
                ScrollView sv = new ScrollView(context);
                sv.setFillViewport(true);
                return sv;
            }
            case "HorizontalScrollView":
                return new HorizontalScrollView(context);
            case "CardView":
            case "MaterialCardView": {
                CardView cv = new CardView(context);
                cv.setCardBackgroundColor(Color.WHITE);
                cv.setRadius(dp(8));
                cv.setCardElevation(dp(2));
                return cv;
            }
            case "Toolbar":
            case "MaterialToolbar": {
                Toolbar tb = new Toolbar(context);
                tb.setBackgroundColor(Color.parseColor("#6200EE"));
                tb.setTitleTextColor(Color.WHITE);
                return tb;
            }
            case "RecyclerView": {
                RecyclerView rv = new RecyclerView(context);
                rv.setLayoutManager(new LinearLayoutManager(context));
                return rv;
            }
            case "ListView":
                return new ListView(context);
            case "Spinner": {
                Spinner sp = new Spinner(context);
                ArrayAdapter<String> ad = new ArrayAdapter<>(context,
                        android.R.layout.simple_spinner_item,
                        new String[]{"Item 1", "Item 2", "Item 3"});
                ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                sp.setAdapter(ad);
                return sp;
            }
            case "TextView":
            case "MaterialTextView":
                return new TextView(context);
            case "Button":
            case "MaterialButton": {
                Button b = new Button(context);
                b.setAllCaps(false);
                return b;
            }
            case "ImageButton":
                return new ImageButton(context);
            case "ImageView": {
                ImageView iv = new ImageView(context);
                iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                iv.setImageResource(android.R.drawable.ic_menu_gallery);
                return iv;
            }
            case "EditText":
            case "TextInputEditText": {
                EditText et = new EditText(context);
                et.setHint("ข้อความ...");
                return et;
            }
            case "CheckBox":
                return new CheckBox(context);
            case "RadioButton":
                return new RadioButton(context);
            case "Switch":
            case "SwitchCompat":
                return new Switch(context);
            case "ProgressBar":
                return new ProgressBar(context);
            case "SeekBar":
                return new SeekBar(context);
            case "View":
            case "Space":
                return new View(context);
            case "WebView": {
                TextView placeholder = new TextView(context);
                placeholder.setText("[WebView]");
                placeholder.setGravity(Gravity.CENTER);
                placeholder.setBackgroundColor(Color.parseColor("#EEEEEE"));
                placeholder.setPadding(dp(16), dp(24), dp(16), dp(24));
                return placeholder;
            }
            default: {
                TextView unknown = new TextView(context);
                unknown.setText("[" + tagName + "]");
                unknown.setTextColor(Color.parseColor("#888888"));
                unknown.setGravity(Gravity.CENTER);
                unknown.setPadding(dp(8), dp(8), dp(8), dp(8));
                unknown.setBackgroundColor(Color.parseColor("#F5F5F5"));
                return unknown;
            }
        }
    }

    private void applyAttributes(View view, ViewGroup parent, XmlPullParser parser) {
        int width = ViewGroup.LayoutParams.WRAP_CONTENT;
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        float weight = 0f;
        int gravity = Gravity.NO_GRAVITY;
        int layoutGravity = Gravity.NO_GRAVITY;

        int marginAll = 0, marginLeft = 0, marginTop = 0, marginRight = 0, marginBottom = 0;
        boolean hasMarginAll = false;

        int count = parser.getAttributeCount();

        // Pass 1: อ่านค่า Layout Attributes พื้นฐาน
        for (int i = 0; i < count; i++) {
            String name = getCleanAttributeName(parser.getAttributeName(i));
            String value = parser.getAttributeValue(i);
            if (value == null) continue;

            switch (name) {
                case "layout_width":
                    width = parseLayoutSize(value);
                    break;
                case "layout_height":
                    height = parseLayoutSize(value);
                    break;
                case "layout_weight":
                    try {
                        weight = Float.parseFloat(value);
                    } catch (Exception ignored) {}
                    break;
                case "layout_gravity":
                    layoutGravity = parseGravityValue(value);
                    break;
                case "layout_margin":
                    marginAll = parseSizePx(value);
                    hasMarginAll = true;
                    break;
                case "layout_marginLeft":
                case "layout_marginStart":
                    marginLeft = parseSizePx(value);
                    break;
                case "layout_marginRight":
                case "layout_marginEnd":
                    marginRight = parseSizePx(value);
                    break;
                case "layout_marginTop":
                    marginTop = parseSizePx(value);
                    break;
                case "layout_marginBottom":
                    marginBottom = parseSizePx(value);
                    break;
            }
        }

        // ปรับแต่ง LayoutParams ให้ตรงตามประเภทของ Parent View
        ViewGroup.LayoutParams params;
        if (parent instanceof LinearLayout) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height, weight);
            if (layoutGravity != Gravity.NO_GRAVITY) lp.gravity = layoutGravity;
            params = lp;
        } else if (parent instanceof FrameLayout) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
            if (layoutGravity != Gravity.NO_GRAVITY) lp.gravity = layoutGravity;
            params = lp;
        } else if (parent instanceof RelativeLayout) {
            params = new RelativeLayout.LayoutParams(width, height);
        } else {
            params = new ViewGroup.MarginLayoutParams(width, height);
        }

        // กำหนด Margin
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) params;
            if (hasMarginAll) {
                mlp.setMargins(marginAll, marginAll, marginAll, marginAll);
            } else {
                mlp.setMargins(marginLeft, marginTop, marginRight, marginBottom);
            }
        }
        view.setLayoutParams(params);

        // Pass 2: อ่านค่า View Specific Attributes
        for (int i = 0; i < count; i++) {
            String name = getCleanAttributeName(parser.getAttributeName(i));
            String value = parser.getAttributeValue(i);
            if (value == null) continue;

            switch (name) {
                case "padding":
                    int p = parseSizePx(value);
                    view.setPadding(p, p, p, p);
                    break;
                case "paddingLeft":
                case "paddingStart":
                    view.setPadding(parseSizePx(value), view.getPaddingTop(),
                            view.getPaddingRight(), view.getPaddingBottom());
                    break;
                case "paddingRight":
                case "paddingEnd":
                    view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                            parseSizePx(value), view.getPaddingBottom());
                    break;
                case "paddingTop":
                    view.setPadding(view.getPaddingLeft(), parseSizePx(value),
                            view.getPaddingRight(), view.getPaddingBottom());
                    break;
                case "paddingBottom":
                    view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                            view.getPaddingRight(), parseSizePx(value));
                    break;
                case "orientation":
                    if (view instanceof LinearLayout) {
                        ((LinearLayout) view).setOrientation(
                                "vertical".equalsIgnoreCase(value)
                                        ? LinearLayout.VERTICAL
                                        : LinearLayout.HORIZONTAL);
                    }
                    break;
                case "gravity":
                    setGravity(view, value);
                    break;
                case "text":
                    if (view instanceof TextView) {
                        ((TextView) view).setText(resolveString(value));
                    }
                    break;
                case "hint":
                    if (view instanceof EditText) {
                        ((EditText) view).setHint(resolveString(value));
                    }
                    break;
                case "textSize":
                    if (view instanceof TextView) {
                        ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX, parseSizePx(value));
                    }
                    break;
                case "textColor":
                case "titleTextColor":
                    if (view instanceof TextView) {
                        ((TextView) view).setTextColor(resolveColor(value));
                    }
                    break;
                case "textStyle":
                    if (view instanceof TextView) {
                        if (value.contains("bold")) {
                            ((TextView) view).setTypeface(null, Typeface.BOLD);
                        } else if (value.contains("italic")) {
                            ((TextView) view).setTypeface(null, Typeface.ITALIC);
                        }
                    }
                    break;
                case "title":
                    if (view instanceof Toolbar) {
                        ((Toolbar) view).setTitle(resolveString(value));
                    }
                    break;
                case "background":
                    try {
                        view.setBackgroundColor(resolveColor(value));
                    } catch (Exception ignored) {}
                    break;
                case "elevation":
                    view.setElevation(parseSizePx(value));
                    break;
                case "cardBackgroundColor":
                    if (view instanceof CardView) {
                        ((CardView) view).setCardBackgroundColor(resolveColor(value));
                    }
                    break;
                case "cardCornerRadius":
                    if (view instanceof CardView) {
                        ((CardView) view).setRadius(parseSizePx(value));
                    }
                    break;
                case "cardElevation":
                    if (view instanceof CardView) {
                        ((CardView) view).setCardElevation(parseSizePx(value));
                    }
                    break;
                case "visibility":
                    view.setVisibility(parseVisibility(value));
                    break;
                case "enabled":
                    view.setEnabled(!"false".equalsIgnoreCase(value));
                    break;
                case "alpha":
                    try {
                        view.setAlpha(Float.parseFloat(value));
                    } catch (Exception ignored) {}
                    break;
            }
        }
    }

    private void setGravity(View view, String value) {
        int g = parseGravityValue(value);
        if (view instanceof LinearLayout) {
            ((LinearLayout) view).setGravity(g);
        } else if (view instanceof TextView) {
            ((TextView) view).setGravity(g);
        }
    }

    private int parseGravityValue(String value) {
        int g = Gravity.NO_GRAVITY;
        String v = value.toLowerCase();
        if (v.contains("center")) g |= Gravity.CENTER;
        if (v.contains("center_vertical")) g |= Gravity.CENTER_VERTICAL;
        if (v.contains("center_horizontal")) g |= Gravity.CENTER_HORIZONTAL;
        if (v.contains("left") || v.contains("start")) g |= Gravity.START;
        if (v.contains("right") || v.contains("end")) g |= Gravity.END;
        if (v.contains("top")) g |= Gravity.TOP;
        if (v.contains("bottom")) g |= Gravity.BOTTOM;
        return g;
    }

    private int parseLayoutSize(String value) {
        if ("match_parent".equalsIgnoreCase(value) || "fill_parent".equalsIgnoreCase(value)) {
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
        if ("wrap_content".equalsIgnoreCase(value)) {
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        return parseSizePx(value);
    }

    private int parseSizePx(String value) {
        try {
            value = value.trim();
            if (value.endsWith("dp") || value.endsWith("dip")) {
                float dp = Float.parseFloat(value.replaceAll("(dp|dip)", "").trim());
                return Math.round(dp * density);
            }
            if (value.endsWith("sp")) {
                float sp = Float.parseFloat(value.replace("sp", "").trim());
                return Math.round(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP, sp,
                        context.getResources().getDisplayMetrics()));
            }
            if (value.endsWith("px")) {
                return (int) Float.parseFloat(value.replace("px", "").trim());
            }
            return (int) Float.parseFloat(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private int parseVisibility(String value) {
        switch (value.toLowerCase()) {
            case "gone":
                return View.GONE;
            case "invisible":
                return View.INVISIBLE;
            default:
                return View.VISIBLE;
        }
    }

    private String resolveString(String value) {
        if (value == null) return "";
        if (value.startsWith("@string/")) {
            return value.substring(8);
        }
        return value;
    }

    private int resolveColor(String value) {
        try {
            if (value.startsWith("#")) {
                String h = value.substring(1);
                // แปลงสั้น เช่น #FFF เป็น #FFFFFF หรือ #8FFF เป็น #88FFFFFF
                if (h.length() == 3) {
                    h = "" + h.charAt(0) + h.charAt(0)
                           + h.charAt(1) + h.charAt(1)
                           + h.charAt(2) + h.charAt(2);
                } else if (h.length() == 4) {
                    h = "" + h.charAt(0) + h.charAt(0)
                           + h.charAt(1) + h.charAt(1)
                           + h.charAt(2) + h.charAt(2)
                           + h.charAt(3) + h.charAt(3);
                }
                return Color.parseColor("#" + h);
            }
            if (value.startsWith("@color/") || value.startsWith("@android:color/")) {
                return Color.parseColor("#6200EE");
            }
        } catch (Exception ignored) {}
        return Color.parseColor("#222222");
    }

    private String getCleanAttributeName(String attr) {
        if (attr == null) return "";
        return attr.contains(":") ? attr.substring(attr.lastIndexOf(':') + 1) : attr;
    }

    private int dp(int v) {
        return Math.round(v * density);
    }
}
