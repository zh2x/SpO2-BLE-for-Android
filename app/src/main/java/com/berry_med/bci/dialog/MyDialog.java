package com.berry_med.bci.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ListView;

import com.berry_med.bci.R;
import com.berry_med.bci.blutooth.MyBluetooth;
import com.clj.fastble.data.BleDevice;

/*
 * description: Dialog
 * author: zl
 * date: 2024/10/23 10:03
 */
public class MyDialog {
    protected Context context;
    protected Dialog dialog;

    private final DeviceAdapter adapter;
    private final MyBluetooth ble;

    public MyDialog(Context context, MyBluetooth ble, DeviceAdapter adapter) {
        this.context = context;
        this.adapter = adapter;
        this.ble = ble;
        dialog = new Dialog(context, R.style.my_dialog);
    }

    public void show() {
        dialog.setContentView(R.layout.dialog_device_list);
        dialog.setCancelable(false);
        event(dialog);
        dialog.show();
        size(dialog);
    }

    private void event(Dialog dialog) {
        ble.scan();
        ListView listView = dialog.findViewById(R.id.list_view);
        ImageButton close = dialog.findViewById(R.id.close);
        ImageButton refresh = dialog.findViewById(R.id.refresh);
        close.setOnClickListener(v -> dismiss());
        refresh.setOnClickListener(v -> ble.scan());
        listView.setSelector(R.color.transparent);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            BleDevice bleDevice = adapter.getDevices().get(position);
            if (bleDevice != null) {
                ble.conn(bleDevice);
                dismiss();
            }
        });
    }

    public void dismiss() {
        dialog.dismiss();
    }

    private void size(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = width();
            lp.height = height();
            window.setAttributes(lp);
        }
    }

    private int width() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return (int) (displayMetrics.widthPixels * 0.9f);
    }

    private int height() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return (int) (displayMetrics.heightPixels * 0.6f);
    }
}
