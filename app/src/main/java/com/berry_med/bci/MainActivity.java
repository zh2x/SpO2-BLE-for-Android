package com.berry_med.bci;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.berry_med.bci.device_list.DeviceAdapter;
import com.berry_med.bci.device_list.DeviceListDialog;
import com.berry_med.bci.utils.Version;
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

    private TextView mSpo2;
    private TextView mPr;
    private TextView mPi;
    private TextView mRR;
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
        mSpo2 = findViewById(R.id.spo2);
        mPr = findViewById(R.id.pr);
        mPi = findViewById(R.id.pi);
        mRR = findViewById(R.id.rr);
        mWaveForm = findViewById(R.id.wave);
        TextView version = findViewById(R.id.version);
        mWaveForm.setWaveformVisibility(true);
        Button search = findViewById(R.id.search);
        search.setOnClickListener(this);
        _runnable();
        DeviceAdapter adapter = new DeviceAdapter(this);
        new Thread(mParseRunnable).start();
        ble = new BluetoothManager(this, adapter, mParseRunnable, mWaveForm);
        dialog = new DeviceListDialog(this, ble, adapter);
        ble.scanRule();

        version.setText(Version.getVersionName());
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

            @Override
            public void piVal(double pi) {
                runOnUiThread(() -> mPi.setText(pi > 0 ? (pi + "") : "--"));
            }

            @Override
            public void rrVal(int rr) {
                runOnUiThread(() -> mRR.setText(rr > 0 ? (rr + "") : "--"));
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.search) {
            Permissions.all(this, ble, dialog);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mParseRunnable.setStop(false);
    }
}