package com.berry_med.bci;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
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
import com.berry_med.bci.utils.Version;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView mName;
    private TextView mMac;
    private TextView mSpo2;
    private TextView mPr;
    private TextView mPi;
    //    private TextView mBattery;
    private TextView mRr;
//    private TextView mAf;

    private EditText mDeviceName;

    private MyBluetooth ble;
    private MyDialog dialog;
    private ParseRunnable mParseRunnable;
    private WaveForm mWaveForm;

    private RadioButton bciRadioButton;
    private RadioButton berryRadioButton;

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
        mName = findViewById(R.id.device_name);
        mMac = findViewById(R.id.device_mac);
        mSpo2 = findViewById(R.id.spo2);
        mPr = findViewById(R.id.pr);
        mPi = findViewById(R.id.pi);
//        mBattery = findViewById(R.id.battery);
        mRr = findViewById(R.id.rr);
//        mAf = findViewById(R.id.af);
        mDeviceName = findViewById(R.id.input_device_name);
        TextView version = findViewById(R.id.version);
        version.setText(Version.getVersionName());
        mWaveForm = findViewById(R.id.wave_form);
        mWaveForm.setWaveformVisibility(true);
        Button search = findViewById(R.id.search);
        search.setOnClickListener(this);
        Button confirm = findViewById(R.id.confirm);
        confirm.setOnClickListener(this);

//        RadioGroup mRadioGroup = findViewById(R.id.radioGroup);
        bciRadioButton = findViewById(R.id.bci_radio);
        berryRadioButton = findViewById(R.id.berry_radio);
//        mRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
//            if (checkedId == R.id.bci_radio) {
//                ble.writeHex("0xe0");
//            } else if (checkedId == R.id.berry_radio) {
//                ble.writeHex("0xe1");
//            }
//        });

        bciRadioButton.setOnClickListener(v -> mHandler.sendEmptyMessage(0x02));
        berryRadioButton.setOnClickListener(v -> mHandler.sendEmptyMessage(0x03));
    }

    private void initRunnable() {
        mParseRunnable = new ParseRunnable(new ParseRunnable.OnDataChangeListener() {
            @Override
            public void deviceInfo(String name, String mac) {
                runOnUiThread(() -> {
                    mName.setText(!TextUtils.isEmpty(name) ? name : "--");
                    mMac.setText(!TextUtils.isEmpty(mac) ? mac : "--");
                    mHandler.sendEmptyMessage(0x02);
                });
            }

            @Override
            public void value(int spo2, int pr, double pi, int rr, int wave) {
                runOnUiThread(() -> {
                    mSpo2.setText(spo2 > 0 ? (spo2 + "") : "--");
                    mPr.setText(pr > 0 ? (pr + "") : "--");
                    mPi.setText(pi > 0 ? (pi + "") : "--");
//                    mBattery.setText(battery > 0 ? (battery + "") : "--");
                    mRr.setText(rr > 0 ? (rr + "") : "--");
//                    mAf.setText(af > 0 ? (af + "") : "--");
                    mWaveForm.addAmplitude(wave);
                });
            }

            @Override
            public void model(boolean berry) {
                runOnUiThread(() -> {
                    if (berry) {
                        berryRadioButton.setChecked(true);
                    } else {
                        bciRadioButton.setChecked(true);
                    }
                });
            }
        });
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == 0x01) {
                ble.disconnectAllDevice();
                mDeviceName.setText("");
                ToastUtil.showToastShort("Please reconnect the device!");
            } else if (msg.what == 0x02) {
                ble.writeHex("0xe0");
            } else if (msg.what == 0x03) {
                ble.writeHex("0xe1");
            }
            return false;
        }
    });

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.search) {
            Permissions.all(this, ble, dialog);
        } else if (v.getId() == R.id.confirm) {
            String name = mDeviceName.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                ble.bleRename(name);
                mHandler.sendEmptyMessageDelayed(0x01, 500);
            } else {
                ToastUtil.showToastShort("Name is Empty");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mParseRunnable.setStop(false);
    }
}