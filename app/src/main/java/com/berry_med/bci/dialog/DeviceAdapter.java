package com.berry_med.bci.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.berry_med.bci.R;
import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;
import java.util.List;

/*
 * description: Device Adapter
 * author: zl
 * date: 2024/10/23 9:58
 */
public class DeviceAdapter extends BaseAdapter {
    private final Context context;
    private final List<BleDevice> devices;


    public DeviceAdapter(Context context) {
        this.context = context;
        devices = new ArrayList<>();
    }

    // Add Device
    public void addDevice(BleDevice bleDevice) {
        if (bleDevice != null && !TextUtils.isEmpty(bleDevice.getName())) {
            devices.add(bleDevice);
            devices.sort((o1, o2) -> o2.getRssi() - o1.getRssi());
        }
    }

    // Clean
    public void clean() {
        devices.clear();
    }

    // Get Device
    public List<BleDevice> getDevices() {
        return devices;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = View.inflate(context, R.layout.adapter_device, null);
            holder = new ViewHolder();
            view.setTag(holder);
            holder.name = view.findViewById(R.id.name);
            holder.mac = view.findViewById(R.id.mac);
            holder.rssi = view.findViewById(R.id.rssi);
        }
        if (!devices.isEmpty()) {
            BleDevice bleDevice = devices.get(position);
            if (bleDevice != null) {
                holder.name.setText(bleDevice.getName());
                holder.mac.setText(bleDevice.getMac());
                holder.rssi.setText("" + bleDevice.getRssi());
            }
        }
        return view;
    }


    static class ViewHolder {
        TextView name;
        TextView mac;
        TextView rssi;
    }
}
