package com.zjut.henry.indoorlocationng;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;

import static com.zjut.henry.indoorlocationng.GlobalParameter.*;

/**
 * LocationController
 * 整个定位系统的控制器,入口
 *
 * 负责供其他包的调用, 启动服务, 启动守护, 设置全局参数, 以及收取定位结果
 * Created by henry on 12/14/17.
 */

public class LocationController {
    private Context mContext;
    private static OnBeaconUpdateListener sOnBeaconUpdateListener;
    private static OnLocationUpdateListener sOnLocationUpdateListener;

    /**
     * 构造方法, 需要传入当前Context
     * @param context 传入Context
     */
    public LocationController(Context context) {
        mContext = context;
    }

    /**
     * 启动定位守护程序 -> 启动定位服务
     */
    public void start() {
        if (sOnLocationUpdateListener==null)
            throw new RuntimeException("还没有设定地点更新监听器, 请先实现setOnLocationUpdateListener再启动定位.");
        Intent intent = new Intent(mContext, LocationDaemonActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);
    }

    /**
     * 设置定位更新回调监听器
     * @param onLocationUpdateListener 定位更新回调监听器
     */
    public void setOnLocationUpdateListener(OnLocationUpdateListener onLocationUpdateListener) {
        if(onLocationUpdateListener!=null)sOnLocationUpdateListener = onLocationUpdateListener;
    }

    /**
     * 位置更新回调的接口定义, 在位置更新时被调用, 在实例中被实现
     */
    public interface OnLocationUpdateListener {
        /**
         * 实现地点更新时的回调
         * 注意: 地点更新线程非主线程, 不能在这里直接执行UI操作, 如需更新UI, 请使用Handle
         * @param locationResult 地点更新结果
         */
        void onLocationUpdate(LocationResult locationResult);
    }

    /**
     * 注册一个回调, 当位置更新时即被唤起
     * @param locationResult 更新的位置结果
     */
    static void updateLocationResult(LocationResult locationResult) {
        sOnLocationUpdateListener.onLocationUpdate(locationResult);
    }

    /**
     * 设置Beacon更新回调监听器
     * @param onBeaconUpdateListener Beacon更新回调监听器
     */
    public void setOnBeaconUpdateListener(OnBeaconUpdateListener onBeaconUpdateListener) {
        sOnBeaconUpdateListener = onBeaconUpdateListener;
    }

    /**
     * Beacon传入的接口定义, 在Beacon(BLE)传入时被唤起
     */
    public interface OnBeaconUpdateListener {
        /**
         * Beacon传入回调
         * @param scanResult Beacon传入结果
         */
        void onBeaconUpdate(ScanResult scanResult);
    }

    /**
     * 注册一个回调，当Beacon(BLE)传入时被唤起
     * @param scanResult 传入的ScanResult
     */
    static void updateBeaconResult(ScanResult scanResult) {
        if(scanResult!=null) sOnBeaconUpdateListener.onBeaconUpdate(scanResult);
    }

    /**
     * 手动获得定位结果
     * @return 当前定位结果
     */
    public static LocationResult getLocationResult() {
        return LocationLayer.toResult();
    }

    // -------------Setter & Getter-------------

    public String getServerIp() {
        return SERVER_IP;
    }

    public void setServerIp(String serverIp) {
        SERVER_IP = serverIp;
    }

    public int getServerPort() {
        return SERVER_PORT;
    }

    public void setServerPort(int serverPort) {
        SERVER_PORT = serverPort;
    }

    public long getServiceDaemonPeriod() {
        return SERVICE_DAEMON_PERIOD;
    }

    public void setServiceDaemonPeriod(long serviceDaemonPeriod) {
        SERVICE_DAEMON_PERIOD = serviceDaemonPeriod;
    }

    public long getBeaconCacheLife() {
        return BEACON_CACHE_LIFE;
    }

    public void setBeaconCacheLife(long beaconCacheLife) {
        BEACON_CACHE_LIFE = beaconCacheLife;
    }

    public long getBeaconOnlineLife() {
        return BEACON_ONLINE_LIFE;
    }

    public void setBeaconOnlineLife(long beaconOnlineLife) {
        BEACON_ONLINE_LIFE = beaconOnlineLife;
    }

    public long getRegionSwiftPeriod() {
        return REGION_SWIFT_PERIOD;
    }

    public void setRegionSwiftPeriod(long regionSwiftPeriod) {
        REGION_SWIFT_PERIOD = regionSwiftPeriod;
    }

    public long getLocationPeriod() {
        return LOCATION_PERIOD;
    }

    public void setLocationPeriod(long locationPeriod) {
        LOCATION_PERIOD = locationPeriod;
    }

    public int getLocationBeaconQuantity() {
        return LOCATION_BEACON_QUANTITY;
    }

    public void setLocationBeaconQuantity(int locationBeaconQuantity) {
        LOCATION_BEACON_QUANTITY = locationBeaconQuantity;
    }

    public double getLocationWeightedCentroidAlpha() {
        return LOCATION_WEIGHTED_CENTROID_ALPHA;
    }

    public void setLocationWeightedCentroidAlpha(double locationWeightedCentroidAlpha) {
        LOCATION_WEIGHTED_CENTROID_ALPHA = locationWeightedCentroidAlpha;
    }

    public double getResultScale() {
        return RESULT_SCALE;
    }

    public void setResultScale(double resultScale) {
        RESULT_SCALE = resultScale;
    }
}
