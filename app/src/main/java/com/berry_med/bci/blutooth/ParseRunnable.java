package com.berry_med.bci.blutooth;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * description: ParseRunnable
 * author: zl
 * date: 2024/10/23 10:29
 */
public class ParseRunnable implements Runnable {
    protected LinkedBlockingQueue<Integer> oxiData = new LinkedBlockingQueue<>(256);
    protected int[] parseBuf = new int[20];
    protected OnDataChangeListener mOnDataChangeListener;

    private String model = "";
    protected String currentModel = "";
    private List<Integer> buffArray;

    boolean isStop = false;

    public ParseRunnable(OnDataChangeListener onDataChangeListener) {
        isStop = true;
        this.mOnDataChangeListener = onDataChangeListener;
    }

    public void setStop(boolean stop) {
        isStop = stop;
    }

    public void setModel(String model) {
        buffArray = new ArrayList<>();
        this.model = model;
        this.currentModel = model;
    }

    /**
     * interface for parameters changed.
     */
    public interface OnDataChangeListener {

        void deviceInfo(String name, String mac);

        void value(int spo2, int pr, double pi, int rr, int wave, int packetFreq);

        void model(boolean berry);

        void hardwareVersion(String v);

        void softwareVersion(String v);
    }

    /**
     * Add the data from Bluetooth Device to The Queue.
     *
     * @param data data from Bluetooth Device
     */
    public void add(byte[] data) {
        try {
            if (data != null) {
                String ascii = convertToAscii(data);
                if (ascii.contains("SV")) {
                    mOnDataChangeListener.softwareVersion(getHvOrSv(data, "SV"));
                } else if (ascii.contains("HV")) {
                    mOnDataChangeListener.hardwareVersion(getHvOrSv(data, "HV"));
                } else {
                    if (data.length >= 2) {
                        boolean berry = berryProtocol(data);
                        model = berry ? Model.BERRY : currentModel;
                        mOnDataChangeListener.model(berry);
                        for (byte d : data) oxiData.put(toUnsignedInt(d));
                    }
                }
            }
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    private String convertToAscii(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i : array) {
            sb.append((char) (i & 0xff));
        }
        return sb.toString();
    }

    private String getHvOrSv(byte[] data, String ver) {
        byte[] res = Arrays.copyOfRange(data, 2, data.length - 1);
        Matcher matcher = Pattern.compile(ver + "[^?\\x00]+").matcher(convertToAscii(res));
        if (matcher.find() && matcher.group().contains(".")) {
            return matcher.group();
        }
        return "";
    }

    /**
     * Parsing the protocol as the manual.
     */
    @Override
    public void run() {
        int PACKAGE_LEN = 20;
        while (isStop) {
            int data = getData();
            if (model.equals(Model.BERRY)) {
                parseBuf[0] = data;
                for (int i = 1; i < PACKAGE_LEN; i++) {
                    parseBuf[i] = getData();
                }
                berryParse(parseBuf);
            } else {
                if ((data & 0x80) > 0) {
                    parseBuf[0] = data;
                    for (int i = 1; i < PACKAGE_LEN; i++) {
                        data = getData();
                        if ((data & 0x80) == 0) parseBuf[i] = data;
                    }
                    bciParse(parseBuf);
                }
            }
        }
    }

    // BCI
    private void bciParse(int[] data) {
        int beep = data[0];
        int wave = data[1];
        int spo2 = data[4];
        int pr = data[3] | ((data[2] & 0x40) << 1);
        int rr = -1;
        double pi = 0;
        if (model.equals(Model.BCI_RR)) {
            pi = _calculatePi(beep, data[2]);
            rr = data[6];
        } else {
            pi = data[0] & 0x0f;
        }
        mOnDataChangeListener.value(spo2, pr, pi, rr, wave, -1);
    }

    public void berryParse(int[] array) {
        // Convert int[] to List<Integer> and add to buffArray
        for (int value : array) {
            buffArray.add(value);
        }

        int i = 0; // Current Index
        int validIndex = 0; // Valid Index
        int maxIndex = buffArray.size() - 20; // Data Space

        while (i <= maxIndex) {
            // Failed to match the headers
            if (buffArray.get(i) != 0xFF || buffArray.get(i + 1) != 0xAA) {
                i += 1;
                validIndex = i;
                continue;
            }

            // The header is successfully matched
            int total = 0;
            int checkSum = buffArray.get(i + 19);
            for (int index = 0; index <= 18; index++) {
                total += buffArray.get(i + index);
            }

            // If the verification fails, discard the two data
            if (checkSum != (total % 256)) {
                i += 2;
                validIndex = i;
                continue;
            }

            // Extracting a sublist for data
            List<Integer> data = buffArray.subList(i, i + 19);

            if (data.size() >= 19) {
                int spo2 = data.get(4);
                int pr = data.get(6);
                double pi = data.get(10) / 10.0;
                int wave = data.get(12);
                int packetFreq = data.get(18);

                mOnDataChangeListener.value(spo2, pr, pi, 0, wave, packetFreq);
            }
            i += 20; // Move back one group
            validIndex = i;
        }

        // Update buffArray to retain only valid data
        buffArray = new ArrayList<>(buffArray.subList(validIndex, buffArray.size()));
    }

    private int toUnsignedInt(byte x) {
        return ((int) x) & 0xff;
    }

    private int getData() {
        try {
            return oxiData.take();
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
        return 0;
    }

    //Berry Protocol
    private static boolean berryProtocol(byte[] data) {
        for (int i = 0; i < data.length - 1; i++) {
            if (data[i] == (byte) 0xFF && data[i + 1] == (byte) 0xAA) {
                return true;
            }
        }
        return false;
    }

    private double _calculatePi(int lower, int higher) {
        DecimalFormat df = new DecimalFormat("#.00");
        String value = df.format(((lower & 0x0F) + (higher & 0x0F) * 16) / 10.0);
        return Double.parseDouble(value);
    }

    public OnDataChangeListener getOnDataChangeListener() {
        return mOnDataChangeListener;
    }
}
