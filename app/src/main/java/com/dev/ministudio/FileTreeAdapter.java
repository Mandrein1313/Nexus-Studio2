package com.dev.ministudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.dev.ministudio.model.FileNode;
import java.io.File;
import java.util.List;

public class FileTreeAdapter extends BaseAdapter {

    private Context context;
    private List<FileNode> fileList;
    private int selectedPosition = -1;

    public FileTreeAdapter(Context context, List<FileNode> fileList) {
        this.context = context;
        this.fileList = fileList;
    }

    @Override
    public int getCount() {
        return fileList != null ? fileList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return fileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    static class ViewHolder {
        LinearLayout itemRoot;
        ImageView imgArrow;
        ImageView imgFileIcon;
        TextView tvFileName;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false);

            holder = new ViewHolder();
            holder.itemRoot = convertView.findViewById(R.id.item_root_layout);
            holder.imgArrow = convertView.findViewById(R.id.img_arrow);
            holder.imgFileIcon = convertView.findViewById(R.id.img_file_icon);
            holder.tvFileName = convertView.findViewById(R.id.tv_file_name);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FileNode node = fileList.get(position);
        File file = node.file;
        String name = file.getName();
        holder.tvFileName.setText(name);

        int baseIndentDp = 16;
        int indentPx = (int) (node.depth * baseIndentDp * context.getResources().getDisplayMetrics().density);
        int paddingRight = (int) (16 * context.getResources().getDisplayMetrics().density);
        holder.itemRoot.setPadding(indentPx, 0, paddingRight, 0);

        holder.imgArrow.setPadding(0, 0, 0, 0);
        holder.imgFileIcon.clearColorFilter();

        int defaultSizePx = (int) (24 * context.getResources().getDisplayMetrics().density);
        holder.imgFileIcon.getLayoutParams().width = defaultSizePx;
        holder.imgFileIcon.getLayoutParams().height = defaultSizePx;

        String absolutePath = file.getAbsolutePath();
        boolean isInJavaPackage = absolutePath.contains("/src/main/java/")
                || absolutePath.contains("/src/main/kotlin/")
                || absolutePath.endsWith("/java")
                || absolutePath.endsWith("/kotlin");
        boolean isInResourceFolder = absolutePath.contains("/src/main/res/")
                || absolutePath.contains("/app/src/main/res/");

        if (node.isDirectory) {
            holder.imgArrow.setVisibility(View.VISIBLE);
            if (node.isExpanded) {
                holder.imgArrow.setImageResource(R.drawable.ic_arrow_down);
            } else {
                holder.imgArrow.setImageResource(R.drawable.ic_arrow_right);
            }
            holder.imgArrow.setColorFilter(Color.parseColor("#565F89"));

            holder.imgFileIcon.setImageResource(R.drawable.ic_myicon08);

            // โฟลเดอร์โทน teal แบบเว็บ
            if (name.startsWith(".")) {
                holder.imgFileIcon.setColorFilter(Color.parseColor("#565F89"));
            } else if (isInResourceFolder || "res".equalsIgnoreCase(name)) {
                holder.imgFileIcon.setColorFilter(Color.parseColor("#4DB6AC"));
            } else {
                holder.imgFileIcon.setColorFilter(Color.parseColor("#26A69A"));
            }

        } else {
            holder.imgArrow.setVisibility(View.GONE);
            String fileNameLower = name.toLowerCase();

            if (fileNameLower.endsWith(".png") || fileNameLower.endsWith(".jpg")
                    || fileNameLower.endsWith(".jpeg") || fileNameLower.endsWith(".webp")
                    || fileNameLower.endsWith(".gif")) {

                holder.imgFileIcon.setImageURI(android.net.Uri.fromFile(file));
                holder.imgFileIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);

            } else if ("androidmanifest.xml".equalsIgnoreCase(name)) {
                holder.imgFileIcon.setImageResource(R.drawable.ic_myicon06);
                holder.imgFileIcon.setColorFilter(Color.parseColor("#7AA2F7"));

            } else if (fileNameLower.endsWith(".java") || fileNameLower.endsWith(".kt")) {
                holder.imgFileIcon.setImageDrawable(
                        new TextIconDrawable("J", Color.parseColor("#7AA2F7"), true));

            } else if (fileNameLower.endsWith(".xml")) {
                holder.imgFileIcon.setImageDrawable(
                        new TextIconDrawable("X", Color.parseColor("#26A69A"), true));

            } else if (fileNameLower.endsWith(".gradle") || fileNameLower.endsWith(".kts")) {
                holder.imgFileIcon.setImageDrawable(
                        new TextIconDrawable("G", Color.parseColor("#9ECE6A"), true));

            } else if (fileNameLower.endsWith(".properties")) {
                holder.imgFileIcon.setImageDrawable(
                        new TextIconDrawable("P", Color.parseColor("#BB9AF7"), true));

            } else if (fileNameLower.endsWith(".yml") || fileNameLower.endsWith(".yaml")) {
                holder.imgFileIcon.setImageDrawable(
                        new TextIconDrawable("Y", Color.parseColor("#E0AF68"), true));

            } else if ("gradlew".equalsIgnoreCase(name) || "gradlew.bat".equalsIgnoreCase(name)) {
                holder.imgFileIcon.setImageDrawable(
                        new TextIconDrawable("S", Color.parseColor("#565F89"), true));

            } else if (fileNameLower.endsWith(".md")) {
                holder.imgFileIcon.setImageDrawable(
                        new TextIconDrawable("M", Color.parseColor("#7AA2F7"), true));

            } else if (fileNameLower.endsWith(".jar")) {
                holder.imgFileIcon.setImageDrawable(
                        new TextIconDrawable("J", Color.parseColor("#FF9E64"), true));

            } else if (fileNameLower.endsWith(".zip") || fileNameLower.endsWith(".rar")
                    || fileNameLower.endsWith(".7z")) {
                holder.imgFileIcon.setImageDrawable(
                        new TextIconDrawable("Z", Color.parseColor("#BB9AF7"), true));

            } else if (name.startsWith(".")) {
                holder.imgFileIcon.setImageResource(R.drawable.ic_myicon06);
                holder.imgFileIcon.setColorFilter(Color.parseColor("#565F89"));
            } else {
                holder.imgFileIcon.setImageResource(R.drawable.ic_myicon06);
                holder.imgFileIcon.setColorFilter(Color.parseColor("#A9B1D6"));
            }
        }

