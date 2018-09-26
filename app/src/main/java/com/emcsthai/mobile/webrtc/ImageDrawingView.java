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

import com.emcsthai.mobile.webrtc.model.DrawingPoint;

public class ImageDrawingView extends AppCompatImageView {

    private static final String TAG = "ImageDrawingView";

    private Bitmap mBitmap;
    private Canvas mCanvas;

    private Paint mBitmapPaint;
    private Paint circlePaint;
    private Paint mPaint;

    private Path circlePath;
    private Path mPath;

    /*********************************************************************************/

    private Paint mPaint2;
    private Path mPath2;

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

        /*********************************************************************************/

        mPath2 = new Path();

        mPaint2 = new Paint();
        mPaint2.setAntiAlias(true);
        mPaint2.setDither(true);
        mPaint2.setColor(Color.RED);
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setStrokeJoin(Paint.Join.ROUND);
        mPaint2.setStrokeCap(Paint.Cap.ROUND);
        mPaint2.setStrokeWidth(10);
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
        canvas.drawPath(mPath2, mPaint2);
        canvas.drawPath(circlePath, circlePaint);
    }

    private float mX, mY;
    private float mX2, mY2;

    private static final float TOUCH_TOLERANCE = 4;

    public void drawFromServer(DrawingPoint dwp) {

        float x = dwp.getX() * 1080;
        float y = dwp.getY() * 1920;

        switch (dwp.getState()) {
            case 0: // Action Down
                touchStartServer(x, y);
                break;
            case 1: // Action Move
                touchMoveServer(x, y);
                break;
            case 2: // Action Up
                touchUpServer();
                break;
        }
    }

    private void touchStartServer(float x, float y) {
        mPath2.reset();
        mPath2.moveTo(x, y);
        mX2 = x;
        mY2 = y;
    }

    private void touchMoveServer(float x, float y) {
        float dx = Math.abs(x - mX2);
        float dy = Math.abs(y - mY2);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath2.quadTo(mX2, mY2, (x + mX2) / 2, (y + mY2) / 2);
            mX2 = x;
            mY2 = y;
        }
    }

    private void touchUpServer() {
        mPath2.lineTo(mX2, mY2);
        // commit the path to our offscreen
        mCanvas.drawPath(mPath2, mPaint2);
        // kill this so we don't double draw
        mPath2.reset();
    }

    /*********************************************************************************/

    private void touchStartLocal(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMoveLocal(float x, float y) {
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

    private void touchUpLocal() {
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

                touchStartLocal(x, y);

                if (mOnPaintTouchListener != null) {
                    mOnPaintTouchListener.onPaintTouch(mX / 1080, mY / 1920, mX / 1080, mY / 1920);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (mOnPaintTouchListener != null) {
                    mOnPaintTouchListener.onPaintTouch(mX / 1080, mY / 1920, x / 1080, y / 1920);
                }

                touchMoveLocal(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:

                touchUpLocal();

                if (mOnPaintTouchListener != null) {
                    mOnPaintTouchListener.onPaintTouch(mX / 1080, mY / 1920, mX / 1080, mY / 1920);
                }

                invalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }

    interface OnPaintTouchListener {
        void onPaintTouch(float startX, float startY, float moveX, float moveY);
    }
}
