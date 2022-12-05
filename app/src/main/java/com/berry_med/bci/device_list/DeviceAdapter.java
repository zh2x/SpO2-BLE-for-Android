package com.berry_med.bci.device_list;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.berry_med.bci.R;
import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;
import java.util.List;
/*
 * @deprecated DeviceAdapter
 * @author zl
 * @date 2022/12/2 17:25
 */
public class DeviceAdapter extends BaseAdapter {
    protected Context context;
    protected List<BleDevice> devices;

    public DeviceAdapter(Context context) {
        this.context = context;
        devices = new ArrayList<>();
    }

    //Add
    public void addDevice(BleDevice bleDevice) {
        if (bleDevice != null) {
            if (!TextUtils.isEmpty(bleDevice.getName())) {
                devices.add(bleDevice);
                devices.sort((o1, o2) -> o2.getRssi() - o1.getRssi());
            }
        }
    }

    public void clear() {
        if (devices != null) devices.clear();
    }

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
        if (devices != null && devices.size() > 0) {
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

