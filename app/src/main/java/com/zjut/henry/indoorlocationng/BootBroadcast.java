package com.zjut.henry.indoorlocationng;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 此类用且仅用于本程序的开机自动启动
 * Created by henry on 12/3/17.
 */

class BootBroadcast extends BroadcastReceiver {

    /**
     * 接收到开机启动广播后启动服务
     * @param context 系统环境
     * @param intent 系统Intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent2 = new Intent(context, LocationService.class);
//			intent2.setAction("android.intent.action.MAIN");
//			intent2.addCategory("android.intent.category.LAUNCHER");
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startService(intent2);
    }
}