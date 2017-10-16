package com.berry_med.spo2_ble.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.berry_med.spo2_ble.data.Const;

import java.util.List;

/**
 * Created by ZXX on 2017/4/28.
 */

public class BleController {
    //TAG
    private final String TAG = this.getClass().getName();


    private static BleController  mBleController = null;
    private BluetoothAdapter      mBtAdapter     = null;

    public  StateListener         mStateListener;
    private BluetoothLeService    mBluetoothLeService;
    private BluetoothGattCharacteristic chReceiveData;
    private   BluetoothGattCharacteristic chModifyName;

    private boolean mIsConnected = false;



    private BleController(StateListener stateListener){
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mStateListener = stateListener;
        mHandler = new Handler();
    }

    /**
     * Get a Controller
     * @return
     */
    public static BleController getDefaultBleController(StateListener stateListener) {
        if (mBleController == null) {
            mBleController = new BleController(stateListener);
        }
        return mBleController;
    }





    /**
     * enable bluetooth adapter
     */
    public void enableBtAdapter(){
        if (!mBtAdapter.isEnabled()) {
            mBtAdapter.enable();
        }
    }

    public boolean isConnected(){
        return mIsConnected;
    }

    /**
     * connect the bluetooth device
     * @param device
     */
    public void connect(BluetoothDevice device) {
        mBluetoothLeService.connect(device.getAddress());
    }

    /**
     * Disconnect the bluetooth
     */
    public void disconnect(){
        mBluetoothLeService.disconnect();
    }




    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
        new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                mStateListener.onFoundDevice(device);
            }
        };

    /**
     * Scan bluetooth devices
     * @param enable
     */
    private Handler mHandler;
    public void scanLeDevice(boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBtAdapter.stopLeScan(mLeScanCallback);
                    mStateListener.onScanStop();
                }
            }, 5000);

            mBtAdapter.startLeScan(mLeScanCallback);
        } else {
            mBtAdapter.stopLeScan(mLeScanCallback);
            mStateListener.onScanStop();
        }
    }





    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public void bindService(Context context){
        Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
        context.bindService(gattServiceIntent, mServiceConnection, context.BIND_AUTO_CREATE);
    }

    public void unbindService(Context context){
        context.unbindService(mServiceConnection);
    }








    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mStateListener.onConnected();
                mIsConnected = true;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mStateListener.onDisconnected();
                chModifyName = null;
                chReceiveData = null;
                mIsConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                initCharacteristic();
                mStateListener.onServicesDiscovered();
                mBluetoothLeService.setCharacteristicNotification(chReceiveData,true);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.e(TAG, "onReceive: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
            else if (BluetoothLeService.ACTION_SPO2_DATA_AVAILABLE.equals(action)) {
                mStateListener.onReceiveData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_SPO2_DATA_AVAILABLE);
        return intentFilter;
    }



    public void registerBtReceiver(Context context)
    {
        context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    public void unregisterBtReceiver(Context context)
    {
        context.unregisterReceiver(mGattUpdateReceiver);
    }

    public void initCharacteristic()
    {
        List<BluetoothGattService> services =
                mBluetoothLeService.getSupportedGattServices();
        BluetoothGattService mInfoService = null;
        BluetoothGattService mDataService = null;

        if(services == null) return;

        for(BluetoothGattService service : services) {
            if(service.getUuid().equals(Const.UUID_SERVICE_DATA)) {
                mDataService = service;
            }
        }
        if(mDataService != null) {
            List<BluetoothGattCharacteristic> characteristics =
                    mDataService.getCharacteristics();
            for(BluetoothGattCharacteristic ch: characteristics) {
                if(ch.getUuid().equals(Const.UUID_CHARACTER_RECEIVE)) {
                    chReceiveData = ch;
                }
                else if(ch.getUuid().equals(Const.UUID_MODIFY_BT_NAME)) {
                    chModifyName = ch;
                }
            }
        }
    }

    public boolean isChangeNameAvailable(){
        return chModifyName != null;
    }

    public void changeBtName(String name){

        if(mBluetoothLeService == null || chModifyName == null)
            return;

        if(name == null || name.equals(""))
            return;
        byte[] b = name.getBytes();
        byte[] bytes = new byte[b.length+2];
        bytes[0] = 0x00;
        bytes[1] = (byte) b.length;
        System.arraycopy(b,0,bytes,2,b.length);

        mBluetoothLeService.write(chModifyName,bytes);
    }




    /**
     * BTController interfaces
     */
    public interface StateListener
    {
        void onFoundDevice(BluetoothDevice device);

        void onConnected();
        void onDisconnected();
        void onReceiveData(byte[] dat);
        void onServicesDiscovered();
        void onScanStop();
    }
}
