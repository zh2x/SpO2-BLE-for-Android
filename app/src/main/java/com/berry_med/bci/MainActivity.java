package com.berry_med.bci;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.berry_med.bci.blutooth.MyBluetooth;
import com.berry_med.bci.blutooth.ParseRunnable;
import com.berry_med.bci.blutooth.WaveForm;
import com.berry_med.bci.dialog.DeviceAdapter;
import com.berry_med.bci.dialog.MyDialog;
import com.berry_med.bci.utils.Permissions;
import com.berry_med.bci.utils.ToastUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private RadioButton bciRadio;
    private RadioButton berryRadio;
    private LinearLayout frequencyLayout;
    private RadioGroup frequencyView;
    private TextView packetFreq;
    private TextView name;
    private TextView mac;
    private TextView hw;
    private TextView sw;
    private TextView spo2Tv;
    private TextView prTv;
    private TextView piTv;
    private TextView rrTv;
    private WaveForm mWaveForm;
    private EditText inputDeviceName;

    private MyBluetooth ble;
    private MyDialog dialog;
    private ParseRunnable mParseRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initRunnable();
        DeviceAdapter adapter = new DeviceAdapter(this);
        new Thread(mParseRunnable).start();
        ble = new MyBluetooth(this, adapter, mParseRunnable, mWaveForm);
        dialog = new MyDialog(this, ble, adapter);
        ble.scanRule();
    }

    private void initView() {
        bciRadio = findViewById(R.id.bci_radio);
        berryRadio = findViewById(R.id.berry_radio);
        frequencyLayout = findViewById(R.id.frequency_layout);
        frequencyView = findViewById(R.id.frequency_view);
        packetFreq = findViewById(R.id.packetFreq);
        name = findViewById(R.id.name);
        mac = findViewById(R.id.mac);
        hw = findViewById(R.id.hw);
        sw = findViewById(R.id.sw);
        spo2Tv = findViewById(R.id.spo2Tv);
        prTv = findViewById(R.id.prTv);
        piTv = findViewById(R.id.piTv);
        rrTv = findViewById(R.id.rrTv);
        mWaveForm = findViewById(R.id.wave_form);
        mWaveForm.setWaveformVisibility(true);
        inputDeviceName = findViewById(R.id.input_device_name);

        bciRadio.setOnClickListener(v -> mHandler.sendEmptyMessage(0x02));
        berryRadio.setOnClickListener(v -> mHandler.sendEmptyMessage(0x03));

        listener();
    }

    private void listener() {
        findViewById(R.id.search).setOnClickListener(this);
        findViewById(R.id.confirm).setOnClickListener(this);
        findViewById(R.id.hwBtn).setOnClickListener(this);
        findViewById(R.id.swBtn).setOnClickListener(this);

        frequencyView.setOnCheckedChangeListener((group, id) -> {
            if (id == R.id.rb1) {
                ble.writeHex("0xF3");
            } else if (id == R.id.rb2) {
                ble.writeHex("0xF0");
            } else if (id == R.id.rb3) {
                ble.writeHex("0xF1");
            } else if (id == R.id.rb4) {
                ble.writeHex("0xF2");
            } else if (id == R.id.rb5) {
                ble.writeHex("0xF6");
            }
        });
    }

    private void initRunnable() {
        mParseRunnable = new ParseRunnable(new ParseRunnable.OnDataChangeListener() {
            @Override
            public void deviceInfo(String dName, String dMac) {
                runOnUiThread(() -> {
                    name.setText(dName);
                    mac.setText(dMac);
                });
            }

            @Override
            public void value(int spo2, int pr, double pi, int rr, int wave, int pf) {
                runOnUiThread(() -> {
                    spo2Tv.setText(spo2 != 127 ? String.valueOf(spo2) : "--");
                    prTv.setText(pr != 255 ? String.valueOf(pr) : "--");
                    piTv.setText(pi > 0 ? String.valueOf(pi) : "--");
                    rrTv.setText(rr > 0 ? String.valueOf(rr) : "--");
                    packetFreq.setText(pf != -1 ? pf + "Hz" : "--");
                    mWaveForm.addAmplitude(wave);
                });
            }

            @Override
            public void model(boolean berry) {
                runOnUiThread(() -> {
                    if (berry) {
                        berryRadio.setChecked(true);
                        frequencyLayout.setVisibility(View.VISIBLE);
                    } else {
                        bciRadio.setChecked(true);
                        frequencyLayout.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void hardwareVersion(String v) {
                runOnUiThread(() -> hw.setText(v));
            }

            @Override
            public void softwareVersion(String v) {
                runOnUiThread(() -> sw.setText(v));
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mParseRunnable.setStop(false);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.search) {
            Permissions.all(this, ble, dialog);
        } else if (v.getId() == R.id.confirm) {
            String n = inputDeviceName.getText().toString().trim();
            if (!TextUtils.isEmpty(n)) {
                ble.bleRename(n);
                mHandler.sendEmptyMessageDelayed(0x01, 500);
            } else {
                ToastUtil.showToastShort("Name is empty");
            }
        } else if (v.getId() == R.id.hwBtn) {
            ble.writeHex("0xFE");
        } else if (v.getId() == R.id.swBtn) {
            ble.writeHex("0xFF");
        }
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == 0x01) {
                ble.disconnectAllDevice();
                name.setText("");
                ToastUtil.showToastShort("Please reconnect the device!");
            } else if (msg.what == 0x02) {
                ble.writeHex("0xE0");
            } else if (msg.what == 0x03) {
                ble.writeHex("0xE1");
            }
            return false;
        }
    });
}