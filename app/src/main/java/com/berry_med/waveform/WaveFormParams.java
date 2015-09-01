package com.berry_med.waveform;



public class WaveFormParams 
{
	private int     xStep;
	private int     bufferCounter;
	private int[]   valueRange;
	
	public WaveFormParams(int xStep, int bufferCounter, int[] valueRange)
	{
		this.xStep         = xStep;
		this.bufferCounter = bufferCounter;
		this.valueRange    = valueRange;
	}

	public int getxStep() {
		return xStep;
	}

	public int getBufferCounter() {
		return bufferCounter;
	}

	public int[] getValueRange() {
		return valueRange;
	}

}