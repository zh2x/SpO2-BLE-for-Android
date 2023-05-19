package com.berry_med.bci.utils.ble;

/**
 * device model
 */
public class DeviceModel {
    public static final String BCI_ORDINARY = "00:00:00";//Package Length: 5 bytes
    public static final String BCI_BATTERY  = "00:01:00";//Battery Package Length: 6 bytes
    public static final String BCI_RR       = "00:02:00";//RR Package Length: 7 bytes
    public static final String BCI_AF       = "00:03:00";//AF Package Length: 8 bytes
    public static final String BCI_RR_AF    = "00:04:00";//RR+AF Package Length: 9 bytes
}
