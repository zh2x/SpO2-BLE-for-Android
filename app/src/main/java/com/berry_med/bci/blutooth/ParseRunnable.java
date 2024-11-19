package com.berry_med.bci.blutooth;

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

    boolean isStop = false;
    private List<Integer> buffArray1;
    private List<Integer> buffArray2;

    public ParseRunnable(OnDataChangeListener onDataChangeListener) {
        isStop = true;
        this.mOnDataChangeListener = onDataChangeListener;
    }

    public void setStop(boolean stop) {
        isStop = stop;
    }

    public void init() {
        buffArray1 = new ArrayList<>();
        buffArray2 = new ArrayList<>();
    }

    /**
     * interface for parameters changed.
     */
    public interface OnDataChangeListener {

        void deviceInfo(String name, String mac);

        void value(int spo2, int pr, double pi, int wave);

        void value1(int spo2, int pr, double pi, int packetFreq);

        void value2(int index, int wave);

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
            if (Model.MODEL.equals("BERRY")) {
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
        pi = data[0] & 0x0f;
        mOnDataChangeListener.value(spo2, pr, pi, wave);
    }

    public void berryParse(int[] array) {
        // Convert int[] to List<Integer> and add to buffArray
        for (int value : array) {
            buffArray1.add(value);
            buffArray2.add(value);
        }
        newBerryPk1();
        newBerryPk2();
    }


    private void newBerryPk1() {
        int i = 0; // Current Index
        int validIndex = 0; // Valid Index
        int maxIndex = buffArray1.size() - 16; // Data Space

        while (i <= maxIndex) {
            // Failed to match the headers
            if (buffArray1.get(i) != 0xFF || buffArray1.get(i + 1) != 0xAA) {
                i += 1;
                validIndex = i;
                continue;
            }

            // The header is successfully matched
            int total = 0;
            int checkSum = buffArray1.get(i + 15);
            for (int index = 0; index <= 14; index++) {
                total += buffArray1.get(i + index);
            }

            // If the verification fails, discard the two data
            if (checkSum != (total % 256)) {
                i += 2;
                validIndex = i;
                continue;
            }

            // Extracting a sublist for data
            List<Integer> data = buffArray1.subList(i, i + 15);

            if (data.size() >= 15) {
                int spo2 = data.get(3);
                int pr = data.get(4);
                double pi = data.get(5) / 10.0;
                int packetFreq = data.get(14);
                mOnDataChangeListener.value1(spo2, pr, pi, packetFreq);
            }
            i += 15; // Move back one group
            validIndex = i;
        }

        // Update buffArray to retain only valid data
        buffArray1 = new ArrayList<>(buffArray1.subList(validIndex, buffArray1.size()));
    }

    private void newBerryPk2() {

        int i = 0; // Current Index
        int validIndex = 0; // Valid Index
        int maxIndex = buffArray2.size() - 6; // Data Space

        while (i <= maxIndex) {
            // Failed to match the headers
            if (buffArray2.get(i) != 0xFF || buffArray2.get(i + 1) != 0xBB) {
                i += 1;
                validIndex = i;
                continue;
            }

            // The header is successfully matched
            int total = 0;
            int checkSum = buffArray2.get(i + 5);
            for (int index = 0; index <= 4; index++) {
                total += buffArray2.get(i + index);
            }

            // If the verification fails, discard the two data
            if (checkSum != (total % 256)) {
                i += 2;
                validIndex = i;
                continue;
            }

            // Extracting a sublist for data
            List<Integer> data = buffArray2.subList(i, i + 5);

            if (data.size() >= 5) {
                mOnDataChangeListener.value2(data.get(2), data.get(4));
            }
            i += 5; // Move back one group
            validIndex = i;
        }

        // Update buffArray to retain only valid data
        buffArray2 = new ArrayList<>(buffArray2.subList(validIndex, buffArray2.size()));
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

    public OnDataChangeListener getOnDataChangeListener() {
        return mOnDataChangeListener;
    }
}
