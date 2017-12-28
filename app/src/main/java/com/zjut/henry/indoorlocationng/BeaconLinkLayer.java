package com.zjut.henry.indoorlocationng;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

/**
 * 基站数据链路层
 * <p>
 * 负责解析蓝牙传来的报文, 维护在线Beacon表
 * 负责联系服务器获取Beacon注册信息, 维护BeaconCache表
 * Created by henry on 12/6/17.
 */

public class BeaconLinkLayer {

    private static List<Beacon> sBeaconsOnline = new ArrayList<>();
    private static List<Beacon> sBeaconCache = new ArrayList<>();

    private static List<Beacon> sRecycleBin = new ArrayList<>();
    /**
     * BLE扫描回调
     */
    static ScanCallback sScanCallback = new ScanCallback() {

        /**
         * 接收到BLE广播信号, 解析并更新Beacon的信号强度(RSSI)
         * @param callbackType 回调类型
         * @param result 结果
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            LocationController.updateBeaconResult(result);                                          // 先发送给回调接口
            String mac = result.getDevice().getAddress();                                           // 从result包中得到Beacon MAC地址
            int rssi = result.getRssi();                                                            // 从result包中得到RSSI信号强度

            // 夺命连环(#‵′)靠
            Beacon beacon = findOnlineBeaconByMac(mac);                                             // 询问在线列表是否有此地址的Beacon
            if (beacon != null) beacon.setRssi(rssi);                                                               // 如果存在, 直接更新RSSI值
            else {
                beacon = cacheGet(mac);                                                             // 如果不存在, 则询问Cache是否保存此beacon
                if (beacon != null) {
                    if (beacon.isReg()) {
                        beacon.setRssi(rssi);
                        beaconListsProcess(true, beacon);                                // 如果Cache中存在, 添加到在线列表
                    }
                } else ServerConnection.requestBeacon(mac);                                        // 如果Cache也不存在(或Cache过期), 则向服务器询问
            }
        }

        /**
         * 蓝牙BLE扫描出错
         * @param errorCode 错误代码
         */
        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.e("BeaconLinkLayer", "\tScan Failed. Already started.");
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.e("BeaconLinkLayer", "\tScan Failed. Application registration failed.");
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    Log.e("BeaconLinkLayer", "\tScan Failed. Internal error.");
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.e("BeaconLinkLayer", "\tScan Failed. Feature unsupported.");
                    break;
                default:
                    super.onScanFailed(errorCode);
                    break;
            }
        }
    };

    /**
     * 根据MAC地址获得在线Beacon
     * O(n)
     *
     * @param mac MAC地址
     * @return Beacon / null - 不在线
     */
    private static Beacon findOnlineBeaconByMac(String mac) {
        for (Beacon beacon : sBeaconsOnline) {
            if (beacon.getMac().equals(mac)) return beacon;
        }
        return null;
    }

    /**
     * 过期在线Beacon清理, 每秒被执行
     * 在线Beacon列表需要维护, 当超过规定时间(BEACON_ONLINE_LIFE)都没有更新过RSSI值的Beacon, 移出列表
     * 此任务在BluetoothProcess构造时即被执行
     * 注意: 在遍历一个列表的时候不能执行remove操作, 会报错, 必须在遍历完成之后"清空回收站"
     */
    static TimerTask sScavenger = new TimerTask() {
        @Override
        public void run() {
            try {
                Date now = new Date();

                for (Beacon beacon : sBeaconsOnline) {
                    if (now.getTime() - beacon.getOnlineDate().getTime() > GlobalParameter.BEACON_ONLINE_LIFE) {
                        sRecycleBin.add(beacon);
                    }
                }
                if (sRecycleBin.size() > 0) {
                    for (Beacon beacon : sRecycleBin) beaconListsProcess(false, beacon);
                    Log.d("BeaconLinkLayer", "Offline: " + Arrays.toString(sRecycleBin.toArray()));
                }
                sRecycleBin.clear();
                for (Beacon beacon : sBeaconCache) {
                    if (now.getTime() - beacon.getCacheDate().getTime() > GlobalParameter.BEACON_CACHE_LIFE) {
                        sRecycleBin.add(beacon);
                    }
                }
                if (sRecycleBin.size() > 0) {
                    sBeaconCache.removeAll(sRecycleBin);
                    Log.d("BeaconLinkLayer", "Cache expired: " + Arrays.toString(sRecycleBin.toArray()));
                }
                sRecycleBin.clear();

                // for (String mac : sBeaconsPending) ServerConnection.requestBeacon(mac);
            } catch (ConcurrentModificationException cme) {
                Log.e("Scavenger", "CME");
            }
        }
    };

    /**
     * 获得所有在线Beacon列表
     *
     * @return 当前在线Beacon列表
     */
    public static List<Beacon> getBeaconsOnline() {
        return sBeaconsOnline;
    }

    /**
     * 获得BeaconCache列表
     *
     * @return 当前BeaconCache列表
     */
    public static List<Beacon> getBeaconsCache() {
        return sBeaconCache;
    }

    /**
     * 操作Beacon列表, 包括Online表与Current表
     *
     * @param addOrDelete true - 增加 / false - 删除
     * @param beacon      待操作的Beacon
     */
    private static void beaconListsProcess(boolean addOrDelete, Beacon beacon) {
        // Online List
        if (addOrDelete) sBeaconsOnline.add(beacon);
        else sBeaconsOnline.remove(beacon);

        // Current List
        if (!RegionLayer.sLock && beacon.getRegionID() == RegionLayer.getRegionNow()) {
            if (addOrDelete) RegionLayer.getBeaconsCurrent().add(beacon);
            else RegionLayer.getBeaconsCurrent().remove(beacon);
        }
    }

    /**
     * 从Server返回的Beacon, 取消Pending状态, 并添加到Cache
     *
     * @param beacon 返回的Beacon
     */
    static void addBeaconCacheFromServer(Beacon beacon) {
        try {
            Beacon pointer = null;
            for(Beacon b : sBeaconCache){
                if (b.getMac().equals(beacon.getMac()))pointer = b;
            }
            if(pointer!=null) sBeaconCache.remove(pointer);
            beacon.setCacheDate(new Date());
            sBeaconCache.add(beacon);
            Log.d("BeaconCache", "Add:\t"+ beacon.getMac() + beacon.getRegionID() + ":" + beacon.getCoordination());
        }catch (ConcurrentModificationException cme) {
            Log.e("BeaconCache", "CME");
            addBeaconCacheFromServer(beacon);
        }
    }

    /**
     * 由MAC地址得到对应的Beacon
     *
     * @param mac 给定MAC地址
     * @return 对应的Beacon 若没有找到则返回null
     */
    private static Beacon cacheGet(String mac) {
        for (Beacon beacon : sBeaconCache) {
            if(mac.equals(beacon.getMac()))return beacon;
        }
        return null;
    }

}