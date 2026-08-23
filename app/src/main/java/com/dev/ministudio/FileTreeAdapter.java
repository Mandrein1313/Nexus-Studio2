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

        // 🌟 1. ระบบกิ่งไม้แตกแขนง (True Tree Indentation)
        int baseIndentDp = 16; 
        int indentPx = (int) (node.depth * baseIndentDp * context.getResources().getDisplayMetrics().density);
        int paddingRight = (int) (16 * context.getResources().getDisplayMetrics().density);
        holder.itemRoot.setPadding(indentPx, 0, paddingRight, 0);

        holder.imgArrow.setPadding(0, 0, 0, 0);
        holder.imgFileIcon.clearColorFilter();
        
        int defaultSizePx = (int) (24 * context.getResources().getDisplayMetrics().density);
        holder.imgFileIcon.getLayoutParams().width = defaultSizePx;
        holder.imgFileIcon.getLayoutParams().height = defaultSizePx;

        // ตรวจสอบความสัมพันธ์ของเส้นทาง (Path) เพื่อใช้จัดกลุ่มสีให้แม่นยำขึ้น
        String absolutePath = file.getAbsolutePath();
        boolean isInJavaPackage = absolutePath.contains("/src/main/java/") || absolutePath.endsWith("/java");
        // เช็กว่าไฟล์หรือโฟลเดอร์นี้สิงอยู่ในอาณาเขตของ /res/ หรือไม่
        boolean isInResourceFolder = absolutePath.contains("/src/main/res/") || absolutePath.contains("/app/src/main/res/");

        // 🌟 2. ลоจิกคัดแยกประเภทไอคอนและโทนสีขั้นสูง
        if (node.isDirectory) {
            holder.imgArrow.setVisibility(View.VISIBLE);
            if (node.isExpanded) {
                holder.imgArrow.setImageResource(R.drawable.ic_arrow_down);
            } else {
                holder.imgArrow.setImageResource(R.drawable.ic_arrow_right);
            }
            holder.imgArrow.setColorFilter(Color.parseColor("#90A4AE")); 

            holder.imgFileIcon.setImageResource(R.drawable.ic_myicon08);
            
            // สั่งเปลี่ยนสีโฟลเดอร์ตามกลุ่มโครงสร้างโปรเจกต์
            if (isInJavaPackage) {
                holder.imgFileIcon.setColorFilter(Color.parseColor("#2196F3")); // สีฟ้าสำหรับทุกโฟลเดอร์ในสาย Java Code (com, example, mygame)
            } else if ("res".equalsIgnoreCase(name)) {
                holder.imgFileIcon.setColorFilter(Color.parseColor("#4CAF50")); // สีเขียวเข้มสำหรับ Resource หลัก
            } else if (isInResourceFolder) {
                // 🟢 ปรับปรุงใหม่: ชนะทุกเงื่อนไขชื่อไฟล์ ไม่ว่าจะสร้างโฟลเดอร์ชื่ออะไรภายใต้ res (รวมถึงใน xml) จะเป็นสีเขียวพาสเทลทั้งหมดครับน้า
                holder.imgFileIcon.setColorFilter(Color.parseColor("#81C784")); 
            } else if (name.startsWith(".")) {
                holder.imgFileIcon.setColorFilter(Color.parseColor("#78909C")); // สีเทาสำหรับโฟลเดอร์ซ่อนระบบ (.git, .github)
            } else {
                holder.imgFileIcon.setColorFilter(Color.parseColor("#FFA726")); // สีส้มสำหรับโครงสร้างทั่วไป (app, src, main, gradle)
            }

        } else {
            holder.imgArrow.setVisibility(View.GONE);
            String fileNameLower = name.toLowerCase();

            // 🖼️ ตรวจสอบไฟล์รูปภาพเพื่อดึงพรีวิวภาพจริงมาโชว์
            if (fileNameLower.endsWith(".png") || fileNameLower.endsWith(".jpg") || 
                fileNameLower.endsWith(".jpeg") || fileNameLower.endsWith(".webp") || fileNameLower.endsWith(".gif")) {
                
                holder.imgFileIcon.setImageURI(android.net.Uri.fromFile(file));
                holder.imgFileIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                
            } else if ("androidmanifest.xml".equalsIgnoreCase(name)) {
                holder.imgFileIcon.setImageResource(R.drawable.ic_myicon06); 
                holder.imgFileIcon.setColorFilter(Color.parseColor("#E91E63")); // ไอคอนระบบสีชมพูเข้มสำหรับ Manifest
                
            } else if (fileNameLower.endsWith(".java")) {
                holder.imgFileIcon.setImageDrawable(new TextIconDrawable("C", Color.parseColor("#00E5FF"), true));

            } else if (fileNameLower.endsWith(".xml")) {
                holder.imgFileIcon.setImageDrawable(new TextIconDrawable("X³", Color.parseColor("#FF6D00"), false));

            } else if (fileNameLower.endsWith(".gradle")) {
                // 🛠️ เพิ่มไอคอนเฉพาะตัวให้ไฟล์สคริปต์บิวด์ Gradle
                holder.imgFileIcon.setImageDrawable(new TextIconDrawable("GR", Color.parseColor("#607D8B"), false));

            } else if (fileNameLower.endsWith(".properties")) {
                // ⚙️ เพิ่มไอคอนเฉพาะตัวให้ไฟล์ตั้งค่าคอนฟิกโปรเจกต์
                holder.imgFileIcon.setImageDrawable(new TextIconDrawable("PR", Color.parseColor("#455A64"), false));

            } else if (fileNameLower.endsWith(".yml") || fileNameLower.endsWith(".yaml")) {
                // ⚙️ เพิ่มไอคอนสี Indigo/น้ำเงินเข้ม พร้อมตัวย่อ YM สำหรับไฟล์จำพวก YAML คอนฟิกโครงสร้าง
                holder.imgFileIcon.setImageDrawable(new TextIconDrawable("YM", Color.parseColor("#3F51B5"), false));

            } else if ("gradlew".equalsIgnoreCase(name) || "gradlew.bat".equalsIgnoreCase(name)) {
                // 🚀 อัปเกรดใหม่: เพิ่มไอคอนสีเทาอมฟ้าส้ม พร้อมตัวย่อ SH คล้ายไฟล์ Script รันคำสั่งสำหรับ gradlew
                holder.imgFileIcon.setImageDrawable(new TextIconDrawable("SH", Color.parseColor("#546E7A"), false));

            } else if (fileNameLower.endsWith(".jar")) {
                // 📦 เพิ่มไอคอนวงกลมสีน้ำตาลแดงให้ไฟล์ Library สำเร็จรูป (.jar)
                holder.imgFileIcon.setImageDrawable(new TextIconDrawable("J", Color.parseColor("#FF5722"), true));

            } else if (fileNameLower.endsWith(".zip") || fileNameLower.endsWith(".rar") || fileNameLower.endsWith(".7z")) {
                holder.imgFileIcon.setImageDrawable(new TextIconDrawable("ZIP", Color.parseColor("#7C4DFF"), false));

            } else if (name.startsWith(".")) {
                holder.imgFileIcon.setImageResource(R.drawable.ic_myicon06);
                holder.imgFileIcon.setColorFilter(Color.parseColor("#90A4AE")); // สีเทาอ่อนสำหรับไฟล์ซ่อนอย่าง .gitignore
            } else {
                holder.imgFileIcon.setImageResource(R.drawable.ic_myicon06);
            }
        }

        // 🌟 3. ระบบไฮไลต์และสีของชื่อไฟล์เนื้อหา (Selection State)
        if (position == selectedPosition) {
            holder.itemRoot.setBackgroundColor(Color.parseColor("#243144")); 
            holder.tvFileName.setTypeface(null, Typeface.BOLD); 
            holder.tvFileName.setTextColor(Color.parseColor("#00E5FF")); 
        } else {
            holder.itemRoot.setBackgroundColor(Color.TRANSPARENT); 
            holder.tvFileName.setTypeface(null, Typeface.NORMAL); 
            
            if (node.isDirectory) {
                if (isInJavaPackage) {
                    holder.tvFileName.setTextColor(Color.parseColor("#4FC3F7")); // อักษรสีฟ้าพาสเทลสำหรับแพ็กเกจโค้ด
                } else if (isInResourceFolder || "res".equalsIgnoreCase(name)) {
                    // 🟢 ปรับปรุงเพิ่มเติม: ทำให้ตัวอักษรของโฟลเดอร์ย่อยในระบบ res สว่างเด่นขึ้นตามสีไอคอนครับน้า
                    holder.tvFileName.setTextColor(Color.parseColor("#A5D6A7"));
                } else if (name.startsWith(".")) {
                    // 📁 อัปเกรดใหม่: ดักจับโฟลเดอร์ซ่อน (เช่น .github) ให้ตัวอักษรเป็นสีเทาเข้มหลบสายตาอย่างเหมาะสม
                    holder.tvFileName.setTextColor(Color.parseColor("#78909C"));
                } else {
                    holder.tvFileName.setTextColor(Color.parseColor("#ECEFF1")); // อักษรสีขาวสว่างสำหรับโฟลเดอร์อื่น
                }
            } else {
                String fileNameLower = name.toLowerCase();
                if (name.startsWith(".")) {
                    holder.tvFileName.setTextColor(Color.parseColor("#78909C")); // ไฟล์ซ่อนระบบ (เช่น .gitignore) ปรับสีเทาเข้มเพื่อหลบสายตา
                } else if (fileNameLower.endsWith(".java") || fileNameLower.endsWith(".xml") || 
                    fileNameLower.endsWith(".gradle") || fileNameLower.endsWith(".properties") ||
                    fileNameLower.endsWith(".yml") || fileNameLower.endsWith(".yaml") ||
                    "gradlew".equalsIgnoreCase(name)) { // ไฮไลต์ชื่อตัวอักษรของไฟล์รันระบบ gradlew ให้เด่นขึ้นมาด้วย
                    holder.tvFileName.setTextColor(Color.parseColor("#FFFFFF")); // ไฟล์ทำงานหลักและคอนฟิกหลักให้ออกสีขาวชัดเจน
                } else {
                    holder.tvFileName.setTextColor(Color.parseColor("#90A4AE")); // ไฟล์ย่อยอื่นๆ ให้ออกสีเทาจางลงมา
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
            // ปรับขนาดฟอนต์อัตโนมัติตามความยาวตัวอักษรเพื่อไม่ให้ล้นกรอบไอคอน
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
