package com.berry_med.spo2_ble.dialog;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.berry_med.spo2_ble.R;

import java.util.ArrayList;

/**
 * Created by ZXX on 2015/12/30.
 */
public class DeviceListAdapter extends BaseAdapter
{

    private LayoutInflater mInflater;
    private ArrayList<BluetoothDevice> mDevices;

    public DeviceListAdapter(Context context, ArrayList<BluetoothDevice> devices)
    {
        this.mInflater = LayoutInflater.from(context);
        this.mDevices  = devices;
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return mDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        BluetoothDevice dev = mDevices.get(position);

        LinearLayout llItem = null;
        if(convertView != null)
        {
            llItem = (LinearLayout) convertView;
        }
        else
        {
            llItem  = (LinearLayout) mInflater.inflate(R.layout.search_dialog_device_item,null);
        }

        TextView tvName = (TextView) llItem.findViewById(R.id.tvBtItemName);
        TextView tvAddr = (TextView) llItem.findViewById(R.id.tvBtItemAddr);
        tvName.setText(dev.getName());
        tvAddr.setText("MAC: "+dev.getAddress());

        return llItem;
    }
}
