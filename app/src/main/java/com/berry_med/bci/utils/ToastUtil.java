package com.berry_med.bci.utils;

import android.widget.Toast;

import com.berry_med.bci.application.MyApplication;

/*
 * description: Toast
 * author: zl
 * date: 2024/10/23 9:49
 */
public class ToastUtil {
    private static Toast toast;

    public static void showToastShort(String content) {
        if (toast == null) {
            toast = Toast.makeText(MyApplication.getContext(), content, Toast.LENGTH_SHORT);
        } else {
            toast.setText(content);
        }
        toast.show();
    }
}
