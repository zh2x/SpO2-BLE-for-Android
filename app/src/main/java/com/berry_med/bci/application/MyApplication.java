package com.berry_med.bci.application;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/*
 * description: MyApplication
 * author: zl
 * date: 2024/10/23 9:42
 */
public class MyApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
