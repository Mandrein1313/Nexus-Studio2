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
 * - รองรับ Device Frame (Phone/Tablet)
 * - รองรับ Widget พื้นฐานรวมถึง Material Components
 */
public class XmlPreviewManager {

    public enum DeviceMode { PHONE, TABLET }

    private final Context context;
    private final float density;
    private DeviceMode deviceMode = DeviceMode.PHONE;

    public XmlPreviewManager(Context context) {
        this.context = context;
        this.density = context.getResources().getDisplayMetrics().density;
    }

    public void setDeviceMode(DeviceMode mode) {
        this.deviceMode = mode != null ? mode : DeviceMode.PHONE;
    }

    public View inflateXml(String xmlContent) {
        if (TextUtils.isEmpty(xmlContent) || xmlContent.trim().isEmpty()) {
            return createErrorView("XML ว่างเปล่า");
        }

        try {
            String cleaned = xmlContent
                    .replaceAll("(?s)<!--.*?-->", "")
                    .trim();

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(cleaned));

            View rootView = null;
            Stack<ViewGroup> parentStack = new Stack<>();
            Stack<Boolean> isGroupStack = new Stack<>();

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = getCleanTagName(parser.getName());

                    if ("include".equals(tagName) || "merge".equals(tagName)
                            || "resources".equals(tagName) || "color".equals(tagName)
                            || "string".equals(tagName) || "dimen".equals(tagName)
                            || "style".equals(tagName) || "item".equals(tagName)) {
                        isGroupStack.push(false);
                        eventType = parser.next();
                        continue;
                    }

                    View view = createViewFromTag(tagName);
                    if (view != null) {
                        applyAttributes(view, parser);

                        if (rootView == null) {
                            rootView = view;
                        } else if (!parentStack.isEmpty()) {
                            try {
                                parentStack.peek().addView(view);
                            } catch (Exception ignored) {
                            }
                        }

                        boolean isGroup = view instanceof ViewGroup
                                && !(view instanceof AdapterViewSafe)
                                && !(view instanceof Toolbar);

                        if (view instanceof ListView || view instanceof RecyclerView
                                || view instanceof Spinner || view instanceof SeekBar) {
                            isGroup = false;
                        }

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

            return wrapInDeviceFrame(rootView);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorView("XML ผิดพลาด:\n" + e.getMessage());
        }
    }

    /** กรอบเครื่อง + ขนาดตาม Phone / Tablet */
    private View wrapInDeviceFrame(View content) {
        FrameLayout outer = new FrameLayout(context);
        outer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        outer.setBackgroundColor(Color.parseColor("#12131A"));
        outer.setPadding(dp(16), dp(16), dp(16), dp(16));

        FrameLayout device = new FrameLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(2), Color.parseColor("#3B4261"));
        device.setBackground(bg);
        device.setElevation(dp(8));
        device.setClipToOutline(true);

        int widthDp;
        int heightDp;
        if (deviceMode == DeviceMode.TABLET) {
            widthDp = 480;
            heightDp = 320;
        } else {
            widthDp = 320;
            heightDp = 560;
        }

        FrameLayout.LayoutParams deviceLp = new FrameLayout.LayoutParams(
                dp(widthDp), dp(heightDp));
        deviceLp.gravity = Gravity.CENTER;
        device.setLayoutParams(deviceLp);

