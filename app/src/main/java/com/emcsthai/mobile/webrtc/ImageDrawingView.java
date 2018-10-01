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

import com.emcsthai.mobile.webrtc.model.EventDrawing;

// https://stackoverflow.com/questions/16454974/how-to-draw-with-multiple-fingers-in-canvas

public class ImageDrawingView extends AppCompatImageView {

    private static final String TAG = "ImageDrawingView";

    private Bitmap mBitmap;
    private Canvas mCanvas;

    private Paint mBitmapPaint;

    private Paint mLocalCirclePaint;
    private Paint mLocalPaint;

    private Path mLocalCirclePath;
    private Path mLocalPath;

    /*********************************************************************************/

    private Paint mServerPaint;
    private Path mServerPath;

    private OnPaintTouchListener mOnPaintTouchListener = null;

    private int mHeight = 0;
    private int mWidth = 0;

    private float mLocalX, mLocalY;
    private float mServerX, mServerY;

    private static final float TOUCH_TOLERANCE = 4;

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

        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mLocalCirclePath = new Path();

        mLocalCirclePaint = new Paint();
        mLocalCirclePaint.setAntiAlias(true);
        mLocalCirclePaint.setColor(Color.BLUE);
        mLocalCirclePaint.setStyle(Paint.Style.STROKE);
        mLocalCirclePaint.setStrokeJoin(Paint.Join.MITER);
        mLocalCirclePaint.setStrokeWidth(4f);

        mLocalPath = new Path();

        mLocalPaint = new Paint();
        mLocalPaint.setAntiAlias(true);
        mLocalPaint.setDither(true);
        mLocalPaint.setColor(Color.GREEN);
        mLocalPaint.setStyle(Paint.Style.STROKE);
        mLocalPaint.setStrokeJoin(Paint.Join.ROUND);
        mLocalPaint.setStrokeCap(Paint.Cap.ROUND);
        mLocalPaint.setStrokeWidth(10);

        /*********************************************************************************/

        mServerPath = new Path();

        mServerPaint = new Paint();
        mServerPaint.setAntiAlias(true);
        mServerPaint.setDither(true);
        mServerPaint.setColor(Color.RED);
        mServerPaint.setStyle(Paint.Style.STROKE);
        mServerPaint.setStrokeJoin(Paint.Join.ROUND);
        mServerPaint.setStrokeCap(Paint.Cap.ROUND);
        mServerPaint.setStrokeWidth(10);
    }

    public void setOnPaintTouchListener(OnPaintTouchListener onPaintTouchListener) {
        mOnPaintTouchListener = onPaintTouchListener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = h;
        mWidth = w;
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mLocalPath, mLocalPaint);
//        canvas.drawPath(mServerPath, mServerPaint);
        canvas.drawPath(mLocalCirclePath, mLocalCirclePaint);
    }

    public void setDrawingPoint(EventDrawing dwp) {
        float x = dwp.getX() * mWidth;
        float y = dwp.getY() * mHeight;

        mCanvas.drawPoint(x, y, mServerPaint);
        postInvalidate();

//        switch (dwp.getState()) {
//            case 0: // Action Down
//                onServerTouchDown(x, y);
//                break;
//            case 1: // Action Move
//                onServerTouchMove(x, y);
//                postInvalidate();
//                break;
//            case 2: // Action Up
//                onServerTouchUp();
//                postInvalidate();
//                break;
//        }
    }

    private void onServerTouchDown(float x, float y) {
        mServerPath.reset();
        mServerPath.moveTo(x, y);
        mServerX = x;
        mServerY = y;
    }

    private void onServerTouchMove(float x, float y) {
        float dx = Math.abs(x - mServerX);
        float dy = Math.abs(y - mServerY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mServerPath.quadTo(mServerX, mServerY, (x + mServerX) / 2, (y + mServerY) / 2);
            mServerX = x;
            mServerY = y;
        }
    }

    private void onServerTouchUp() {
        mServerPath.lineTo(mServerX, mServerY);
        // commit the path to our offscreen
        mCanvas.drawPath(mServerPath, mServerPaint);
        // kill this so we don't double setDrawingPoint
        mServerPath.reset();
    }

    /*********************************************************************************/

    private void onLocalTouchDown(float x, float y) {
        mLocalPath.reset();
        mLocalPath.moveTo(x, y);
        mLocalX = x;
        mLocalY = y;

        if (mOnPaintTouchListener != null) {
            mOnPaintTouchListener.onPaintTouch(mLocalX / mWidth, mLocalY / mHeight, mLocalX / mWidth, mLocalY / mHeight);
        }
    }

    private void onLocalTouchMove(float x, float y) {

        if (mOnPaintTouchListener != null) {
            mOnPaintTouchListener.onPaintTouch(mLocalX / mWidth, mLocalY / mHeight, x / mWidth, y / mHeight);
        }

        float dx = Math.abs(x - mLocalX);
        float dy = Math.abs(y - mLocalY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mLocalPath.quadTo(mLocalX, mLocalY, (x + mLocalX) / 2, (y + mLocalY) / 2);
            mLocalX = x;
            mLocalY = y;

            mLocalCirclePath.reset();
            mLocalCirclePath.addCircle(mLocalX, mLocalY, 30, Path.Direction.CW);
        }
    }

    private void onLocalTouchUp() {
        mLocalPath.lineTo(mLocalX, mLocalY);
        mLocalCirclePath.reset();
        // commit the path to our offscreen
        mCanvas.drawPath(mLocalPath, mLocalPaint);
        // kill this so we don't double setDrawingPoint
        mLocalPath.reset();

        if (mOnPaintTouchListener != null) {
            mOnPaintTouchListener.onPaintTouch(mLocalX / mWidth, mLocalY / mHeight, mLocalX / mWidth, mLocalY / mHeight);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Method from this class
                onLocalTouchDown(x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                // Method from this class
                onLocalTouchMove(x, y);
                break;

            case MotionEvent.ACTION_UP:
                // Method from this class
                onLocalTouchUp();
                break;
        }

        invalidate();
        return true;
    }

    interface OnPaintTouchListener {
        void onPaintTouch(float startX, float startY, float moveX, float moveY);
    }
}
