package com.berry_med.bci.utils.ble;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.concurrent.LinkedBlockingQueue;

/*
 * @deprecated WaveForm
 * @author zl
 * @date 2022/12/2 17:25
 */
public class WaveForm extends SurfaceView {
    private float LINE_WIDTH = 2f;
    private int X_STEP = 2;
    private int BUF_COUNT = 3;

    private int MESSAGE_WAVEFORM_GONE = 1;


    private int mRectIndex;
    private Rect mDrawRect;
    private Rect mClearRect;

    private Paint mLinePaint;
    private Path mLinePath;
    private Path mFillPath;
    private Paint mFillPaint;

    private Paint mClearPaint;

    private int preValue = 0;
    private int curValue;

    private final LinkedBlockingQueue<Integer> ampQueue;

    private boolean isVisibility = false;

    private final Handler mHandler;
    private int index = 1;

    public WaveForm(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLinePaint = new Paint();
        mLinePaint.setARGB(255, 47, 211, 217);
        mLinePaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, LINE_WIDTH, getResources().getDisplayMetrics()));
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setAntiAlias(true);
        mLinePath = new Path();
        mFillPath = new Path();

        mClearPaint = new Paint();
        mClearPaint.setColor(Color.parseColor("#E6E6E6"));
        mClearPaint.setStyle(Paint.Style.FILL);

        LinearGradient linearGradient = new LinearGradient(0, 0, 10, 10, new int[]{
                Color.rgb(255, 189, 22),
                Color.rgb(221, 43, 6),
                Color.rgb(0, 25, 233),
                Color.rgb(0, 232, 210)},
                new float[]{0, .3F, .6F, .9F}, Shader.TileMode.CLAMP);
        mFillPaint = new Paint();
        mFillPaint.setShader(linearGradient);
        mFillPaint.setStyle(Paint.Style.FILL);

        mRectIndex = 0;
        mDrawRect = new Rect(mRectIndex * X_STEP - 150, 0, (mRectIndex + 1) * X_STEP, getHeight());
        mClearRect = new Rect((mRectIndex + 1) * X_STEP - 151, 0, (mRectIndex + 2) * X_STEP, getHeight());
        preValue = 0;
        curValue = 0;

        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        ampQueue = new LinkedBlockingQueue<>();

        mHandler = new Handler(message -> {
            if (message.what == MESSAGE_WAVEFORM_GONE) {
                setVisibility(View.GONE);
                setKeepScreenOn(false);
            }
            return false;
        });
    }

    public WaveForm(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveForm(Context context) {
        this(context, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void addAmplitude(int amp) {
        if (isVisibility) ampQueue.add(amp);
    }

    public void setWaveformVisibility(boolean b) {
        isVisibility = b;
        if (isVisibility) {
            setVisibility(View.VISIBLE);
            new DrawWaveThread().start();
            setKeepScreenOn(true);
        }
    }

    public class DrawWaveThread extends Thread {
        @Override
        public void run() {
            super.run();
            int amp = 0;
            SurfaceHolder holder = getHolder();
            mLinePath.reset();
            if (index == 1) {
                mDrawRect.set(mRectIndex * X_STEP - 150, 0, (mRectIndex + 1) * X_STEP, getHeight());
            } else {
                mDrawRect.set(mRectIndex * X_STEP, 0, (mRectIndex + 1) * X_STEP, getHeight());
            }
            mLinePath.moveTo(mDrawRect.left, getHeight() - preValue);
            while (isVisibility) {
                if (ampQueue.size() >= BUF_COUNT) {
                    mClearRect.set(mRectIndex * X_STEP, 0, (mRectIndex + BUF_COUNT * 4) * X_STEP, getHeight());
                    mFillPath.reset();
                    mFillPath.moveTo(mDrawRect.left, getHeight() - preValue);
                    for (int i = 0; i < BUF_COUNT; i++) {
                        try {
                            amp = ampQueue.take();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mRectIndex++;
                        if (mRectIndex * X_STEP > getWidth()) {
                            mRectIndex = 0;
                            mLinePath.reset();
                            mLinePath.moveTo(0, getHeight() - preValue);
                        }
                        mDrawRect.set(mRectIndex * X_STEP, 0, (mRectIndex + 1) * X_STEP, getHeight());
                        curValue = amp * getHeight() / 200;
                        mLinePath.quadTo(mDrawRect.centerX(), getHeight() - preValue / 2 - curValue / 2, mDrawRect.right, getHeight() - curValue);
                        mFillPath.lineTo(mDrawRect.right, getHeight() - curValue);
                        preValue = curValue;
                    }
                    mFillPath.lineTo(mDrawRect.right, getHeight());
                    mFillPath.lineTo(mDrawRect.left - BUF_COUNT * X_STEP, getHeight());
                    mFillPath.close();
                    Canvas mDirtyCanvas = holder.lockCanvas(mClearRect);
                    if (mDirtyCanvas != null) {
                        try {
                            mDirtyCanvas.drawRect(mClearRect, mClearPaint);
                            mDirtyCanvas.drawPath(mLinePath, mLinePaint);
                            mDirtyCanvas.drawPath(mFillPath, mFillPaint);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            holder.unlockCanvasAndPost(mDirtyCanvas);
                        }
                    }
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            mHandler.obtainMessage(MESSAGE_WAVEFORM_GONE).sendToTarget();
            index++;
        }
    }

    public void clear() {
        try {
            addAmplitude(0);
            index = 1;
            mRectIndex = 0;
            mDrawRect = new Rect(mRectIndex * X_STEP - 150, 0, (mRectIndex + 1) * X_STEP, getHeight());
            mClearRect = new Rect((mRectIndex + 1) * X_STEP - 151, 0, (mRectIndex + 2) * X_STEP, getHeight());
            preValue = 0;
            curValue = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas(null);
            if (canvas != null) {
                canvas.drawColor(Color.TRANSPARENT);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }
}