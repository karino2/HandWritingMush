package com.livejournal.karino2.handwritingmash.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;


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
        mPaint.setStrokeWidth(3);

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
        // canvas.drawOval(mBrushCursorRegion, mCursorPaint);
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

    class UndoList {
        class UndoCommand {
            Bitmap undoBmp;
            Bitmap redoBmp;
            int x, y;
            RectF effectiveUndo;
            RectF effectiveRedo;
            UndoCommand(int x, int y, Bitmap undo, Bitmap redo, RectF effectiveUndo, RectF effectiveRedo) {
                undoBmp = undo;
                redoBmp = redo;
                this.effectiveUndo = effectiveUndo;
                this.effectiveRedo = effectiveRedo;
                this.x = x;
                this.y = y;
            }
            void undo(Canvas target, RectF outEffective) {
                target.drawBitmap(undoBmp, x, y, null);
                outEffective.set(effectiveUndo);
            }
            void redo(Canvas target, RectF outEffective) {
                target.drawBitmap(redoBmp, x, y, null);
                outEffective.set(effectiveRedo);
            }
            int getBitmapSize(Bitmap bmp) {
                return 4*bmp.getWidth()*bmp.getHeight();
            }
            int getSize() {
                return getBitmapSize(undoBmp)+getBitmapSize(redoBmp);
            }

        }

        ArrayList<UndoCommand> commandList = new ArrayList<UndoCommand>();
        int currentPos = -1;


        public void pushUndoCommand(int x, int y, Bitmap undo, Bitmap redo, RectF effectiveUndo, RectF effectiveRedo) {
            discardLaterCommand();
            commandList.add(new UndoCommand(x, y, undo, redo, effectiveUndo, effectiveRedo));
            currentPos++;
            discardUntilSizeFit();
        }

        void discardLaterCommand() {
            for(int i = commandList.size()-1; i > currentPos; i--) {
                commandList.remove(i);
            }
        }

        final int COMMAND_MAX_SIZE = 1024*1024; // 1M

        private void discardUntilSizeFit() {
            // currentPos ==0, then do not remove even though it bigger than threshold (I guess never happen, though).
            while(currentPos > 0 && getCommandsSize() > COMMAND_MAX_SIZE) {
                commandList.remove(0);
                currentPos--;
            }
        }

        int getCommandsSize() {
            int res = 0;
            for(UndoCommand cmd : commandList) {
                res += cmd.getSize();
            }
            return res;
        }
        int getCurrentPos() {
            return currentPos;
        }
        public boolean canUndo() {
            return getCurrentPos() >= 0;
        }
        public boolean canRedo() {
            return getCurrentPos() < commandList.size()-1;
        }

        public void redo(Canvas target, RectF outEffective) {
            if (!canRedo())
                return;
            currentPos++;
            commandList.get(getCurrentPos()).redo(target, outEffective);
        }

        public void undo(Canvas target, RectF outEffective) {
            if(!canUndo())
                return;
            commandList.get(getCurrentPos()).undo(target, outEffective);
            currentPos--;
        }


    }

    UndoList undoList = new UndoList();

    public void redo() {
        undoList.redo(mCanvas, tempRegion);
        assignEffective(tempRegion);
        invalidate();
    }

    private void assignEffective(RectF rect) {
        if(rect.left < 0)
            return;
        left = rect.left;
        bottom = rect.bottom;
        right = rect.right;
        top = rect.top;
    }

    public void undo() {
        undoList.undo(mCanvas, tempRegion);
        assignEffective(tempRegion);
        invalidate();
    }

    void fitInsideScreen(Rect region) {
        region.intersect(0, 0, mWidth, mHeight);
    }



    Rect tempRect = new Rect();
    private Rect pathBound() {
        mPath.computeBounds(tempRegion, false);
        tempRegion.roundOut(tempRect);
        widen(tempRect, 5);
        fitInsideScreen(tempRect);
        return tempRect;
    }

    private void widen(Rect tmpInval, int width) {
        int newLeft = Math.max(0, tmpInval.left- width);
        int newTop = Math.max(0, tmpInval.top - width);
        int newRight = Math.min(mWidth, tmpInval.right+ width);
        int newBottom = Math.min(mHeight, tmpInval.bottom+ width);
        tmpInval.set(newLeft, newTop, newRight, newBottom);
    }


    boolean mDownHandled = false;
    RectF tempRegion = new RectF();

    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();


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

                RectF undoRegion = new RectF(left, top, right, bottom);

                mPath.lineTo(mX, mY);
                mPath.computeBounds(tempRegion, true);
                updateEffectiveRegion(tempRegion);

                RectF redoRegion = new RectF(left, top, right, bottom);

                Rect region = pathBound();
                Bitmap undo = Bitmap.createBitmap(mBitmap, region.left, region.top, region.width(), region.height() );
                mCanvas.drawPath(mPath, mPaint);
                Bitmap redo = Bitmap.createBitmap(mBitmap, region.left, region.top, region.width(), region.height());

                undoList.pushUndoCommand(region.left, region.top, undo, redo, undoRegion, redoRegion);

                mPath.reset();
                invalidate();
                break;
        }
        return true;
    }

    private void updateEffectiveRegion(RectF region) {
        if(region.width() == 0)
            return;
        updateEffectiveRegion(region.left, region.top);
        updateEffectiveRegion(region.right, region.bottom);
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
