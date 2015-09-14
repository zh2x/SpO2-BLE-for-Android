package com.berry_med.spo2_ble;

import java.util.UUID;

/**
 * Created by ZXX on 2015/8/31.
 */
public class Const {

    public static final UUID  UUID_SERVICE_DATA                 = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455");
    public static final UUID       UUID_CHARACTER_RECEIVE       = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616");

    public static final UUID UUID_CLIENT_CHARACTER_CONFIG       = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int  MESSAGE_OXIMETER_PARAMS            = 2003;
    public static final int  MESSAGE_OXIMETER_WAVE              = 2004;

    public static final String GITHUB_SITE                      = "https://github.com/zh2x/SpO2_BLE";
}
