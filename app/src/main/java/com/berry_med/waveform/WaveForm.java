package com.berry_med.waveform;

import java.util.concurrent.LinkedBlockingQueue;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class WaveForm 
{
	private SurfaceView                  mSurfaceView;
	private SurfaceHolder                mSurfaceHolder;
	private LinkedBlockingQueue<Integer> mWaveQueue;
	private Thread                       mWaveThread;
	private DrawRunnable                 mWaveRunnable;
	
	private boolean IsStoped = false;
	
	public WaveForm(Context context, SurfaceView surfaceView, WaveFormParams waveFormParams) {
		this.mSurfaceView = surfaceView;
		
		
		mSurfaceView.setZOrderOnTop(true);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
		
		mWaveQueue   =  new LinkedBlockingQueue<Integer>(1024);
		mWaveRunnable = new DrawRunnable(context,mWaveQueue,mSurfaceView,mSurfaceHolder,waveFormParams);
		mWaveThread = new Thread(mWaveRunnable);
		mWaveThread.start();
		
		mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
			
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				// TODO Auto-generated method stub
				stop();

			}
			
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				// TODO Auto-generated method stub
				start();
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width,
					int height) {
				// TODO Auto-generated method stub
				//�������ĺۼ�
				Canvas mCanvas = holder.lockCanvas();
				mCanvas.drawColor(0x000000);
				holder.unlockCanvasAndPost(mCanvas);
			}
		});
	}
	
	
	protected void stop() {
		// TODO Auto-generated method stub
		mWaveQueue.clear();
		IsStoped = true;
	}


	protected void start() {
		// TODO Auto-generated method stub
		IsStoped = false;
	}


	public void add(Integer dat)
	{
		if(IsStoped) return;
		
		try 
		{
			mWaveQueue.put(dat);
			
			
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
	}


	public void setVisibility(boolean b) {
		// TODO Auto-generated method stub
		if(b)
		{
			 mSurfaceView.setVisibility(View.VISIBLE);
		}
		else {
			 mSurfaceView.setVisibility(View.GONE);
		}
		
	}
}