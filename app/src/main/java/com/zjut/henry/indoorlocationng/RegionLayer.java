package com.zjut.henry.indoorlocationng;

import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * 地图管理层
 *
 * 此类将定位到楼层级别
 * 此类管理一个RegionPower稀疏数列, 维护当前在线Beacon中不同楼层的功率总值, 从而确定当前楼层
 * 此类管理BeaconCurrent列表, 当楼层切换时列表自动切到当前楼层的Beacon
 * Created by henry on 12/7/17.
 */
public class RegionLayer {
    private static int sRegionNow = 0;
    private static SparseArray<Double> sRegionPower = new SparseArray<>();

    /**
     * 计算当前在线Beacon中每个Region的功率和, 并返回功率最大的楼层
     * @return Region ID (无法定位则返回0)
     */
    private static int regionPowerUpdate(){
        sRegionPower.clear();
        for(Beacon beacon : BeaconLinkLayer.getBeacons()){
            if(beacon.getStatus() >= 3) {
                int regionID = beacon.getRegionID();
                if(regionID!=0) sRegionPower.put(regionID, sRegionPower.get(regionID, 0D) + beacon.getRsmw());
            }
        }

        int regionID = 0;
        double max=0;
        for(int i = 0; i< sRegionPower.size(); ++i){
            if(sRegionPower.valueAt(i)>max){
                max= sRegionPower.valueAt(i);
                regionID= sRegionPower.keyAt(i);
            }
        }
        return regionID;
    }

    /**
     * 定时器任务: 地点判断与转化
     */
    static TimerTask sRegionSwift = new TimerTask() {
        @Override
        public void run() {
            int regionID = regionPowerUpdate();
            if(regionID!=sRegionNow){
                sRegionNow = regionID;
                for(Beacon beacon : BeaconLinkLayer.getBeacons()) {
                    if(beacon.getStatus() >= 3) {
                        if(beacon.getRegionID() == regionID)beacon.setStatus(4);
                        else beacon.setStatus(3);
                    }
                }
            }
        }
    };

    /**
     * 获得当前的地点ID
     * @return Region ID (无法定位则返回0)
     */
    public static int getRegionNow() {
        return sRegionNow;
    }

    /**
     * 获得Region功率稀疏数组
     * @return Region功率稀疏数组
     */
    public static SparseArray<Double> getRegionPower() {
        return sRegionPower;
    }
}
