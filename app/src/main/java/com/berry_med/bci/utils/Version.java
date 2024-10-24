package com.berry_med.bci.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.berry_med.bci.application.MyApplication;

/*
 * description: APP Version
 * author: zl
 * date: 2024/10/23 9:51
 */
public class Version {
    public static String getVersionName() {
        try {
            PackageManager manager = MyApplication.getContext().getPackageManager();
            PackageInfo info = manager.getPackageInfo(MyApplication.getContext().getPackageName(), 0);
            String version = info.versionName;
            return "v" + version;
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return "";
        }
    }
}
