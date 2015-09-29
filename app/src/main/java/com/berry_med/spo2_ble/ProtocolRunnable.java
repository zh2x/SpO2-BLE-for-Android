package com.berry_med.spo2_ble;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ZXX on 2015/8/31.
 *
 * Add all data from oximeter into a Queue, and then Parsing the data as the protocol manual.
 * If you want more details about the protocol, click the link below.
 *
 *     https://github.com/zh2x/BCI_Protocol_Demo/tree/master/protocol_manual
 */
public class ProtocolRunnable implements Runnable
{
    private static int PACKAGE_LEN               = 5;
    private LinkedBlockingQueue<Integer> oxiData = new LinkedBlockingQueue<Integer>(256);
    private int[] parseBuf                       = new int[5];
    private boolean isStop                       = false;
    private OxiParams mOxiParams                 = new OxiParams();

    private OnDataChangeListener mOnDataChangeListener;

    public ProtocolRunnable(OnDataChangeListener onDataChangeListener)
    {
        this.mOnDataChangeListener = onDataChangeListener;
    }

    /**
     * interface for parameters changed.
     */
    public interface OnDataChangeListener
    {
        void onSpO2ParamsChanged();
        void onSpO2WaveChanged(int wave);
    }

    public OxiParams getOxiParams()
    {
        return mOxiParams;
    }

    /**
     * Add the data from Bluetooth Device to The Queue.
     *
     * @param data data from Bluetooth Device
     *
     */
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


    /**
     * Parsing the protocol as the manual.
     */
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

                spo2      = parseBuf[4];
                pulseRate = parseBuf[3] | ((parseBuf[2] & 0x40) << 1);
                pi        = parseBuf[0] & 0x0f;

                if(spo2 != mOxiParams.spo2 || pulseRate != mOxiParams.pulseRate || pi != mOxiParams.pi)
                {
                    mOxiParams.update(spo2,pulseRate,pi);
                    mOnDataChangeListener.onSpO2ParamsChanged();
                }
                mOnDataChangeListener.onSpO2WaveChanged(parseBuf[1]);
            }

        }
    }


    /**
     * stop the current thread.
     */
    public void stop()
    {
        isStop = false;
    }

    /**
     * just a small collection of Oximeter parameters.
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

    /**
     *
     * Modify the Bluetooth Name On the Air.
     *
     * @param service service of BluetoothLeService
     *
     * @param ch      characteristic of Modify Bluetooth Name
     *                if this characteristic not found, the function
     *                of modify not support.
     *
     * @param btName  length of btName should not more than 26 bytes.
     *                the bytes more then 26 bytes will be ignored.
     */
    public static void modifyBluetoothName(BluetoothLeService          service,
                                           BluetoothGattCharacteristic ch,
                                           String                      btName)
    {
        if(service == null || ch == null)
            return;

        byte[] b = btName.getBytes();
        byte[] bytes = new byte[b.length+2];
        bytes[0] = 0x00;
        bytes[1] = (byte) b.length;
        System.arraycopy(b,0,bytes,2,b.length);

        service.write(ch,bytes);
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
}
