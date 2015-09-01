package com.berry_med.waveform;

import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.berry_med.spo2_ble.R;


public class DrawRunnable implements Runnable{

	private Paint                        mPaint;
	private int                          WAVEFORM_PADDING = 120;
	private int                          STROKE_WIDTH = 4;
	private LinkedBlockingQueue<Integer> mQueue;
	private SurfaceHolder                mSurfaceHolder;
	private SurfaceView                  mSurfaceView;
	private WaveFormParams               mWaveParams;
	
	private Context                      mContext;
	
	
	public DrawRunnable(Context context, LinkedBlockingQueue<Integer> queue,
			SurfaceView surfaceView, SurfaceHolder surfaceHolder,
			WaveFormParams waveParams)
	{
		mPaint = new Paint();
		mPaint.setColor(Color.WHITE);
		mPaint.setStrokeWidth(STROKE_WIDTH);
		mPaint.setAntiAlias(true);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStyle(Paint.Style.STROKE);
		
		this.mQueue = queue;
		this.mSurfaceHolder = surfaceHolder;
		this.mSurfaceView = surfaceView;
		this.mWaveParams   = waveParams;
		
		this.mContext = context;
		
	}


	@Override
	public void run() {
		// TODO Auto-generated method stub 

		int    temp = 0;
	    Point  oldPoint  = new Point(WAVEFORM_PADDING+1, WAVEFORM_PADDING);
	    Point  newPoint  = new Point(WAVEFORM_PADDING+1, WAVEFORM_PADDING);
	    Point  prevOldPoint = new Point(WAVEFORM_PADDING+1, WAVEFORM_PADDING);
	    int[]  tempArray = new int[5];
	    int    counter   = 0;
	    Path   mPath     = new Path();
	    float  rangeCoef = (float) (1.0/(mWaveParams.getValueRange()[1] - mWaveParams.getValueRange()[0]));
	    
		while(true)
		{

			    for(counter = 0; counter < mWaveParams.getBufferCounter(); counter++)
			    {
			    	try {
	            		temp = mQueue.take();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			    	tempArray[counter] = temp;
			    	
			    }
			   
			    synchronized (this) {
			    		Canvas mCanvas = mSurfaceHolder.lockCanvas(new Rect(oldPoint.x,WAVEFORM_PADDING,oldPoint.x+STROKE_WIDTH*8,mSurfaceView.getHeight()-WAVEFORM_PADDING+1));
					    if(mCanvas != null)
					    {
					    	mCanvas.drawColor(mContext.getResources().getColor(R.color.app_background_color));
					     
						    mPath.reset();
						    for(counter = 0;counter <mWaveParams.getBufferCounter(); counter++)
						    {
						    	
						    	newPoint.x = oldPoint.x+mWaveParams.getxStep();
						    	if(newPoint.x > mSurfaceView.getWidth() - WAVEFORM_PADDING)
						    	{
						    		newPoint.x = WAVEFORM_PADDING;
						    	}
				            	newPoint.y = mSurfaceView.getHeight()-WAVEFORM_PADDING - (int)(rangeCoef*tempArray[counter]*(mSurfaceView.getHeight() - WAVEFORM_PADDING*2));
				            	           	
				    	        //
				    	        
				    	        mPath.moveTo(oldPoint.x, oldPoint.y);
				    	        //mPath.quadTo((newPoint.x+oldPoint.x)/2, (newPoint.y+oldPoint.y)/2, newPoint.x, newPoint.y);
				    	        mPath.cubicTo(oldPoint.x + (oldPoint.x - prevOldPoint.x)/2, 
				    	        		      oldPoint.y + (oldPoint.y - prevOldPoint.y)/2, 
				    	        		      
				    	        		      oldPoint.x + (newPoint.x - oldPoint.x)/2,
				    	        		      oldPoint.y + (newPoint.y - oldPoint.y)/2,
				    	        		      
				    	        		      newPoint.x, 
				    	        		      newPoint.y);
				    	        
				    	        prevOldPoint.x = oldPoint.x;
				    	        prevOldPoint.y = oldPoint.y;
				            	//oldPoint = newPoint;
				    	        oldPoint.x = newPoint.x;
				    	        oldPoint.y = newPoint.y;
				    	        
						    }
						    
						    mCanvas.drawPath(mPath, mPaint);
			    		    mCanvas.save();
			    		    
			    		    mSurfaceHolder.unlockCanvasAndPost(mCanvas);
					    }
			    		
			    	
				}
			    
    		    
    		    
		}
	}

	
}