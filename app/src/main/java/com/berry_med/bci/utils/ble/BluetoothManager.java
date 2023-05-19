package com.berry_med.bci.utils.ble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.berry_med.bci.device_list.DeviceAdapter;
import com.berry_med.bci.device_list.DeviceListDialog;
import com.berry_med.bci.utils.ToastUtil;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/*
 * @deprecated Bluetooth
 * @author zl
 * @date 2022/12/2 13:48
 */
public class BluetoothManager {
    public static final String UUID_SERVICE_DATA = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455").toString();
    public static final String UUID_CHARACTER_RECEIVE = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616").toString();
    public static final String UUID_MODIFY_BT_NAME = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3").toString();

    protected Activity activity;
    protected DeviceAdapter adapter;
    protected ParseRunnable mParseRunnable;
    protected WaveForm mWaveForm;

    public BluetoothManager(Activity activity, DeviceAdapter adapter, ParseRunnable parseRunnable, WaveForm mWaveForm) {
        this.activity = activity;
        this.adapter = adapter;
        this.mParseRunnable = parseRunnable;
        this.mWaveForm = mWaveForm;
        init();
    }

    //Initialization configuration
    private void init() {
        //Init
        BleManager.getInstance().init(activity.getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setMaxConnectCount(1)
                .setOperateTimeout(5000);
    }

    /**
     * Support Ble
     */
    private boolean isSupportBle() {
        return BleManager.getInstance().isSupportBle();
    }

    /**
     * Determine whether the current Android system supports BLE
     */
    public boolean isBlueEnable() {
        return BleManager.getInstance().isBlueEnable();
    }

    /**
     * Open Bluetooth
     */
    public void enableBluetooth() {
        BleManager.getInstance().enableBluetooth();
    }

    /**
     * Close Bluetooth
     */
    public void disableBluetooth() {
        BleManager.getInstance().disableBluetooth();
    }

    /**
     * Open
     */
    public void isOpen(DeviceListDialog dialog) {
        try {
            disconnectAllDevice();
            if (!isSupportBle()) {
                ToastUtil.showToastShort("Not Support Ble Device");
                return;
            }
            if (!isBlueEnable()) {
                enableBluetooth();
            } else {
                dialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                enableBluetooth();
            } else {
                disconnectAllDevice();
            }
        }
    }


    //Configuration scan rules
    public void scanRule() {
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(null)
                .setDeviceName(true, "")
                .setDeviceMac(null)
                .setAutoConnect(false)
                .setScanTimeOut(10000)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    //Scan
    public void scan() {
        adapter.clear();
        BleManager.getInstance().scan(new BleScanCallback() {

            @Override
            public void onScanStarted(boolean success) {
                if (adapter != null) {
                    adapter.clear();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                if (adapter != null && bleDevice != null) {
                    adapter.addDevice(bleDevice);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                if (adapter != null) adapter.notifyDataSetChanged();
            }
        });
    }

    //Connect with device
    public void conn(BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {

            }

            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                ToastUtil.showToastShort("Connect Success");
                notification(bleDevice, UUID_SERVICE_DATA, UUID_CHARACTER_RECEIVE);
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                mHandler.sendEmptyMessage(0x01);
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                mHandler.sendEmptyMessageDelayed(0x01, 500);
            }
        });
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == 0x01) {
                if (mWaveForm != null) mWaveForm.clear();
            }
            return false;
        }
    });


    /**
     * Disconnect
     */
    public void disconnect(BleDevice bleDevice) {
        BleManager.getInstance().disconnect(bleDevice);
    }

    /**
     * Disconnect All Device
     */
    public void disconnectAllDevice() {
        BleManager.getInstance().disconnectAllDevice();
    }

    //Notify
    @RequiresApi(api = Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    public void notification(BleDevice bleDevice, String uuid_service, String uuid_characteristic_notify) {
        String model = toHexString(bleDevice.getScanRecord());//device model
        if (DeviceModel.BCI_RR.equals(model)) {
            mParseRunnable.setModel(DeviceModel.BCI_RR);
        } else {
            mParseRunnable.setModel(DeviceModel.BCI_ORDINARY);
        }
        BleManager.getInstance().notify(
                bleDevice,
                uuid_service,
                uuid_characteristic_notify,
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {

                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {

                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        try {
                            if (mParseRunnable != null) mParseRunnable.add(data);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    /**
     * model
     * <p>
     * 00:00:00
     */
    private String toHexString(byte[] scanRecord) {
        if (scanRecord != null && scanRecord.length > 3) {
            String first = padLeft(Integer.toHexString(scanRecord[scanRecord.length - 3]));
            String second = padLeft(Integer.toHexString(scanRecord[scanRecord.length - 2]));
            String third = padLeft(Integer.toHexString(scanRecord[scanRecord.length - 1]));
            return first + ":" + second + ":" + third;
        }
        return "00:00:00";
    }

    private String padLeft(String str) {
        return str.length() >= 2 ? str : "0" + str;
    }

}
