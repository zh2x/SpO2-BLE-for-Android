package com.berry_med.bci.utils;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.berry_med.bci.blutooth.MyBluetooth;
import com.berry_med.bci.dialog.MyDialog;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.List;

/*
 * description: Permissions
 * author: zl
 * date: 2024/10/23 10:45
 */
public class Permissions {
    public static void all(Activity activity, MyBluetooth ble, MyDialog dialog) {
        XXPermissions.with(activity)
                .permission(Permission.ACCESS_FINE_LOCATION)
                .permission(Permission.ACCESS_COARSE_LOCATION)
                .permission(Permission.Group.BLUETOOTH)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean all) {
                        if (ble != null) ble.isOpen(dialog);
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean never) {
                        if (never) XXPermissions.startPermissionActivity(activity, permissions);
                    }
                });
    }
}
