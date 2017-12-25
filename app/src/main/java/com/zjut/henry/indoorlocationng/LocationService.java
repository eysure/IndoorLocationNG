package com.zjut.henry.indoorlocationng;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.zjut.henry.indoorlocationng.ServerConnection.*;

/**
 * LocationService
 * 定位服务类
 *
 * Created by henry on 12/3/17.
 */
public class LocationService extends Service {
    private static final int NOTIFICATION_ID = -1213;

    private static Context sContext;
    public static Handler sHandler;
    private static StepNav sStepNav;
    BluetoothLayer mBluetoothLayer;
    private static NotificationManager sNotificationManager;
    private static TimerTask sLocationLayer = new LocationLayer();

    /**
     * 只有在Service被创建的时刻被调用
     * 我们不能直接调用它，它是由系统负责调用的。
     */
    @Override
    public void onCreate() {
        sContext = this;
        initialHandler();
        new Thread(new ServerConnection(), "NetworkThread").start();
        ServerConnection.activateLinkStart();
        new Timer()
                .schedule(mBluetoothDaemon,GlobalParameter.SERVICE_DAEMON_PERIOD,GlobalParameter.SERVICE_DAEMON_PERIOD);
        registerPowerLock();
        sNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mBluetoothLayer = new BluetoothLayer(this);
        new Timer()
                .schedule(BeaconLinkLayer.sScavenger,GlobalParameter.BEACON_ONLINE_LIFE,GlobalParameter.BEACON_ONLINE_LIFE);   // 启动BeaconLinkLayer的Beacon过期清理任务
        new Timer()
                .schedule(RegionLayer.sRegionSwift,GlobalParameter.REGION_SWIFT_PERIOD,GlobalParameter.REGION_SWIFT_PERIOD);   // 启动地点切换器扫描任务
        new Timer()
                .schedule(sLocationLayer,GlobalParameter.LOCATION_PERIOD,GlobalParameter.LOCATION_PERIOD);   // 启动定位任务

        sStepNav = new StepNav(this);
        sStepNav.setOnStepUpdateListener(new StepNav.OnStepUpdateListener() {
            @Override
            public void onStepUpdate() {
                sLocationLayer.run();
            }
        });
    }

    /**
     * 服务销毁时注销蓝牙广播
     */
    @Override
    public void onDestroy() {
        unregisterReceiver(mBluetoothLayer.getReceiver());    // 注销蓝牙广播
        super.onDestroy();
    }

    /**
     * OnStartCommand方法是最重要的方法，因为它在我们需要启动Service的时候被调用。
     * 在这个方法中，我们拥有在运行Service 时传递进来的Intent，这样就可以与Service交换一些信息。
     * 在这个方法中，我们实现自己的逻辑：如果不是耗时的操作可以直接在这个方法中执行， 否则可以创建一个线程。
     * 正如你看到的那样，这个方法需要返回一个整型值。
     * @param intent 传入的intent
     * @param flags 传入的flag
     * @param startId 传入的startId
     * @return START_STICKY / START_NOT_STICKY / START_REDELIVER_INTENT
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification("LocationService","Started."));
        return super.onStartCommand(intent, START_STICKY_COMPATIBILITY, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 创建前台服务通知栏
     * @return 通知栏
     */
    private static Notification createNotification(String title, String text){
        return new Notification.Builder(sContext)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setShowWhen(true)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .build();
    }

    /**
     * 更新通知栏
     * @param title 标题
     * @param text 文本
     */
    public static void updateNotification(String title, String text){
        sNotificationManager.notify(NOTIFICATION_ID, createNotification(title,text));
    }

    /**
     * 蓝牙扫描守护机制
     * 每次调用时启动MainActivity一次, 以防止蓝牙数据中断
     */
    private TimerTask mBluetoothDaemon = new TimerTask() {
        @Override
        public void run() {
            Log.d("Service","Daemon TimerTask");
            Intent intent = new Intent(sContext,LocationDaemonActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            sContext.startActivity(intent);
        }
    };

    /**
     * 注册PowerLock
     * 进一步防止手机在锁屏后切断蓝牙和网络 (亲测并无卵)
     */
    private void registerPowerLock(){
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
        wakeLock.acquire();
    }

    /**
     * 初始化Handler
     */
    private void initialHandler() {
        sHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Bundle b = msg.getData();
                if(b!=null)switch (msg.what) {
                    // Get network status and show warning
                    case CONNECTION_STATUS: {
                        String status = b.getString("status");
                        if(status==null)break;
                        switch (status) {
                            case "connected":Log.i("Service","Connected to server.");break;
                            case "failed":Log.e("Service","Connection failed!");break;
                            default:break;
                        }break;
                    }
                    default:super.handleMessage(msg);break;
                }
            }
        };
    }
}