        // Status bar จำลอง
        View statusBar = new View(context);
        statusBar.setBackgroundColor(Color.parseColor("#000000"));
        FrameLayout.LayoutParams sbLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(24));
        sbLp.gravity = Gravity.TOP;
        device.addView(statusBar, sbLp);

        // Frame สำหรับบรรจุ layout
        FrameLayout contentHolder = new FrameLayout(context);
        FrameLayout.LayoutParams chLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        chLp.topMargin = dp(24);
        contentHolder.setLayoutParams(chLp);

        ViewGroup.LayoutParams contentLp = content.getLayoutParams();
        if (contentLp == null) {
            contentLp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            contentLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            contentLp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        content.setLayoutParams(contentLp);
        contentHolder.addView(content);

        device.addView(contentHolder);
        outer.addView(device);
        return outer;
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
            case "DrawerLayout": {
                FrameLayout drawer = new FrameLayout(context);
                drawer.setBackgroundColor(Color.WHITE);
                return drawer;
            }
            case "CoordinatorLayout":
                return new FrameLayout(context);
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
            case "AppBarLayout":
            case "CollapsingToolbarLayout": {
                LinearLayout appBar = new LinearLayout(context);
                appBar.setOrientation(LinearLayout.VERTICAL);
                appBar.setBackgroundColor(Color.parseColor("#00897B"));
                return appBar;
            }
            case "FloatingActionButton":
            case "ExtendedFloatingActionButton": {
                TextView fab = new TextView(context);
                fab.setText("+");
                fab.setTextSize(22);
                fab.setTextColor(Color.WHITE);
                fab.setGravity(Gravity.CENTER);
                GradientDrawable fabBg = new GradientDrawable();
                fabBg.setShape(GradientDrawable.OVAL);
                fabBg.setColor(Color.parseColor("#FF9800"));
                fab.setBackground(fabBg);
                int size = dp(56);
                fab.setLayoutParams(new ViewGroup.LayoutParams(size, size));
                return fab;
            }
            case "NavigationView": {
                LinearLayout nav = new LinearLayout(context);
                nav.setOrientation(LinearLayout.VERTICAL);
                nav.setBackgroundColor(Color.parseColor("#F5F5F5"));
                nav.setPadding(dp(8), dp(16), dp(8), dp(8));
                String[] items = {"Home", "Gallery", "Slideshow"};
                for (String item : items) {
                    TextView tv = new TextView(context);
                    tv.setText("●  " + item);
                    tv.setTextColor(Color.parseColor("#424242"));
                    tv.setTextSize(14);
                    tv.setPadding(dp(16), dp(12), dp(16), dp(12));
                    nav.addView(tv);
                }
                return nav;
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

    private interface AdapterViewSafe {}

    private void applyAttributes(View view, XmlPullParser parser) {
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        int count = parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String name = getCleanAttributeName(parser.getAttributeName(i));
            String value = parser.getAttributeValue(i);
            if (value == null) continue;

            switch (name) {
                case "layout_width":
                    params.width = parseLayoutSize(value);
                    break;
                case "layout_height":
                    params.height = parseLayoutSize(value);
                    break;
                case "layout_margin":
                    int m = parseSizePx(value);
                    params.setMargins(m, m, m, m);
                    break;
                case "layout_marginLeft":
                case "layout_marginStart":
                    params.leftMargin = parseSizePx(value);
                    break;
                case "layout_marginRight":
                case "layout_marginEnd":
                    params.rightMargin = parseSizePx(value);
                    break;
                case "layout_marginTop":
                    params.topMargin = parseSizePx(value);
                    break;
                case "layout_marginBottom":
                    params.bottomMargin = parseSizePx(value);
                    break;
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
                    } catch (Exception ignored) {
                    }
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
                case "src":
                case "srcCompat":
                    if (view instanceof ImageView) {
                        ((ImageView) view).setImageResource(android.R.drawable.ic_menu_gallery);
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
                    } catch (Exception ignored) {
                    }
                    break;
            }
        }

        String weightStr = null;
        for (int i = 0; i < count; i++) {
            if ("layout_weight".equals(getCleanAttributeName(parser.getAttributeName(i)))) {
                weightStr = parser.getAttributeValue(i);
                break;
            }
        }
        if (weightStr != null) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(params);
            try {
                lp.weight = Float.parseFloat(weightStr);
            } catch (Exception ignored) {
            }
            if (lp.weight > 0 && lp.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
                lp.width = 0;
            }
            view.setLayoutParams(lp);
        } else {
            view.setLayoutParams(params);
        }
    }

    private void setGravity(View view, String value) {
        int g = Gravity.NO_GRAVITY;
        String v = value.toLowerCase();
        if (v.contains("center")) g |= Gravity.CENTER;
        if (v.contains("center_vertical")) g |= Gravity.CENTER_VERTICAL;
        if (v.contains("center_horizontal")) g |= Gravity.CENTER_HORIZONTAL;
        if (v.contains("left") || v.contains("start")) g |= Gravity.START;
        if (v.contains("right") || v.contains("end")) g |= Gravity.END;
        if (v.contains("top")) g |= Gravity.TOP;
        if (v.contains("bottom")) g |= Gravity.BOTTOM;

        if (view instanceof LinearLayout) {
            ((LinearLayout) view).setGravity(g);
        } else if (view instanceof TextView) {
            ((TextView) view).setGravity(g);
        }
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
            if (value.endsWith("dp")) {
                float dp = Float.parseFloat(value.replace("dp", "").trim());
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
                if (h.length() == 3) {
                    h = "" + h.charAt(0) + h.charAt(0)
                            + h.charAt(1) + h.charAt(1)
                            + h.charAt(2) + h.charAt(2);
                    return Color.parseColor("#" + h);
                }
                if (h.length() == 6 || h.length() == 8) {
                    return Color.parseColor(value.length() == 7 || value.length() == 9
                            ? value : "#" + h);
                }
            }
            if (value.startsWith("@color/") || value.startsWith("@android:color/")) {
                return Color.parseColor("#6200EE");
            }
        } catch (Exception ignored) {
        }
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
