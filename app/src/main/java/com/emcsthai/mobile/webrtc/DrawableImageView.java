package com.emcsthai.mobile.webrtc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DrawableImageView extends AppCompatImageView {

    private Paint currentPaint;

    private float x = 0;
    private float y = 0;



    public DrawableImageView(Context context) {
        super(context);

        initPaint();
    }

    public DrawableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initPaint();
    }

    public DrawableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initPaint();
    }

    private void initPaint() {
        currentPaint = new Paint();
        currentPaint.setDither(true);
        currentPaint.setColor(Color.BLUE);  // alpha.r.g.b
        currentPaint.setStrokeWidth(2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawPoint(x, y, currentPaint);

        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            x = event.getX();
            y = event.getY();
            invalidate();
            return true;
        }
        return false;
    }
}
