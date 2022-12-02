package com.berry_med.bci.application;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/*
 * @deprecated Application
 * @author zl
 * @date 2022/12/2 17:25
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
