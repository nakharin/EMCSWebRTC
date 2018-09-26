package com.emcsthai.mobile.webrtc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ImageDrawingView extends AppCompatImageView {

    public int width;
    public int height;

    private Bitmap mBitmap;

    private Canvas mCanvas;

    private Paint mBitmapPaint;
    private Paint circlePaint;
    private Paint mPaint;

    private Path mPath;
    private Path circlePath;

    private OnPaintTouchListener mOnPaintTouchListener = null;

    public ImageDrawingView(Context context) {
        super(context);
        init();
    }

    public ImageDrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImageDrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        circlePaint = new Paint();
        circlePath = new Path();
        circlePaint.setAntiAlias(true);
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeJoin(Paint.Join.MITER);
        circlePaint.setStrokeWidth(4f);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(10);
    }

    public void setOnPaintTouchListener(OnPaintTouchListener onPaintTouchListener) {
        mOnPaintTouchListener = onPaintTouchListener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
        canvas.drawPath(circlePath, circlePaint);
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;

            circlePath.reset();
            circlePath.addCircle(mX, mY, 30, Path.Direction.CW);
        }
    }

    private void touch_up() {
        mPath.lineTo(mX, mY);
        circlePath.reset();
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        // kill this so we don't double draw
        mPath.reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                if (mOnPaintTouchListener != null) {
                    mOnPaintTouchListener.onPaintTouch(x/ 1280, y / 720);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                if (mOnPaintTouchListener != null) {
                    mOnPaintTouchListener.onPaintTouch(x/ 1280, y / 720);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                if (mOnPaintTouchListener != null) {
                    mOnPaintTouchListener.onPaintTouch(x/ 1280, y / 720);
                }
                invalidate();
                break;
        }
        return true;
    }

    interface OnPaintTouchListener {
        void onPaintTouch(float x, float y);
    }
}
