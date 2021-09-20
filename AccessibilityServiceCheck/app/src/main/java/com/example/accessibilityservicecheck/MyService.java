package com.example.accessibilityservicecheck;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class MyService extends AccessibilityService {
    private String TAG = "MyService";
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d("event", String.valueOf(event.getEventTime()));
        Log.d("event", String.valueOf(event));
        Log.e(TAG, "onAccessibilityEvent: ");
        String packageName = event.getPackageName().toString();
        PackageManager packageManager = this.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            final CharSequence applicationLabel = packageManager.getApplicationLabel(applicationInfo);
            Log.e(TAG, "App name is: "+applicationLabel);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "onInterrupt: Something went wrong");
    }

//    @Override
//    protected void onServiceConnected() {
//        super.onServiceConnected();
//        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
//        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
//                AccessibilityEvent.TYPE_VIEW_FOCUSED;
//
//        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
//        info.notificationTimeout = 100;
//
//        this.setServiceInfo(info);
//        Log.e(TAG, "onServiceConnected: ");
//    }
}
