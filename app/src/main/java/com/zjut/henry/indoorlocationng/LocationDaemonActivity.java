package com.zjut.henry.indoorlocationng;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * LocationDaemonActivity
 * 定位守护Activity
 *
 * 此类的作用是启动定位服务, 并守护定位服务不被系统杀掉
 * 此Activity无UI, Create后开启服务便Destroy
 * Created by henry on 12/3/17.
 */
public class LocationDaemonActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("LocationDaemonActivity","Start.");

        // 启动服务
        ActivityManager.RunningServiceInfo service = ServiceUtils.findServiceByName(this,"com.zjut.henry.indoorlocationng.LocationService");
        if(service==null) {
            startLocationService(this);
            service = ServiceUtils.findServiceByName(this,"com.zjut.henry.indoorlocationng.LocationService");
            if(service==null) throw new RuntimeException("Service cannot start.");
        }
        else Log.d("Daemon","Service is running. Finish.");
        finish();
    }

    /**
     * 开始定位服务
     */
    private static void startLocationService(Context context){
        Intent i = new Intent(context,LocationService.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(i);
    }

}