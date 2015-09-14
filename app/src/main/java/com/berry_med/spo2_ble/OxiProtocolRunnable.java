package com.berry_med.spo2_ble;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ZXX on 2015/8/31.
 */
public class OxiProtocolRunnable implements Runnable
{
    private static int PACKAGE_LEN               = 5;
    private LinkedBlockingQueue<Integer> oxiData = new LinkedBlockingQueue<Integer>(256);
    private int[] parseBuf                       = new int[5];
    private boolean isStop                       = false;
    private SpO2Params mSpO2Params               = new SpO2Params();
    private OnDataChangeListener mOnDataChangeListener;

    public OxiProtocolRunnable(OnDataChangeListener onDataChangeListener)
    {
        this.mOnDataChangeListener = onDataChangeListener;
    }

    public interface OnDataChangeListener
    {
        void onSpO2ParamsChanged();
        void onSpO2WaveChanged(int wave);
    }

    public SpO2Params getSpO2Params()
    {
        return mSpO2Params;
    }

    public void add(byte[] data)
    {
        for(byte d : data)
        {
            try {
                oxiData.put(toUnsignedInt(d));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        int dat = 0;
        int spo2 = 0;
        int pulseRate = 0;
        int pi = 0;
        while(!isStop)
        {
            dat = getData();
            if((dat & 0x80) > 0) //search package head
            {
                parseBuf[0] = dat;
                for(int i = 1; i < PACKAGE_LEN; i++)
                {
                    dat = getData();
                    if((dat & 0x80) == 0)
                    {
                        parseBuf[i] = dat;
                    }
                    else
                    {
                        continue;
                    }
                }

                // want more info such as status or parameters read the manual paper
                // https://github.com/zh2x/BCI_Protocol_Demo/tree/master/protocol_manual
                spo2      = parseBuf[4];
                pulseRate = parseBuf[3] | ((parseBuf[2] & 0x40) << 1);
                pi        = parseBuf[0] & 0x0f;
                if(spo2 != mSpO2Params.spo2 || pulseRate != mSpO2Params.pulseRate || pi != mSpO2Params.pi)
                {
                    mSpO2Params.update(spo2,pulseRate,pi);
                    mOnDataChangeListener.onSpO2ParamsChanged();
                }
                mOnDataChangeListener.onSpO2WaveChanged(parseBuf[1]);
            }

        }
    }

    public void stop()
    {
        isStop = false;
    }

    private int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    private int getData()
    {
        int dat = 0;
        try {
            dat = oxiData.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return dat;
    }

    public class SpO2Params
    {
        private int spo2;
        private int pulseRate;
        private int pi;             //perfusion index

        private void update(int spo2, int pulseRate, int pi) {
            this.spo2 = spo2;
            this.pulseRate = pulseRate;
            this.pi = pi;
        }

        public int getSpo2() {
            return spo2;
        }

        public int getPulseRate() {
            return pulseRate;
        }

        public int getPi() {
            return pi;
        }
    }
}
