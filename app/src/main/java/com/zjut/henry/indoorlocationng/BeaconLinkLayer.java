package com.zjut.henry.indoorlocationng;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Iterator;
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
    private static List<String> sBeaconsPending = new ArrayList<>();
    private static List<Beacon> sBeaconCache = new ArrayList<>();

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
            String mac = result.getDevice().getAddress();                                           // 从result包中得到Beacon MAC地址
            int rssi = result.getRssi();                                                            // 从result包中得到RSSI信号强度
            String status;

            // 夺命连环(#‵′)靠
            Beacon beacon = findOnlineBeaconByMac(mac);                                             // 询问在线列表是否有此地址的Beacon
            if (beacon != null) {
                status = "Online";
                beacon.setRssi(rssi);                                                               // 如果存在, 直接更新RSSI值
            } else {
                beacon = cacheGet(mac);                                                             // 如果不存在, 则询问Cache是否保存此beacon
                if (beacon != null) {
                    if (beacon.isReg()) {
                        status = "Valid";
                        beacon.setRssi(rssi);
                        beaconListsProcess(true, beacon);                                // 如果Cache中存在, 添加到在线列表
                    } else status = "Raw";
                } else {
                    status = "Pending";
                    if (!sBeaconsPending.contains(mac)) {
                        ServerConnection.requestBeacon(mac);                                        // 如果Cache也不存在(或Cache过期), 则向服务器询问
                        sBeaconsPending.add(mac);
                    }
                }
            }

            if (!status.equals("Raw")) {
                Log.d("BeaconLinkLayer", "Beacon " + mac + "\t" + rssi + "\t[ " + status + " ]");
                LocationService.updateNotification(
                        "Current: " + RegionLayer.getRegionNow(),
                        "[" + status + "] " + mac + "\t" + rssi);     // 更新通知栏
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
     * 过期在线Beacon清理
     * 在线Beacon列表需要维护, 当超过规定时间(BEACON_ONLINE_LIFE)都没有更新过RSSI值的Beacon, 移出列表
     * 此任务在BluetoothProcess构造时即被执行
     * 注意: 在遍历一个列表的时候不能执行remove操作, 会报错, 必须在遍历完成之后"清空回收站"
     */
    public static TimerTask sScavenger = new TimerTask() {
        @Override
        public void run() {
            try {
                Log.i("BeaconLinkLayer", "Scavenger called.");
                List<Beacon> recycleBin = new ArrayList<>();
                Date now = new Date();
                for (Beacon beacon : sBeaconsOnline) {
                    if (now.getTime() - beacon.getOnlineDate().getTime() > GlobalParameter.BEACON_ONLINE_LIFE) {
                        recycleBin.add(beacon);
                    }
                }
                if (recycleBin.size() > 0) {
                    for (Beacon beacon : recycleBin) beaconListsProcess(false, beacon);
                    Log.i("BeaconLinkLayer", "Removed: " + Arrays.toString(recycleBin.toArray()));
                }
                for (String mac : sBeaconsPending) ServerConnection.requestBeacon(mac);
            } catch (ConcurrentModificationException cme) {
                Log.e("BeaconLinkLayer", "Scavenger Concurrent Modification Exception.");
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
     * 获得网络阻塞Beacon列表
     *
     * @return 当前网络阻塞Beacon列表
     */
    public static List<String> getBeaconsPending() {
        return sBeaconsPending;
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
    public static void beaconListsProcess(boolean addOrDelete, Beacon beacon) {
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
    public static void addBeaconCacheFromServer(Beacon beacon) {
        boolean flag = false;
        for (String mac : sBeaconsPending) {
            if (mac.equals(beacon.getMac())) {
                Log.i("BeaconLinkLayer", "Server return. " + mac + " Region: " + beacon.getBuilding() + ":" + beacon.getFloor() + "\t" + beacon.getCoordination());
                flag = true;
                break;
            }
        }
        if (flag) sBeaconsPending.remove(beacon.getMac());

        // update Cache
        beacon.setCacheDate(new Date());
        Beacon pointer = null;
        for (Beacon b : sBeaconCache) {
            if (b.getMac().equals(beacon.getMac())) {
                pointer = b;
                break;
            }
        }
        if (pointer != null) {
            sBeaconCache.remove(pointer);
        }
        sBeaconCache.add(beacon);
        Log.d("BeaconCache", "After:\t" + beacon.getRegionID() + ":" + beacon.getCoordination());
    }

    /**
     * 由MAC地址得到对应的Beacon
     *
     * @param mac 给定MAC地址
     * @return 对应的Beacon 若没有找到则返回null
     */
    private static Beacon cacheGet(String mac) {
        try {
            Iterator<Beacon> it = sBeaconCache.iterator();
            while (it.hasNext()) {
                Beacon beacon = it.next();
                if (isCacheExpired(beacon)) {
                    Log.d("BeaconCache", beacon.getMac() + " is Expired.");
                    it.remove();
                    if (beacon.getMac().equals(mac)) return null;
                } else if (beacon.getMac().equals(mac)) return beacon;
            }
            return null;
        } catch (ConcurrentModificationException cme) {
            Log.e("BeaconCache", "Cache get Concurrent Modification Exception.");
            return null;
        }
    }

    /**
     * 判断Cache中的某个Beacon是否过期
     *
     * @param beacon 给定Beacon
     * @return true - 已过期 / false - 有效
     */
    private static boolean isCacheExpired(Beacon beacon) {
        return beacon.getCacheDate() == null ||
                (double) ((new Date()).getTime() - beacon.getCacheDate().getTime()) > GlobalParameter.BEACON_CACHE_LIFE;
    }

    /**
     * 打印BeaconCache列表
     *
     * @return BeaconCache列表
     */
    public static String cacheToString() {
        StringBuilder sb = new StringBuilder();
        for (Beacon beacon : sBeaconCache)
            sb.append(beacon.getMac()).append(" ").append(beacon.getCacheDate().toLocaleString()).append("\n");
        return sb.toString();
    }

}