package com.berry_med.bci.utils;

import android.widget.Toast;

import com.berry_med.bci.application.MyApplication;

/*
 * @deprecated Toast
 * @author zl
 * @date 2022/12/2 16:21
 */
public class ToastUtil {
    private static Toast toast;

    /**
     * LENGTH_SHORT
     *
     * @param content
     */
    public static void showToastShort(String content) {
        if (toast == null) {
            toast = Toast.makeText(MyApplication.getContext(), content, Toast.LENGTH_SHORT);
        } else {
            toast.setText(content);
        }
        toast.show();
    }
}
