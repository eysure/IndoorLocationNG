package com.zjut.henry.indoorlocationng;

import android.app.ActivityManager;
import android.app.ActivityManager.*;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Android服务工具类
 * Created by henry on 12/3/17.
 */

class ServiceUtils {
    /**
     * 判断服务是否开启
     *
     * @return 是否开启
     */
    public static RunningServiceInfo findServiceByName(Context context, String ServiceName) {
        if (("").equals(ServiceName) || ServiceName == null)return null;
        List<RunningServiceInfo> services = getServices(context, 100);
        if(services==null || services.size()==0)return null;

        // Traverse the service list to get this service
        for (RunningServiceInfo info : services) {
            if (info.service.getClassName().equals(ServiceName)) return info;
        }
        return null;
    }

    /**
     * 得到服务列表
     *
     * @param context Context
     * @param max 列表最大值
     * @return 当前服务列表
     */
    public static List<RunningServiceInfo> getServices(Context context,int max) {
        ActivityManager myManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (myManager==null) return null;
        return myManager.getRunningServices(max);
    }
}