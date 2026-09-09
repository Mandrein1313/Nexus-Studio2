package com.dev.ministudio;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.GridView;
import java.util.List;

public class IconAdapter extends BaseAdapter {
    private Context context;
    private List<String> iconPathList; // เปลี่ยนเป็น List<String> เพื่อเก็บ Path ของไฟล์

    // Constructor รับค่า List ของ Path ไฟล์
    public IconAdapter(Context context, List<String> iconPathList) {
        this.context = context;
        this.iconPathList = iconPathList;
    }

    @Override 
    public int getCount() { 
        return iconPathList.size(); 
    }

    @Override 
    public Object getItem(int position) { 
        return iconPathList.get(position); 
    }

    @Override 
    public long getItemId(int position) { 
        return position; 
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(context);
            // ปรับขนาดไอคอนให้พอดี
            imageView.setLayoutParams(new GridView.LayoutParams(150, 150));
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }
        
        // ดึง Path ของไฟล์จากตำแหน่งที่ต้องการ
        String path = iconPathList.get(position);
        
        // โหลดรูปจาก Path โดยตรง
        if (path != null) {
            imageView.setImageDrawable(Drawable.createFromPath(path));
        }
        
        return imageView;
    }
}
