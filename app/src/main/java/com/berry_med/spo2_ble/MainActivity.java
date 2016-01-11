package com.berry_med.spo2_ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.berry_med.OximeterData.DataParser;
import com.berry_med.OximeterData.PackageParser;
import com.berry_med.waveform.WaveForm;
import com.berry_med.waveform.WaveFormParams;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PackageParser.OnDataChangeListener{

    private final static String TAG = MainActivity.class.getSimpleName();

    private static final long SCAN_PERIOD = 3000;

    private LinearLayout rlInfoBtns;
    private LinearLayout llModifyBtName;
    private Button btnBluetoothToggle;
    private Button btnSearchOximeters;
    private TextView tvStatusBar;
    private TextView tvParamsBar;
    private EditText edBluetoothName;
    private EditText edNewBtName;


    private BluetoothAdapter   mBluetoothAdapter;
    private BluetoothDevice    mTargetDevice;
    private BluetoothLeService mBluetoothLeService;

    private BluetoothGattCharacteristic chReceive;
    private BluetoothGattCharacteristic chChangeBtName;


    private boolean mIsNotified;
    private boolean mIsScanFinished;

    private DataParser mDataParser;
    private PackageParser mPackageParser;
    private WaveForm mSpO2WaveDraw;

    private String strTargetBluetoothName = "BerryMed";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edBluetoothName = (EditText) findViewById(R.id.edBluetoothName);

        btnBluetoothToggle = (Button) findViewById(R.id.btnBluetoothToggle);
        btnSearchOximeters = (Button) findViewById(R.id.btnSearchOximeters);
        tvStatusBar        = (TextView) findViewById(R.id.tvStatusBar);
        tvParamsBar        = (TextView) findViewById(R.id.tvParamsBar);
        rlInfoBtns         = (LinearLayout) findViewById(R.id.rlInfoBtns);
        llModifyBtName     = (LinearLayout) findViewById(R.id.llModifyBtName);
        edNewBtName        = (EditText) findViewById(R.id.etNewBtName);

        rlInfoBtns.setVisibility(View.GONE);

        //init bluetooth adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if(mBluetoothAdapter.isEnabled())
        {
            btnBluetoothToggle.setText(getString(R.string.turn_off_bluetooth));
            btnSearchOximeters.setEnabled(true);
        }

        SurfaceView sfvSpO2 = (SurfaceView) findViewById(R.id.sfvSpO2);
        WaveFormParams mSpO2WaveParas = new WaveFormParams(3,2,new int[]{0,100});
        mSpO2WaveDraw = new WaveForm(this, sfvSpO2,mSpO2WaveParas);

        TextView tvGetSource = (TextView) findViewById(R.id.tvGetSource);
        tvGetSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(Const.GITHUB_SITE)));
            }
        });

        //******************************** package parse******************************
        mDataParser = new DataParser(DataParser.Protocol.BCI, new DataParser.onPackageReceivedListener() {
            @Override
            public void onPackageReceived(int[] dat) {
                Log.i(TAG, "onPackageReceived: " + Arrays.toString(dat));
                if(mPackageParser == null) {
                    mPackageParser = new PackageParser(MainActivity.this);
                }

                mPackageParser.parse(dat);
            }
        });

        mDataParser.start();
        //*******************************************************************************

    }

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what)
            {
                case Const.MESSAGE_OXIMETER_PARAMS:
                    tvParamsBar.setText("SpO2: "+ msg.arg1 + "   Pulse Rate:"+msg.arg2);
                    break;
                case Const.MESSAGE_OXIMETER_WAVE:
                    mSpO2WaveDraw.add(msg.arg1);
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mTargetDevice.getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDataParser.stop();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    @SuppressWarnings("deprecation")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    if(!mIsScanFinished)
                    {
                        tvStatusBar.setText("No devices found.");
                        mIsNotified = true;
                    }
                }
            }, SCAN_PERIOD);

            mBluetoothAdapter.startLeScan(mLeScanCallback);
            mIsScanFinished = false;
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mIsScanFinished = true;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(device.getName().equals(strTargetBluetoothName))
                            {
                                mTargetDevice = device;
                                scanLeDevice(false);
                                tvStatusBar.setText("Name:"+device.getName()+"     "+"Mac:"+device.getAddress());

                                //start BluetoothLeService

                                Intent gattServiceIntent = new Intent(MainActivity.this, BluetoothLeService.class);
                                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                            }
                        }
                    });
                }
            };


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mTargetDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };



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
                Toast.makeText(MainActivity.this, "Connected",Toast.LENGTH_SHORT).show();
                mIsNotified = false;
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(MainActivity.this, "Disconnected",Toast.LENGTH_SHORT).show();
                unbindService(mServiceConnection);
                mBluetoothLeService = null;
                rlInfoBtns.setVisibility(View.GONE);
                mIsNotified = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                initCharacteristic();
                rlInfoBtns.setVisibility(View.VISIBLE);

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Toast.makeText(MainActivity.this,
                               intent.getStringExtra(BluetoothLeService.EXTRA_DATA),
                               Toast.LENGTH_SHORT).show();
            }
            else if (BluetoothLeService.ACTION_SPO2_DATA_AVAILABLE.equals(action)) {
                mDataParser.add(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
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

    public void onClick(View v)
    {

        switch(v.getId())
        {
            case R.id.btnBluetoothToggle:
                if(btnBluetoothToggle.getText().toString().equals(getString(R.string.turn_on_bluetooth)))
                {
                    btnBluetoothToggle.setText(R.string.turn_off_bluetooth);
                    //turn on bluetooth
                    if(!mBluetoothAdapter.isEnabled())
                    {
                        mBluetoothAdapter.enable();
                    }
                    btnSearchOximeters.setEnabled(true);
                    edBluetoothName.setFocusable(false);
                    edBluetoothName.setFocusableInTouchMode(false);
                }
                else
                {
                    btnBluetoothToggle.setText(R.string.turn_on_bluetooth);
                    //turn off bluetooth
                    if(mBluetoothAdapter.isEnabled())
                    {
                        mBluetoothAdapter.disable();
                    }
                    btnSearchOximeters.setEnabled(false);
                    edBluetoothName.setFocusableInTouchMode(true);
                    edBluetoothName.setFocusable(true);
                    edBluetoothName.requestFocus();
                }
                break;

            case R.id.btnSearchOximeters:
                strTargetBluetoothName = edBluetoothName.getText().toString();

                scanLeDevice(true);
                tvStatusBar.setText("Searching...");
                break;
            case R.id.btnNotify:
                if(chReceive != null)
                {
                    if(!mIsNotified)
                    {
                        mBluetoothLeService.setCharacteristicNotification(chReceive,true);
                        Log.i(TAG,">>>>>>>>>>>>>>>>>>>>START<<<<<<<<<<<<<<<<<<<");
                    }
                    else
                    {
                        mBluetoothLeService.setCharacteristicNotification(chReceive,false);
                        Log.i(TAG,">>>>>>>>>>>>>>>>>>>>STOP<<<<<<<<<<<<<<<<<<<");
                    }
                    mIsNotified = !mIsNotified;
                }
                break;
            case R.id.btnModifyBtName:
                String btName = edNewBtName.getText().toString();
                PackageParser.modifyBluetoothName(mBluetoothLeService, chChangeBtName, btName);
                break;
        }
    }

    public void initCharacteristic()
    {
        List<BluetoothGattService> services =
                mBluetoothLeService.getSupportedGattServices();
        BluetoothGattService mInfoService = null;
        BluetoothGattService mDataService = null;
        for(BluetoothGattService service : services)
        {
            if(service.getUuid().equals(Const.UUID_SERVICE_DATA))
            {
                mDataService = service;
            }
        }
        if(mDataService != null)
        {
            List<BluetoothGattCharacteristic> characteristics =
                    mDataService.getCharacteristics();
            for(BluetoothGattCharacteristic ch: characteristics)
            {
                if(ch.getUuid().equals(Const.UUID_CHARACTER_RECEIVE))
                {
                    chReceive = ch;
                }
                else if(ch.getUuid().equals(Const.UUID_MODIFY_BT_NAME))
                {
                    chChangeBtName = ch;
                    llModifyBtName.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onSpO2ParamsChanged() {
        PackageParser.OxiParams params = mPackageParser.getOxiParams();
        mHandler.obtainMessage(Const.MESSAGE_OXIMETER_PARAMS,params.getSpo2(),params.getPulseRate()).sendToTarget();
    }

    @Override
    public void onSpO2WaveChanged(int wave) {
         mHandler.obtainMessage(Const.MESSAGE_OXIMETER_WAVE,wave,0).sendToTarget();
    }
}
