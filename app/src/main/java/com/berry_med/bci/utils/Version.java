package com.berry_med.bci.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.berry_med.bci.application.MyApplication;

public class Version {
    public static String getVersionName() {
        try {
            PackageManager manager = MyApplication.getContext().getPackageManager();
            PackageInfo info = manager.getPackageInfo(MyApplication.getContext().getPackageName(), 0);
            String version = info.versionName;
            return "v" + version;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
