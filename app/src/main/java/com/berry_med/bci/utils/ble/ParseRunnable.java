package com.berry_med.bci.utils.ble;

import java.util.concurrent.LinkedBlockingQueue;
/*
 * @deprecated ParseRunnable
 * @author zl
 * @date 2022/12/2 17:25
 */
public class ParseRunnable implements Runnable {
    private final LinkedBlockingQueue<Integer> oxiData = new LinkedBlockingQueue<>(256);
    private final int[] parseBuf = new int[5];
    private final OnDataChangeListener mOnDataChangeListener;

    public ParseRunnable(OnDataChangeListener onDataChangeListener) {
        this.mOnDataChangeListener = onDataChangeListener;
    }

    /**
     * interface for parameters changed.
     */
    public interface OnDataChangeListener {
        void spo2Val(int spo2);

        void prVal(int pr);

        void waveVal(int wave);
    }

    /**
     * Add the data from Bluetooth Device to The Queue.
     *
     * @param data data from Bluetooth Device
     */
    public void add(byte[] data) throws Exception {
        for (byte d : data) {
            oxiData.put(toUnsignedInt(d));
        }
    }


    /**
     * Parsing the protocol as the manual.
     */
    @Override
    public void run() {
        int dat = 0;
        int spo2 = 0;
        int pr = 0;
        int pi = 0;
        boolean isStop = false;
        while (!isStop) {
            dat = getData();
            if ((dat & 0x80) > 0) {
                parseBuf[0] = dat;
                int PACKAGE_LEN = 5;
                for (int i = 1; i < PACKAGE_LEN; i++) {
                    dat = getData();
                    if ((dat & 0x80) == 0) parseBuf[i] = dat;
                }
                spo2 = parseBuf[4];
                pr = parseBuf[3] | ((parseBuf[2] & 0x40) << 1);
                pi = parseBuf[0] & 0x0f;

                if (spo2 < 35 || spo2 > 100) spo2 = 0;
                if (pr < 25 || pr > 250) pr = 0;

                mOnDataChangeListener.spo2Val(spo2);
                mOnDataChangeListener.prVal(pr);
                mOnDataChangeListener.waveVal(parseBuf[1]);
            }
        }
    }


    //PI
    public static double getFloatPi(int value) {
        switch (value & 15) {
            case 0:
                return 0.1;
            case 1:
                return 0.2;
            case 2:
                return 0.4;
            case 3:
                return 0.7;
            case 4:
                return 1.4;
            case 5:
                return 2.7;
            case 6:
                return 5.3;
            case 7:
                return 10.3;
            case 8:
                return 20.0;
            default:
                return 0;
        }
    }

    //SI
    public static int getIntPi(double pi) {
        if (pi >= 0.1 && pi < 0.2) {
            return 0;
        } else if (pi >= 0.2 && pi < 0.4) {
            return 1;
        } else if (pi >= 0.4 && pi < 0.7) {
            return 2;
        } else if (pi >= 0.7 && pi < 1.4) {
            return 3;
        } else if (pi >= 1.4 && pi < 2.7) {
            return 4;
        } else if (pi >= 2.7 && pi < 5.3) {
            return 5;
        } else if (pi >= 5.3 && pi < 10.3) {
            return 6;
        } else if (pi >= 10.3 && pi < 20.0) {
            return 7;
        } else if (pi >= 20.0) {
            return 8;
        } else {
            return 0;
        }
    }


    private int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    private int getData() {
        int dat = 0;
        try {
            dat = oxiData.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return dat;
    }
}
