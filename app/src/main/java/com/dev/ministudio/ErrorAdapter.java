package com.dev.ministudio;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;

public class ErrorAdapter extends RecyclerView.Adapter<ErrorAdapter.ErrorViewHolder> {

    private final ArrayList<ParsedError> errorList;
    private final OnErrorClickListener listener;

    public interface OnErrorClickListener {
        void onErrorClick(ParsedError error);
    }

    public ErrorAdapter(ArrayList<ParsedError> errorList, OnErrorClickListener listener) {
        this.errorList = errorList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ErrorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ใช้เลย์เอาต์ไอเทมรายการบั๊ก (พี่สามารถไปสร้างตบแต่งใน XML เพิ่มเติมได้ครับ)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_compile_error, parent, false);
        return new ErrorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ErrorViewHolder holder, int position) {
        ParsedError error = errorList.get(position);
        
        // แยกเฉพาะชื่อไฟล์ออกมาแสดงเพื่อความสะอาดสายตา
        String fileName = new File(error.file).getName();
        holder.tvErrorLocation.setText("📍 " + fileName + " (บรรทัดที่ " + error.line + ", คอลัมน์ " + error.column + ")");
        holder.tvErrorMessage.setText(error.message);

        // ดักจับเหตุการณ์กดคลิกที่รายการบั๊กเพื่อวาร์ป
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onErrorClick(error);
        });
    }

    @Override
    public int getItemCount() {
        return errorList.size();
    }

    static class ErrorViewHolder extends RecyclerView.ViewHolder {
        TextView tvErrorLocation, tvErrorMessage;
        ImageView imgIcon;

        public ErrorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvErrorLocation = itemView.findViewById(R.id.tvErrorLocation);
            tvErrorMessage = itemView.findViewById(R.id.tvErrorMessage);
            imgIcon = itemView.findViewById(R.id.imgErrorIcon);
        }
    }
}
