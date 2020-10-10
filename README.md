
## SpO2 BLE for Android  

This demo will show you how to connect the **BLE Oximeters**, obtain & parse data, and display the pulse waveform. For protocol details, please download the paper <https://github.com/zh2x/BCI_Protocol>

该demo用于演示如何连接 **BLE 蓝牙血氧仪**，获取并解析数据以及显示脉搏波形。关于协议细节可以下载协议文档 <https://github.com/zh2x/BCI_Protocol>



## Location Permissions

~~~xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
~~~

if `targetSdkVersion >= 23` , you shall add the permissions below, and make sure the app can obtain these permissions.

如果 `targetSdkVersion >= 23` , 请加入一下权限, 并确保app能得到这些权限。