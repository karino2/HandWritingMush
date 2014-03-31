package com.livejournal.karino2.handwritingmash.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


public class DrawingCanvas extends View {


    private Bitmap mBitmap;
    private Canvas  mCanvas;
    private Path mPath;
    private Paint   mBitmapPaint;
    private Paint       mPaint;
    private Paint mCursorPaint;



    public DrawingCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFF000000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);

        mCursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCursorPaint.setStyle(Paint.Style.STROKE);
        mCursorPaint.setPathEffect(new DashPathEffect(new float[]{5, 2}, 0));

    }

    int mWidth;
    int mHeight;
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        resetCanvas(w, h);
    }

    public void resetCanvas() {
        resetCanvas(mBitmap.getWidth(), mBitmap.getHeight());
        invalidate();
    }
    public void resetCanvas(int w, int h) {
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mBitmap.eraseColor(Color.WHITE);
    }

    int margin = 10;
    int getEffectiveTop() {
        return Math.max(0, (int)(top-margin));
    }
    int getEffectiveBottom() {
       return Math.min(mHeight, (int)(bottom+2*margin));
    }
    int getEffectiveLeft() {
        return Math.max(0, (int)(left-margin));
    }
    int getEffectiveRight() {
        return Math.min(mWidth, (int)(right+2*margin));
    }


    public Bitmap getBitmap()
    {
        return Bitmap.createBitmap(mBitmap, getEffectiveLeft(),  getEffectiveTop(),
                getEffectiveRight()-getEffectiveLeft(),
                getEffectiveBottom()-getEffectiveTop() );
    }


    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFFFFFFF);

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

        canvas.drawPath(mPath, mPaint);
        canvas.drawOval(mBrushCursorRegion, mCursorPaint);
    }



    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;
    private float left = -1;
    private float top = -1;
    private float right = -1;
    private float bottom = -1;

    private static final float CURSOR_SIZE=10;
    RectF mBrushCursorRegion = new RectF(0f, 0f, 0f, 0f);

    private void setBrushCursorPos(float x, float y)
    {
        mBrushCursorRegion = new RectF(x-CURSOR_SIZE/2, y-CURSOR_SIZE/2,
                x+CURSOR_SIZE/2, y+CURSOR_SIZE/2);

    }


    boolean mDownHandled = false;

    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();
        updateEffectiveRegion(x, y);


        setBrushCursorPos(x, y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownHandled = true;
                mPath.reset();
                mPath.moveTo(x, y);
                mX = x;
                mY = y;
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if(!mDownHandled)
                    break;
                float dx = Math.abs(x - mX);
                float dy = Math.abs(y - mY);
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                    mX = x;
                    mY = y;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if(!mDownHandled)
                    break;
                mDownHandled = false;
                mPath.lineTo(mX, mY);
                mCanvas.drawPath(mPath, mPaint);
                mPath.reset();
                invalidate();
                break;
        }
        return true;
    }

    private void updateEffectiveRegion(float x, float y) {
        if(left == -1)
            left = x;
        if(right == -1)
            right = x;
        if(top == -1)
            top = y;
        if(bottom == -1)
            bottom = y;

        left = Math.min(left, x);
        right = Math.max(right, x);
        top = Math.min(top, y);
        bottom = Math.max(bottom, y);
    }


}
