package com.dev.ministudio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorWheelView extends View {

    public interface OnColorChangeListener {
        void onColorChanged(int color);
    }

    private Paint wheelPaint;
    private Paint selectorPaint;
    private Paint centerPaint;
    private float centerX, centerY, radius;
    private float selectorAngle = 120f; // เริ่มที่สีเขียว
    private float selectorDistRatio = 0.85f;
    private int currentColor = Color.GREEN;
    private OnColorChangeListener listener;
    private SweepGradient sweepGradient;

    public ColorWheelView(Context context) {
        super(context);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(4f);
        selectorPaint.setColor(Color.WHITE);

        centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerPaint.setColor(currentColor);
    }

    public void setOnColorChangeListener(OnColorChangeListener l) {
        this.listener = l;
    }

    public int getCurrentColor() {
        return currentColor;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) / 2f * 0.92f;

        int[] colors = {
                Color.RED, Color.YELLOW, Color.GREEN,
                Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
        };
        sweepGradient = new SweepGradient(centerX, centerY, colors, null);
        wheelPaint.setShader(sweepGradient);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // วาดวงล้อ
        canvas.drawCircle(centerX, centerY, radius, wheelPaint);

        // วงกลมตรงกลาง (สีที่เลือก)
        float centerRadius = radius * 0.35f;
        centerPaint.setColor(currentColor);
        canvas.drawCircle(centerX, centerY, centerRadius, centerPaint);

        // จุดเลือกบนวงล้อ
        double rad = Math.toRadians(selectorAngle);
        float selX = centerX + (float) (Math.cos(rad) * radius * selectorDistRatio);
        float selY = centerY + (float) (Math.sin(rad) * radius * selectorDistRatio);

        // วงขาวรอบจุดเลือก
        selectorPaint.setStyle(Paint.Style.FILL);
        selectorPaint.setColor(Color.WHITE);
        canvas.drawCircle(selX, selY, 14f, selectorPaint);

        // จุดสีด้านใน
        selectorPaint.setColor(currentColor);
        canvas.drawCircle(selX, selY, 10f, selectorPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float dx = x - centerX;
        float dy = y - centerY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > radius * 0.4f && dist < radius * 1.05f) {
            selectorAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
            if (selectorAngle < 0) selectorAngle += 360f;

            float hue = selectorAngle;
            currentColor = Color.HSVToColor(new float[]{hue, 1f, 1f});

            if (listener != null) {
                listener.onColorChanged(currentColor);
            }
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }
}