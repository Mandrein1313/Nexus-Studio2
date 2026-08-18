package com.dev.ministudio;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.Stack;

public class XmlPreviewManager {

    private final Context context;
    private final ResourceResolver resourceResolver;

    public XmlPreviewManager(Context context) {
        this.context = context;
        this.resourceResolver = new ResourceResolver(context);
    }

    public View inflateXml(String xmlContent) {
        if (TextUtils.isEmpty(xmlContent) || xmlContent.trim().isEmpty()) {
            return createErrorView("XML ว่างเปล่า");
        }

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlContent));

            View rootView = null;
            Stack<ViewGroup> parentStack = new Stack<>();

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = getCleanTagName(parser.getName());

                    View view = createViewFromTag(tagName, parser);
                    if (view != null) {
                        applyAttributes(view, parser);

                        String id = parser.getAttributeValue(null, "id");
                        if (id != null && !id.isEmpty()) {
                            view.setTag(id);
                        }

                        if (rootView == null) {
                            rootView = view;
                            makeRootViewFullScreen(rootView);
                        } else if (!parentStack.isEmpty()) {
                            parentStack.peek().addView(view);
                        }

                        if (view instanceof ViewGroup) {
                            parentStack.push((ViewGroup) view);
                        }
                    }
                } 
                else if (eventType == XmlPullParser.END_TAG) {
                    String tagName = getCleanTagName(parser.getName());
                    if (!parentStack.isEmpty() && 
                        tagName.equals(parentStack.peek().getClass().getSimpleName())) {
                        parentStack.pop();
                    }
                }

                eventType = parser.next();
            }

            return rootView != null ? rootView : createErrorView("ไม่พบ Root View");

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorView("เกิดข้อผิดพลาด:\n" + e.getMessage());
        }
    }

    private void makeRootViewFullScreen(View rootView) {
        ViewGroup.LayoutParams params = rootView.getLayoutParams();
        if (params == null) {
            params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
        rootView.setLayoutParams(params);
    }

    private View createErrorView(String message) {
        TextView errorView = new TextView(context);
        errorView.setText("❌ " + message);
        errorView.setTextColor(Color.RED);
        errorView.setBackgroundColor(0x33FF0000);
        errorView.setPadding(32, 48, 32, 48);
        errorView.setTextSize(15);
        errorView.setGravity(Gravity.CENTER);
        return errorView;
    }

    private String getCleanTagName(String tag) {
        if (tag == null) return "";
        if (tag.contains(".")) {
            tag = tag.substring(tag.lastIndexOf(".") + 1);
        }
        return tag;
    }

    private View createViewFromTag(String tagName, XmlPullParser parser) {
        switch (tagName) {
            case "LinearLayout": return new LinearLayout(context);
            case "FrameLayout": return new FrameLayout(context);
            case "RelativeLayout": return new RelativeLayout(context);
            case "ConstraintLayout": return new ConstraintLayout(context);
            case "ScrollView": 
                ScrollView sv = new ScrollView(context);
                sv.setFillViewport(true);
                return sv;
            case "HorizontalScrollView": return new HorizontalScrollView(context);
            case "DrawerLayout":
                DrawerLayout dl = new DrawerLayout(context);
                dl.setFitsSystemWindows(true);
                return dl;
            case "NavigationView":
                try {
                    NavigationView nv = new NavigationView(context);
                    nv.setBackgroundColor(Color.parseColor("#1A1A1A"));
                    return nv;
                } catch (Exception e) {
                    return createErrorView("NavigationView (ต้องการ Material)");
                }
            case "Toolbar":
                Toolbar toolbar = new Toolbar(context);
                toolbar.setBackgroundColor(Color.parseColor("#1E1E1E"));
                return toolbar;
            case "RecyclerView":
                RecyclerView rv = new RecyclerView(context);
                rv.setLayoutManager(new LinearLayoutManager(context));
                return rv;
            case "Spinner":
                Spinner spinner = new Spinner(context);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item,
                        new String[]{"เลือกโปรเจกต์", "ตัวเลือก 1", "ตัวเลือก 2"});
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                return spinner;
            case "CardView": return new CardView(context);
            case "TextView": return new TextView(context);
            case "EditText": return new EditText(context);
            case "Button": return new Button(context);
            case "ImageView": return new ImageView(context);
            case "ImageButton":
                ImageButton ib = new ImageButton(context);
                ib.setBackground(null);
                return ib;
            case "ListView": return new ListView(context);

            case "CodeEditor":
                TextView placeholder = new TextView(context);
                placeholder.setText("[CodeEditor]");
                placeholder.setBackgroundColor(0xFF1E1E1E);
                placeholder.setTextColor(0xFFBB86FC);
                placeholder.setPadding(16, 32, 16, 32);
                return placeholder;

            default:
                return createErrorView("[" + tagName + " ยังไม่รองรับ]");
        }
    }

    private void applyAttributes(View view, XmlPullParser parser) {
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attrName = getCleanAttributeName(parser.getAttributeName(i));
            String attrValue = parser.getAttributeValue(i);
            if (attrValue == null) continue;

            switch (attrName) {
                case "layout_width":
                    params.width = parseLayoutSize(attrValue);
                    break;
                case "layout_height":
                    params.height = parseLayoutSize(attrValue);
                    break;
                case "layout_weight":
                    if (params instanceof LinearLayout.LayoutParams lp) {
                        lp.weight = parseFloat(attrValue, 0f);
                    }
                    break;
                case "orientation":
                    if (view instanceof LinearLayout ll) {
                        ll.setOrientation("vertical".equalsIgnoreCase(attrValue) 
                                ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
                    }
                    break;
                case "text":
                    if (view instanceof TextView tv) {
                        tv.setText(resourceResolver.resolveString(attrValue));
                    }
                    break;
                case "title":
                case "app:title":
                    if (view instanceof Toolbar tb) {
                        tb.setTitle(resourceResolver.resolveString(attrValue));
                    }
                    break;
                case "textColor":
                case "titleTextColor":
                case "app:titleTextColor":
                    if (view instanceof TextView tv) {
                        tv.setTextColor(resourceResolver.resolveColor(attrValue));
                    } else if (view instanceof Toolbar tb) {
                        try {
                            tb.setTitleTextColor(Color.parseColor(attrValue));
                        } catch (Exception ignored) {}
                    }
                    break;
                case "background":
                    try {
                        if (attrValue.startsWith("#")) {
                            view.setBackgroundColor(Color.parseColor(attrValue));
                        } else {
                            view.setBackgroundColor(resourceResolver.resolveColor(attrValue));
                        }
                    } catch (Exception ignored) {}
                    break;
                case "visibility":
                    view.setVisibility(parseVisibility(attrValue));
                    break;
            }
        }

        view.setLayoutParams(params);
    }

    private int parseLayoutSize(String value) {
        if ("match_parent".equalsIgnoreCase(value) || "fill_parent".equalsIgnoreCase(value)) {
            return ViewGroup.LayoutParams.MATCH_PARENT;
        }
        if ("wrap_content".equalsIgnoreCase(value)) {
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        return resourceResolver.resolveDimensionPx(value);
    }

    private int parseVisibility(String value) {
        switch (value.toLowerCase()) {
            case "gone": return View.GONE;
            case "invisible": return View.INVISIBLE;
            default: return View.VISIBLE;
        }
    }

    private float parseFloat(String value, float defaultValue) {
        try { return Float.parseFloat(value); } catch (Exception e) { return defaultValue; }
    }

    private String getCleanAttributeName(String attr) {
        if (attr == null) return "";
        return attr.contains(":") ? attr.substring(attr.lastIndexOf(":") + 1) : attr;
    }

    // ====================== RESOURCE RESOLVER ======================
    private static class ResourceResolver {
        private final Context context;
        private final Resources resources;

        ResourceResolver(Context context) {
            this.context = context;
            this.resources = context.getResources();
        }

        String resolveString(String value) {
            if (value == null) return "";
            if (value.startsWith("@string/")) {
                String name = value.substring(8);
                try {
                    int id = resources.getIdentifier(name, "string", context.getPackageName());
                    return id != 0 ? resources.getString(id) : value;
                } catch (Exception e) { return value; }
            }
            return value;
        }

        int resolveColor(String value) {
            if (value == null) return Color.BLACK;
            if (value.startsWith("#")) {
                try { return Color.parseColor(value); } catch (Exception e) { return Color.BLACK; }
            }
            if (value.startsWith("@color/")) {
                String name = value.substring(7);
                try {
                    int id = resources.getIdentifier(name, "color", context.getPackageName());
                    return id != 0 ? resources.getColor(id) : Color.BLACK;
                } catch (Exception e) { return Color.BLACK; }
            }
            return Color.BLACK;
        }

        float resolveDimension(String value) {
            if (value == null) return 14f;
            if (value.startsWith("@dimen/")) {
                String name = value.substring(7);
                try {
                    int id = resources.getIdentifier(name, "dimen", context.getPackageName());
                    return id != 0 ? resources.getDimension(id) : 14f;
                } catch (Exception e) { return 14f; }
            }
            return parseFloatDimension(value);
        }

        int resolveDimensionPx(String value) {
            return (int) resolveDimension(value);
        }

        private float parseFloatDimension(String value) {
            try {
                if (value.endsWith("dp")) {
                    float dp = Float.parseFloat(value.replace("dp", "").trim());
                    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
                }
                if (value.endsWith("sp")) {
                    float sp = Float.parseFloat(value.replace("sp", "").trim());
                    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.getDisplayMetrics());
                }
                return Float.parseFloat(value);
            } catch (Exception e) {
                return 0f;
            }
        }
    }
}
