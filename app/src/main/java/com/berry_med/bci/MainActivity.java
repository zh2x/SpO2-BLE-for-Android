package com.berry_med.bci;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.berry_med.bci.blutooth.Model;
import com.berry_med.bci.blutooth.MyBluetooth;
import com.berry_med.bci.blutooth.ParseRunnable;
import com.berry_med.bci.blutooth.WaveForm;
import com.berry_med.bci.dialog.DeviceAdapter;
import com.berry_med.bci.dialog.MyDialog;
import com.berry_med.bci.utils.MyFiles;
import com.berry_med.bci.utils.Permissions;
import com.berry_med.bci.utils.ToastUtil;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private RadioGroup protocolRG;
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
    private TextView rrTitle;
    private TextView rrTv;
    private WaveForm mWaveForm;
    private EditText inputDeviceName;

    private MyBluetooth ble;
    private MyDialog dialog;
    private ParseRunnable mParseRunnable;
    private MyFiles myFiles;
    private ActivityResultLauncher<Intent> launcher;
    private Button startRecord;
    private long startTime = 0;

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
        protocolRG = findViewById(R.id.protocolRG);
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
        rrTitle = findViewById(R.id.rr_title);
        rrTv = findViewById(R.id.rrTv);
        mWaveForm = findViewById(R.id.wave_form);
        mWaveForm.setWaveformVisibility(true);
        inputDeviceName = findViewById(R.id.input_device_name);

        myFiles = new MyFiles();
        listener();

        launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        });
    }

    private void listener() {
        findViewById(R.id.search).setOnClickListener(this);
        findViewById(R.id.confirm).setOnClickListener(this);
        findViewById(R.id.hwBtn).setOnClickListener(this);
        findViewById(R.id.swBtn).setOnClickListener(this);
        startRecord = findViewById(R.id.start_record);
        startRecord.setOnClickListener(this);
        findViewById(R.id.share).setOnClickListener(this);

        protocolRG.setOnCheckedChangeListener((group, id) -> {
            if (id == R.id.bci_radio) {
                mHandler.sendEmptyMessage(0x02);
            } else if (id == R.id.bci_rr_radio) {
                mHandler.sendEmptyMessage(0x03);
            } else if (id == R.id.berry_radio) {
                mHandler.sendEmptyMessage(0x04);
            }
        });

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
            public void value(int spo2, int rr, int pr, double pi, int resp, int wave, int pf) {
                runOnUiThread(() -> {
                    mWaveForm.addAmplitude(wave);
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime > 100) {
                        spo2Tv.setText(spo2 != 127 ? String.valueOf(spo2) : "--");
                        prTv.setText(pr != 255 ? String.valueOf(pr) : "--");
                        piTv.setText(pi != 0 ? String.valueOf(pi) : "--");
                        rrTv.setText(rr != 0 ? String.valueOf(rr) : "--");
                        packetFreq.setText(pf != -1 ? pf + "Hz" : "--");
                        myFiles.writeTxt(currentTime, rr, pr, spo2);
                        startTime = currentTime;
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
        if (mParseRunnable != null) mParseRunnable.setStop(false);
        if (launcher != null) launcher.unregister();
        if (myFiles != null) myFiles.close();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.search) {
            startTime = System.currentTimeMillis();
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
        } else if (v.getId() == R.id.start_record) {
            startTime = System.currentTimeMillis(); 
            Permissions.storage(this);
            String name = startRecord.getText().toString().trim();
            if (ble.isConn()) {
                if (name.equals("Start")) {
                    mHandler.sendEmptyMessage(0x05);
                } else {
                    mHandler.sendEmptyMessage(0x06);
                }
            } else {
                mHandler.sendEmptyMessage(0x07);
            }
        } else if (v.getId() == R.id.share) {
            mHandler.sendEmptyMessageDelayed(0x08, 1000);
        }
    }

    Handler mHandler = new Handler(new Handler.Callback() {
        @SuppressLint("SetTextI18n")
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0x01:
                    ble.disconnectAllDevice();
                    name.setText("");
                    ToastUtil.showToastShort("Please reconnect the device!");
                    break;
                case 0x02:
                    Model.MODEL = "BCI";
                    rrTitle.setText("Resp Rate");
                    frequencyLayout.setVisibility(View.GONE);
                    ble.writeHex("0xE0");
                    break;
                case 0x03:
                    Model.MODEL = "BCI-RESP";
                    rrTitle.setText("Resp Rate");
                    frequencyLayout.setVisibility(View.GONE);
                    ble.writeHex("0xE0");
                    break;
                case 0x04:
                    Model.MODEL = "BERRY";
                    rrTitle.setText("RR");
                    frequencyLayout.setVisibility(View.VISIBLE);
                    ble.writeHex("0xE1");
                    break;
                case 0x05:
                    myFiles.createTxt();
                    startRecord.setText("Stop");
                    break;
                case 0x06:
                    startRecord.setText("Start");
                    myFiles.close();
                    break;
                case 0x07:
                    ToastUtil.showToastShort("Please connect the device!");
                    break;
                case 0x08:
                    shareFile();
                    break;
            }
            return false;
        }
    });

    private Uri fpUri(String path) {
        File file = new File(path);
        if (file.exists() && file.length() > 0) {
            return FileProvider.getUriForFile(this, getPackageName() + ".fileProvider", file);
        }
        return null;
    }

    private void shareFile() {
        Uri uri = fpUri(myFiles.getFilePath());
        if (uri != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Data");
            intent.putExtra(Intent.EXTRA_TEXT, "Content: ");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Intent chooser = Intent.createChooser(intent, "Share");
            @SuppressLint("QueryPermissionsNeeded")
            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            launcher.launch(Intent.createChooser(chooser, "Share"));
        } else {
            ToastUtil.showToastShort("No Data");
        }
    }
}