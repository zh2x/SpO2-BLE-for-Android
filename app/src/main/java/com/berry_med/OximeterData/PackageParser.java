package com.berry_med.OximeterData;

import android.bluetooth.BluetoothGattCharacteristic;

import com.berry_med.spo2_ble.BluetoothLeService;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ZXX on 2015/8/31.
 *
 * Add all data from oximeter into a Queue, and then Parsing the data as the protocol manual.
 * If you want more details about the protocol, click the link below.
 *
 *     https://github.com/zh2x/BCI_Protocol_Demo/tree/master/protocol_manual
 */
public class PackageParser
{

    private OxiParams mOxiParams;
    private OnDataChangeListener mOnDataChangeListener;

    public PackageParser(OnDataChangeListener onDataChangeListener)
    {
        this.mOnDataChangeListener = onDataChangeListener;

        mOxiParams = new OxiParams();
    }

    public void parse(int[] packageDat) {

        int spo2, pulseRate, pi;

        spo2      = packageDat[4];
        pulseRate = packageDat[3] | ((packageDat[2] & 0x40) << 1);
        pi        = packageDat[0] & 0x0f;

        if(spo2 != mOxiParams.spo2 || pulseRate != mOxiParams.pulseRate || pi != mOxiParams.pi)
        {
            mOxiParams.update(spo2,pulseRate,pi);
            mOnDataChangeListener.onSpO2ParamsChanged();
        }
        mOnDataChangeListener.onSpO2WaveChanged(packageDat[1]);
    }

    /**
     * interface for parameters changed.
     */
    public interface OnDataChangeListener
    {
        void onSpO2ParamsChanged();
        void onSpO2WaveChanged(int wave);
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

    public OxiParams getOxiParams()
    {
        return mOxiParams;
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
    public static void modifyBluetoothName(BluetoothLeService service,
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
}
