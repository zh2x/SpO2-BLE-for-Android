package com.berry_med.bci.utils.ble;


import java.text.DecimalFormat;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * @deprecated ParseRunnable
 * @author zl
 * @date 2022/12/2 17:25
 */
public class ParseRunnable implements Runnable {
    private final LinkedBlockingQueue<Integer> oxiData = new LinkedBlockingQueue<>(256);
    private final int[] parseBuf = new int[7];
    private final OnDataChangeListener mOnDataChangeListener;

    String model = "00:00:00";

    boolean isStop = false;

    public ParseRunnable(OnDataChangeListener onDataChangeListener) {
        isStop = true;
        this.mOnDataChangeListener = onDataChangeListener;
    }

    public void setStop(boolean stop) {
        isStop = stop;
    }

    public void setModel(String model) {
        this.model = model;
    }

    /**
     * interface for parameters changed.
     */
    public interface OnDataChangeListener {
        void spo2Val(int spo2);

        void prVal(int pr);

        void waveVal(int wave);

        void piVal(double pi);

        void rrVal(int rr);
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
        int PACKAGE_LEN = 7;
        while (isStop) {
            int dat = getData();
            if ((dat & 0x80) > 0) {
                parseBuf[0] = dat;
                for (int i = 1; i < PACKAGE_LEN; i++) {
                    dat = getData();
                    if ((dat & 0x80) == 0) parseBuf[i] = dat;
                }
                int beep = parseBuf[0];
                int wave = parseBuf[1];
                int spo2 = parseBuf[4];
                int pr = parseBuf[3] | ((parseBuf[2] & 0x40) << 1);
                int battery = -1;
                int rr = -1;
                double pi = 0;
                if (model.equals(DeviceModel.BCI_RR)) {
                    pi = _calculatePi(beep, parseBuf[2]);
                    battery = parseBuf[5];
                    rr = parseBuf[6];
                } else {
                    pi = parseBuf[0] & 0x0f;
                }
                if (spo2 < 35 || spo2 > 100) spo2 = 0;
                if (pr < 25 || pr > 250) pr = 0;

                mOnDataChangeListener.spo2Val(spo2);
                mOnDataChangeListener.prVal(pr);
                mOnDataChangeListener.piVal(pi);
                mOnDataChangeListener.rrVal(rr);
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
        try {
            return oxiData.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double _calculatePi(int lower, int higher) {
        DecimalFormat df = new DecimalFormat("#.00");
        String value = df.format(((lower & 0X0F) + (higher & 0X0F) * 16) / 10.0);
        return Double.parseDouble(value);
    }
}
