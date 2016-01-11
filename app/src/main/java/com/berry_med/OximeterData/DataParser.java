package com.berry_med.OximeterData;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ZXX on 2016/1/8.
 */
public class DataParser {

    //Const
    public String TAG = this.getClass().getSimpleName();

    //Protocol Type
    public static enum  Protocol {BCI, BERRY};
    private int BERRY_LEN = 18;

    //Buffer queue
    private LinkedBlockingQueue<Integer> bufferQueue = new LinkedBlockingQueue<Integer>(256);

    //Parse Runnable
    private ParseRunnable mParseRunnable;
    private boolean       isStop = true;
    private Protocol      mCurProtocol;

    private onPackageReceivedListener mListener;


    /**
     * interface for parameters changed.
     */
    public interface onPackageReceivedListener
    {
        void onPackageReceived(int[] dat);
    }

    //Constructor
    public DataParser(Protocol protocol, onPackageReceivedListener listener)
    {
        this.mCurProtocol = protocol;
        this.mListener    = listener;

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
                switch (mCurProtocol)
                {
                    case BCI:
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
                            mListener.onPackageReceived(packageData);
                        }
                        break;
                    case BERRY:
                        dat = getData();
                        if(dat == 0x55){
                            dat = getData();
                            if(dat == 0xaa){
                                packageData = new int[BERRY_LEN];
                                packageData[0] = 0x55;
                                packageData[1] = 0xaa;
                                for(int i = 2; i < BERRY_LEN; i++)
                                {
                                    packageData[i] = getData();
                                }
                                mListener.onPackageReceived(packageData);
                            }
                        }
                        break;
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

}
