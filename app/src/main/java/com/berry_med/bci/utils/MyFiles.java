package com.berry_med.bci.utils;

import android.os.Build;
import android.os.Environment;

import com.berry_med.bci.application.MyApplication;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/*
 * @Description File
 * @Author zl
 * @Date 2025/3/30 11:26
 */
public class MyFiles {
    private String filePath = "";
    private BufferedWriter writer;

    // sdCard path
    private String getInnerSDCardPath() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File file = MyApplication.getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (file == null) return "";
            return file.getAbsolutePath() + File.separator + "Spo2BleForAndroid";
        }
        return Environment.getExternalStorageDirectory().getPath();
    }

    // Create Txt
    public void createTxt() {
        File file = new File(getInnerSDCardPath());
        if (!file.exists()) file.mkdirs();
        filePath = file.getPath() + File.separator + "TestRecord.txt";
        try {
            FileOutputStream outputStream = new FileOutputStream(filePath, false);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            writer = new BufferedWriter(outputStreamWriter);
            writer.write("Time,RR,Pr,SpO₂\r\n");
            writer.flush();
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    // Write txt
    public void writeTxt(long time, int rr, int pr, int spo2) {
        try {
            if (writer != null) {
                writer.write(time + "," + rr + "," + pr + "," + spo2 + "\r\n");
                writer.flush();
            }
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (writer != null) writer.close();
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    public String getFilePath() {
        return filePath;
    }
}
