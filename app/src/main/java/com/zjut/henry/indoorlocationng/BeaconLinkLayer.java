package com.zjut.henry.indoorlocationng;

import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.graphics.PointF;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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
    private static List<Beacon> sBeacons = new ArrayList<>();
    
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
            for(Beacon beacon : sBeacons) {
                if(beacon.getMac().equals(mac)) {
                    beacon.setRssi(rssi);
                    switch (beacon.getStatus()) {
                        //   0 - Expired 失效, 未向服务器发送Beacon请求, 或Cache已过期, 或任何种类的已失效
                        case 0:{
                            ServerConnection.requestBeacon(mac);
                            beacon.setStatus(1);
                            return;
                        }
                        //   1 - Pending 挂起, 已向服务器发送Beacon请求, 服务器尚未返回
                        case 1:return;
                        //   2 - Cache 缓存, 服务器已返回注册信息, 且Cache尚未过期
                        case 2:{
                            if(RegionLayer.getRegionNow()==beacon.getRegionID())beacon.setStatus(4);
                            else beacon.setStatus(3);
                            return;
                        }
                        //   3 - Online 在线, 当前在线Beacon
                        case 3:if(RegionLayer.getRegionNow()==beacon.getRegionID())beacon.setStatus(4);return;
                        //   4 - Current 当前, 当前同层Beacon
                        case 4:if(RegionLayer.getRegionNow()!=beacon.getRegionID())beacon.setStatus(3);return;
                        // 未知
                        default:return;
                    }
                }
            }
            
            // 新Beacon
            try {
                Beacon beacon = new Beacon(mac);
                sBeacons.add(beacon);
                beacon.setRssi(rssi);
                beacon.setStatus(0);
                ServerConnection.requestBeacon(mac);
                beacon.setStatus(1);
            }catch (ConcurrentModificationException cme) {
                Log.w("BeaconLinkLayer","CME");
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
                
                // Status 版本
                for(Beacon beacon : sBeacons) {
                    switch (beacon.getStatus()) {
                        //   2 - Cache 缓存检测是否过期
                        case 2: if (now.getTime() - beacon.getCacheDate().getTime() > GlobalParameter.BEACON_CACHE_LIFE) beacon.setStatus(0);break;
                        //   3/4/5 - 对于在线及以上的Beacon检测在线时间是否过期
                        case 3:
                        case 4:
                        case 5: {
                            if (now.getTime() - beacon.getCacheDate().getTime() > GlobalParameter.BEACON_CACHE_LIFE) beacon.setStatus(0);
                            else if (now.getTime() - beacon.getOnlineDate().getTime() > GlobalParameter.BEACON_ONLINE_LIFE) beacon.setStatus(2);
                            break;
                        }
                        //   0/1 - 不变
                        default:break;
                    }
                }
            } catch (ConcurrentModificationException cme) {
                Log.e("Scavenger", "CME");
            }
        }
    };

    /**
     * 服务器返回Beacon注册信息更新
     * Status 版本
     * @param beaconJSON 服务器返回JSON格式的Beacon
     */
    static void beaconRegUpdate(JSONObject beaconJSON) {
        try {
            String mac = beaconJSON.getString("mac");
            for(Beacon beacon : sBeacons) {
                if (beacon.getMac().equals(mac)) {
                    beacon.setRegionID(beaconJSON.isNull("region_id") ? 0 : beaconJSON.getInt("region_id"));
                    beacon.setBuilding(beaconJSON.isNull("building") ? null : beaconJSON.getString("building"));
                    beacon.setFloor(beaconJSON.isNull("floor") ? null : beaconJSON.getString("floor"));
                    beacon.setCoordination(
                            new PointF(
                                    beaconJSON.isNull("x") ? 0 : beaconJSON.getInt("x"),
                                    beaconJSON.isNull("y") ? 0 : beaconJSON.getInt("y")));
                    beacon.setStatus(2);
                    beacon.setCacheDate(new Date());
                    return;
                }
            }
        }catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * 获得Beacon列表
     * @return beacon列表
     */
    public static List<Beacon> getBeacons() {
        return sBeacons;
    }
}