        // ชื่อไฟล์ + selection
        if (position == selectedPosition) {
            holder.itemRoot.setBackgroundColor(Color.parseColor("#1F2335"));
            holder.tvFileName.setTypeface(null, Typeface.BOLD);
            holder.tvFileName.setTextColor(Color.parseColor("#7DCFFF"));
        } else {
            holder.itemRoot.setBackgroundColor(Color.TRANSPARENT);
            holder.tvFileName.setTypeface(null, Typeface.NORMAL);

            if (node.isDirectory) {
                if (name.startsWith(".")) {
                    holder.tvFileName.setTextColor(Color.parseColor("#565F89"));
                } else {
                    holder.tvFileName.setTextColor(Color.parseColor("#C0CAF5"));
                }
            } else {
                if (name.startsWith(".")) {
                    holder.tvFileName.setTextColor(Color.parseColor("#565F89"));
                } else {
                    holder.tvFileName.setTextColor(Color.parseColor("#A9B1D6"));
                }
            }
        }

        holder.tvFileName.setTextSize(14);
        return convertView;
    }

    private static class TextIconDrawable extends Drawable {
        private final Paint paint;
        private final String text;
        private final int backgroundColor;
        private final boolean isCircle;

        public TextIconDrawable(String text, int backgroundColor, boolean isCircle) {
            this.text = text;
            this.backgroundColor = backgroundColor;
            this.isCircle = isCircle;
            this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int width = getBounds().width();
            int height = getBounds().height();

            if (width <= 0 || height <= 0) {
                width = height = 48;
            }

            paint.setColor(backgroundColor);
            if (isCircle) {
                canvas.drawCircle(width / 2f, height / 2f, Math.min(width, height) / 2f, paint);
            } else {
                RectF rect = new RectF(0, 0, width, height);
                float cornerRadius = 6f * width / 24f;
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
            }

            paint.setColor(Color.WHITE);
            if (text.length() > 2) {
                paint.setTextSize(height * 0.38f);
            } else if (text.length() == 2) {
                paint.setTextSize(height * 0.44f);
            } else {
                paint.setTextSize(height * 0.50f);
            }

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setFakeBoldText(true);

            float yPos = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f);
            canvas.drawText(text, width / 2f, yPos, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}