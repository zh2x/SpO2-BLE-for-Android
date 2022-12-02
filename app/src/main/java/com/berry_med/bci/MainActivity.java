package com.berry_med.bci;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.berry_med.bci.device_list.DeviceAdapter;
import com.berry_med.bci.device_list.DeviceListDialog;
import com.berry_med.bci.utils.ble.WaveForm;
import com.berry_med.bci.utils.ble.BluetoothManager;
import com.berry_med.bci.utils.ble.ParseRunnable;
import com.berry_med.bci.utils.per.Permissions;

/*
 * @deprecated  MainActivity
 * @author zl
 * @date 2022/12/2 13:44
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button search;
    private TextView mSpo2;
    private TextView mPr;
    private WaveForm mWaveForm;
    private BluetoothManager ble;
    private DeviceListDialog dialog;
    private ParseRunnable mParseRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _init();
    }

    private void _init() {
        Permissions.all(this);
        search = findViewById(R.id.search);
        mSpo2 = findViewById(R.id.spo2);
        mPr = findViewById(R.id.pr);
        mWaveForm = findViewById(R.id.wave);
        mWaveForm.setWaveformVisibility(true);
        search.setOnClickListener(this);
        _runnable();
        DeviceAdapter adapter = new DeviceAdapter(this);
        new Thread(mParseRunnable).start();
        ble = new BluetoothManager(this, adapter, mParseRunnable, mWaveForm);
        dialog = new DeviceListDialog(this, ble, adapter);
        ble.scanRule();
    }

    private void _runnable() {
        mParseRunnable = new ParseRunnable(new ParseRunnable.OnDataChangeListener() {
            @Override
            public void spo2Val(int spo2) {
                runOnUiThread(() -> mSpo2.setText(spo2 > 0 ? (spo2 + "") : "--"));
            }

            @Override
            public void prVal(int pr) {
                runOnUiThread(() -> mPr.setText(pr > 0 ? (pr + "") : "--"));
            }

            @Override
            public void waveVal(int wave) {
                runOnUiThread(() -> mWaveForm.addAmplitude(wave));
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        mWaveForm.clear();
        ble.disconnectAllDevice();
        if (v.getId() == R.id.search) {
            Permissions.all(this);
            dialog.show();
        }
    }
}