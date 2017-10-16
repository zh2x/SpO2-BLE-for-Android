package com.berry_med.spo2_ble.data;

import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ZXX on 2016/1/8.
 */
public class DataParser {

    //Const
    public String TAG = this.getClass().getSimpleName();

    //Buffer queue
    private LinkedBlockingQueue<Integer> bufferQueue = new LinkedBlockingQueue<Integer>(256);

    //Parse Runnable
    private ParseRunnable mParseRunnable;
    private boolean       isStop        = true;

    private onPackageReceivedListener mPackageReceivedListener;
    private OxiParams mOxiParams = new OxiParams();


    /**
     * interface for parameters changed.
     */
    public interface onPackageReceivedListener
    {
        void onOxiParamsChanged(OxiParams params);
        void onPlethWaveReceived(int amp);
    }

    //Constructor
    public DataParser(onPackageReceivedListener listener)
    {
        this.mPackageReceivedListener    = listener;

    }

    public void start()
    {
        mParseRunnable = new ParseRunnable();
        new Thread(mParseRunnable).start();
    }

    public void stop()
    {
        isStop = true;
    }

    /**
     * ParseRunnable
     */
    class ParseRunnable implements Runnable {
        int dat;
        int[] packageData;
        @Override
        public void run() {
            while (isStop)
            {
                dat = getData();
                packageData = new int[5];
                if((dat & 0x80) > 0) //search package head
                {
                    packageData[0] = dat;
                    for(int i = 1; i < packageData.length; i++)
                    {
                        dat = getData();
                        if((dat & 0x80) == 0) {
                            packageData[i] = dat;
                        }
                        else {
                            continue;
                        }
                    }


                    int spo2      = packageData[4];
                    int pulseRate = packageData[3] | ((packageData[2] & 0x40) << 1);
                    int pi        = packageData[0] & 0x0f;

                    if(spo2 != mOxiParams.spo2 || pulseRate != mOxiParams.pulseRate || pi != mOxiParams.pi)
                    {
                        mOxiParams.update(spo2,pulseRate,pi);
                        mPackageReceivedListener.onOxiParamsChanged(mOxiParams);
                    }
                    mPackageReceivedListener.onPlethWaveReceived(packageData[1]);
                }
            }
        }
    }

    /**
     * Add the data received from USB or Bluetooth
     * @param dat
     */
    public void add(byte[] dat)
    {
        for(byte b : dat)
        {
            try {
                bufferQueue.put(toUnsignedInt(b));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.e(TAG, "add: "+ Arrays.toString(dat));

        //Log.i(TAG, "add: "+ bufferQueue.size());
    }

    /**
     * Get Dat from Queue
     * @return
     */
    private int getData()
    {
        int dat = 0;
        try {
            dat = bufferQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return dat;
    }


    private int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    /**
     * a small collection of Oximeter parameters.
     * you can add more parameters as the manual.
     *
     * spo2          Pulse Oxygen Saturation
     * pulseRate     pulse rate
     * pi            perfusion index
     *
     */
    public class OxiParams
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
