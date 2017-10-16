package com.berry_med.spo2_ble.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.berry_med.spo2_ble.R;


/**
 * Created by ZXX on 2017/7/8.
 */

public class WaveformView extends SurfaceView implements SurfaceHolder.Callback{

    private static final String TAG = ">>>WAVEFORM VIEW<<<";

    private int mHeight;
    private int mWidth;

    private Paint mWavePaint;
    private Paint mBackgroundPaint;


    private SurfaceHolder mSurfaceHolder;
    private Canvas mCanvas;

    private Point mLastPoint;
    private float pointStep;
    private float mLineWidth;

    private int[] mDataBuffer;
    private int   mDataBufferIndex;
    private int   mBufferSize;
    private int   mMaxValue;

    private boolean isSurfaceViewAvailable;


    public WaveformView(Context context){
        this(context,null);
    }


    public WaveformView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        TypedArray arr = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WaveformView, defStyleAttr, 0);

        int waveColor = arr.getColor(R.styleable.WaveformView_waveColor, Color.WHITE);
        mLineWidth = arr.getDimension(R.styleable.WaveformView_lineWidth, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics));
        pointStep = arr.getDimension(R.styleable.WaveformView_pointStep, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.4f, metrics));
        mBufferSize = arr.getInt(R.styleable.WaveformView_bufferSize, 5);
        mMaxValue = arr.getInteger(R.styleable.WaveformView_maxValue, 100);

        mWavePaint = new Paint();
        mWavePaint.setColor(waveColor);
        mWavePaint.setStrokeWidth(mLineWidth);
        mWavePaint.setStyle(Paint.Style.STROKE);
        mWavePaint.setStrokeCap(Paint.Cap.ROUND);
        mWavePaint.setStrokeJoin(Paint.Join.ROUND);

        int backgroundColor = arr.getColor(R.styleable.WaveformView_backgroundColor,
                getResources().getColor(R.color.waveform_background));
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(backgroundColor);

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        mDataBuffer = new int[mBufferSize*2];
        mDataBufferIndex = 0;

        setBackgroundColor(backgroundColor);
        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width  = (MeasureSpec.getSize(widthMeasureSpec));
        if(width > mWidth) mWidth = width;
        int height = (int) (MeasureSpec.getSize(heightMeasureSpec)*0.95);
        if(height > mHeight) mHeight = height;
//
//        mWidth  = (MeasureSpec.getSize(widthMeasureSpec));
//        mHeight = (int) (MeasureSpec.getSize(heightMeasureSpec)*0.95);

       // Log.i(TAG, "onMeasure: " + mWidth +"-" + mHeight );
    }


    public void addAmp(int amp){
        if(!isSurfaceViewAvailable) {
            mDataBufferIndex = 0;
            return;
        }

        if(mLastPoint == null){
            mLastPoint = new Point();
            mLastPoint.x = 0;
            mLastPoint.y = (int) (mHeight - mHeight/(float)mMaxValue * amp);
            return;
        }

        mDataBuffer[mDataBufferIndex] = amp;
        mDataBufferIndex++;
        if(mDataBufferIndex >= mBufferSize){
            mDataBufferIndex = 0;
            int points = (int) ((mWidth - mLastPoint.x) / pointStep);

            points = points > mBufferSize ? mBufferSize : points;
            int xRight = (int) (mLastPoint.x + pointStep*points);
            mCanvas = mSurfaceHolder.lockCanvas(new Rect(mLastPoint.x, 0, (int) (xRight + pointStep*2), (int) (mHeight + mLineWidth)));
            if(mCanvas == null) return;

            mCanvas.drawRect(new Rect(mLastPoint.x, 0, (int) (xRight + pointStep*2), (int) (mHeight+mLineWidth)), mBackgroundPaint);
            for(int i = 0; i < points; i++){
                Point point = new Point();
                point.x = (int) (mLastPoint.x + pointStep);
                point.y = (int) (mHeight - mHeight/(float)mMaxValue * mDataBuffer[i]);

                mCanvas.drawLine(mLastPoint.x, mLastPoint.y,point.x, point.y ,mWavePaint);
                mLastPoint = point;
            }
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            postInvalidate();

            if((int) ((mWidth - mLastPoint.x) / pointStep) < 1){
                mLastPoint.x = 0;
            }
            if(points < mBufferSize){
                //Log.e(TAG, "addAmp: "+points);
                mDataBufferIndex = mBufferSize - points;
                for(int i = 0; i < mDataBufferIndex; i++){
                    mDataBuffer[i] = mDataBuffer[points + i];
                }
                mLastPoint.x = 0;

                //Log.i(TAG, "drawLine mDataBufferIndex:" + mDataBufferIndex + " Points:" + points);
            }
        }
    }

    public void reset(){
        mDataBufferIndex = 0;
        mLastPoint = new Point(0,(int) (mHeight - mHeight/(float)mMaxValue * 128));
        Canvas c = mSurfaceHolder.lockCanvas();
        c.drawRect(new Rect(0,0,mWidth,mHeight), mBackgroundPaint);
        mSurfaceHolder.unlockCanvasAndPost(c);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if(mLastPoint != null){
            mLastPoint = null;
        }
        //Log.e(TAG, "surfaceCreated: ");
        isSurfaceViewAvailable = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Canvas c = holder.lockCanvas();
        c.drawRect(new Rect(0,0,mWidth,mHeight), mBackgroundPaint);
        holder.unlockCanvasAndPost(c);
       // Log.e(TAG, "surfaceChanged: ");

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isSurfaceViewAvailable = false;
    }
}
