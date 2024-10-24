# SpO2-BLE-for-Android

Error:
if (never) XXPermissions.startPermissionActivity(activity, permissions);
No android.support.v4.app.Fragment found

Fix:
Find the gradle.properties file
Add android.enableJetifier=true
 