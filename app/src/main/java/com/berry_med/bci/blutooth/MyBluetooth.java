package com.berry_med.bci.blutooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.berry_med.bci.dialog.DeviceAdapter;
import com.berry_med.bci.dialog.MyDialog;
import com.berry_med.bci.utils.ToastUtil;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.utils.HexUtil;

import java.util.List;

public class MyBluetooth {
    protected DeviceAdapter adapter;
    protected ParseRunnable mParseRunnable;
    protected WaveForm mWaveForm;
    protected BleDevice bleDevice;

    public MyBluetooth(Activity activity, DeviceAdapter adapter, ParseRunnable parseRunnable, WaveForm mWaveForm) {
        this.adapter = adapter;
        this.mParseRunnable = parseRunnable;
        this.mWaveForm = mWaveForm;

        //Initialization configuration
        BleManager.getInstance().init(activity.getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setMaxConnectCount(1)
                .setOperateTimeout(5000);
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
     * Disconnect
     */
    private void disconnect() {
        if (bleDevice != null) {
            BleManager.getInstance().disconnect(bleDevice);
        }
    }

    /**
     * Disconnect All Device
     */
    public void disconnectAllDevice() {
        disconnect();
        BleManager.getInstance().disconnectAllDevice();
    }

    /**
     * Open
     */
    public void isOpen(MyDialog dialog) {
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
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                enableBluetooth();
            } else {
                disconnectAllDevice();
            }
        }
    }


    //Scan
    public void scan() {
        adapter.clean();
        BleManager.getInstance().scan(new BleScanCallback() {

            @Override
            public void onScanStarted(boolean success) {
                if (adapter != null) {
                    adapter.clean();
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

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                ToastUtil.showToastShort("Connect Success");
                setMtu(bleDevice, 128);
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
                notification(bleDevice);
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException e) {
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
                if (mParseRunnable != null) {
                    mParseRunnable.getOnDataChangeListener().deviceInfo("--", "--");
                    mParseRunnable.getOnDataChangeListener().value(0, 0, 0, 0);
                    mParseRunnable.getOnDataChangeListener().value1(0, 0, 0, 0);
                    mParseRunnable.getOnDataChangeListener().value2(0, 0);
                }
            }
            return false;
        }
    });

    private void notification(BleDevice device) {
        this.bleDevice = device;
        mParseRunnable.init();
        String name = !TextUtils.isEmpty(device.getName()) ? device.getName() : "";
        String mac = !TextUtils.isEmpty(device.getMac()) ? device.getMac() : "";
        mParseRunnable.getOnDataChangeListener().deviceInfo(name.replace("\0", ""), mac);
        BleManager.getInstance().notify(
                device,
                Model.UUID_SERVICE_DATA,
                Model.CHARACTERISTIC_UUID_SEND,
                new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {

                    }

                    @Override
                    public void onNotifyFailure(BleException e) {

                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        if (mParseRunnable != null) mParseRunnable.add(data);
                    }
                }
        );
    }

    /**
     * @param name Rename
     */
    public void bleRename(String name) {
        if (bleDevice != null) {
            byte[] a = {0x00, (byte) name.length()};
            byte[] b = name.getBytes();
            byte[] hex = new byte[a.length + b.length];
            System.arraycopy(a, 0, hex, 0, a.length);
            System.arraycopy(b, 0, hex, a.length, b.length);
            BleManager.getInstance().write(bleDevice,
                    Model.UUID_SERVICE_DATA,
                    Model.CHARACTERISTIC_UUID_RENAME,
                    hex,
                    new BleWriteCallback() {
                        @Override
                        public void onWriteSuccess(int current, int total, byte[] justWrite) {
                            Log.i("TAG", "success");

                        }

                        @Override
                        public void onWriteFailure(BleException e) {
                            Log.i("TAG", "failed");
                        }
                    }
            );
        }
    }

    /**
     * @param hex Hex
     */
    public void writeHex(String hex) {
        if (bleDevice != null) {
            BleManager.getInstance().write(bleDevice,
                    Model.UUID_SERVICE_DATA,
                    Model.CHARACTERISTIC_UUID_RECEIVE,
                    HexUtil.hexStringToBytes(hex),
                    new BleWriteCallback() {
                        @Override
                        public void onWriteSuccess(int current, int total, byte[] justWrite) {
                            Log.i("TAG", "success");
                        }

                        @Override
                        public void onWriteFailure(BleException e) {
                            Log.i("TAG", "failed");
                        }
                    }
            );
        }
    }

    /**
     * model
     * <p>
     * 00:00:00
     */
//    private String toHexString(byte[] data) {
//        if (data != null && data.length >= 3) {
//            int num1 = data[data.length - 3] & 0xFF;
//            int num2 = data[data.length - 2] & 0xFF;
//            int num3 = data[data.length - 1] & 0xFF;
//            return String.format(Locale.ENGLISH, "%02d:%02d:%02d", num1, num2, num3);
//        }
//        return "";
//    }

    /**
     * 设置最大传输单元MTU
     */
    public void setMtu(BleDevice bleDevice, int mtu) {
        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                // 设置MTU失败
            }

            @Override
            public void onMtuChanged(int mtu) {
                // 设置MTU成功，并获得当前设备传输支持的MTU值
            }
        });
    }
}
