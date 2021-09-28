package com.example.accessibilityservicecheck;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Browser;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MyService extends AccessibilityService {
    private String TAG = "MyService";
    private HashMap<String, Long> previousUrlDetections = new HashMap<>();
    String packageName;
    public String foregroundAppName;
    public String googleAppSearchBarId = "com.google.android.googlequicksearchbox:id/search_box";
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //Log.e(TAG, "onAccessibilityEvent: ");
        //Log.d("event", String.valueOf(event.getEventTime()));
        //Log.d("event", String.valueOf(event));


        //get accessibility node info
        AccessibilityNodeInfo parentNodeInfo = event.getSource();
        if (parentNodeInfo == null) {
            return;
        }
        if(event.getPackageName() != null){
            packageName = event.getPackageName().toString();
        }


        //get foreground app name
        PackageManager packageManager = this.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            foregroundAppName = (String) packageManager.getApplicationLabel(applicationInfo);
            Log.e(TAG, "App name is: "+foregroundAppName);
            Toast.makeText(this, foregroundAppName, Toast.LENGTH_SHORT);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //dfs(parentNodeInfo);
        //get all the child views from the nodeInfo
        getChild(parentNodeInfo);

        if(foregroundAppName.equals("Google")){
            String googleAppsSearchTerm = getGoogleAppsSearchTerm(parentNodeInfo);
            if(googleAppsSearchTerm != null){
                Log.d("searchTerm:", googleAppsSearchTerm);
            }
        }

        //fetch urls from different browsers
        SupportedBrowserConfig browserConfig = null;
        for (SupportedBrowserConfig supportedConfig: getSupportedBrowsers()) {
            if (supportedConfig.packageName.equals(packageName)) {
                browserConfig = supportedConfig;
            }
        }
        //this is not supported browser, so exit
        if (browserConfig == null) {
            return;
        }
        String capturedUrl = captureUrl(parentNodeInfo, browserConfig);
        parentNodeInfo.recycle();

        //we can't find a url. Browser either was updated or opened page without url text field
        if (capturedUrl == null) {
            return;
        }
        Log.d("captured URL",capturedUrl);

        long eventTime = event.getEventTime();
        String detectionId = packageName + ", and url " + capturedUrl;
        //noinspection ConstantConditions
        long lastRecordedTime = previousUrlDetections.containsKey(detectionId) ? previousUrlDetections.get(detectionId) : 0;
        //some kind of redirect throttling
        if (eventTime - lastRecordedTime > 2000) {
            previousUrlDetections.put(detectionId, eventTime);
           // analyzeCapturedUrl(capturedUrl, browserConfig.packageName);
        }
    }

    public void dfs(AccessibilityNodeInfo info){
        if(info == null)
            return;
        if(info.getText() != null && info.getText().length() > 0)
            System.out.println(info.getText() + " class: "+info.getClassName());
        for(int i=0;i<info.getChildCount();i++){
            AccessibilityNodeInfo child = info.getChild(i);
            dfs(child);
            if(child != null){
                child.recycle();
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "onInterrupt: Something went wrong");
    }

    @Override
    protected void onServiceConnected() {
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
    }

    private String getGoogleAppsSearchTerm(AccessibilityNodeInfo info){
        //Log.d("in func", "in getgoogleappsearchterm");
        List<AccessibilityNodeInfo> nodes = info.findAccessibilityNodeInfosByViewId(googleAppSearchBarId);
        if (nodes == null || nodes.size() <= 0) {
            return null;
        }

        AccessibilityNodeInfo searchBarNodeInfo = nodes.get(0);
        String searchTerm = null;
        if (searchBarNodeInfo.getText() != null) {
            searchTerm = searchBarNodeInfo.getText().toString();
            //Log.d("url in func", url);
        }
        searchBarNodeInfo.recycle();
        //Log.d("search in func",searchTerm);
        return searchTerm;
    }

    private void getChild(AccessibilityNodeInfo info)
    {
        int i=info.getChildCount();
        for(int p=0;p<i;p++)
        {
            AccessibilityNodeInfo n=info.getChild(p);
            if(n!=null) {
                String strres = n.getViewIdResourceName();
                if (n.getText() != null) {
                    String txt = n.getText().toString();
                    Log.d("Track child", strres + "  :  " + txt);
                }
                getChild(n);
            }
        }
    }

    private static class SupportedBrowserConfig {
        public String packageName, addressBarId;
        public SupportedBrowserConfig(String packageName, String addressBarId) {
            this.packageName = packageName;
            this.addressBarId = addressBarId;
        }
    }

    @NonNull
    private static List<SupportedBrowserConfig> getSupportedBrowsers() {
        List<SupportedBrowserConfig> browsers = new ArrayList<>();
        browsers.add( new SupportedBrowserConfig("com.android.chrome", "com.android.chrome:id/url_bar"));
        browsers.add( new SupportedBrowserConfig("org.mozilla.firefox", "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"));
        browsers.add( new SupportedBrowserConfig("com.opera.browser", "com.opera.browser:id/url_field"));
        browsers.add( new SupportedBrowserConfig("com.opera.mini.native", "com.opera.mini.native:id/url_field"));
        browsers.add( new SupportedBrowserConfig("com.duckduckgo.mobile.android", "com.duckduckgo.mobile.android:id/omnibarTextInput"));
        browsers.add( new SupportedBrowserConfig("com.microsoft.emmx", "com.microsoft.emmx:id/url_bar"));
        return browsers;
    }

    private String captureUrl(AccessibilityNodeInfo info, SupportedBrowserConfig config) {
        List<AccessibilityNodeInfo> nodes = info.findAccessibilityNodeInfosByViewId(config.addressBarId);
        if (nodes == null || nodes.size() <= 0) {
            return null;
        }
        //System.out.println("nodeInfo"+ String.valueOf(info));
        AccessibilityNodeInfo addressBarNodeInfo = nodes.get(0);
        String url = null;
        if (addressBarNodeInfo.getText() != null) {
            url = addressBarNodeInfo.getText().toString();
            //Log.d("url in func", url);
        }
        addressBarNodeInfo.recycle();
        return url;
    }

    private void analyzeCapturedUrl(@NonNull String capturedUrl, @NonNull String browserPackage) {
        String redirectUrl = "your redirect url is here";
        if (capturedUrl.contains("facebook.com")) {
            performRedirect(redirectUrl, browserPackage);
        }
    }

    // we just reopen the browser app with our redirect url using service context
    private void performRedirect(@NonNull String redirectUrl, @NonNull String browserPackage) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl));
            intent.setPackage(browserPackage);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, browserPackage);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        catch(ActivityNotFoundException e) {
            // the expected browser is not installed
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl));
            startActivity(i);
        }
    }
}
