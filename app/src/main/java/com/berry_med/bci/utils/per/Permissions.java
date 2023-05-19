package com.berry_med.bci.utils.per;

import android.app.Activity;

import com.berry_med.bci.device_list.DeviceListDialog;
import com.berry_med.bci.utils.ble.BluetoothManager;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;


import java.util.List;

/*
 * @deprecated Permission
 * @author zl
 * @date 2022/12/2 13:43
 */
public class Permissions {
    public static void all(Activity activity, BluetoothManager ble, DeviceListDialog dialog) {
        XXPermissions.with(activity)
                .permission(Permission.ACCESS_FINE_LOCATION)
                .permission(Permission.ACCESS_COARSE_LOCATION)
                .permission(Permission.Group.BLUETOOTH)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (ble != null) ble.isOpen(dialog);
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        if (never) XXPermissions.startPermissionActivity(activity, permissions);
                    }
                });
    }
}
