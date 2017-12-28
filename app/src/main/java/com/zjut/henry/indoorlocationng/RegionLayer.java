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
    private static List<Beacon> sBeaconsCurrent = new ArrayList<>();
    static boolean sLock = false;

    /**
     * 选择功率最大的楼层
     * @return Region ID (无法定位则返回0)
     */
    private static int selectMaxPowerRegion(){
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
     * 计算当前在线Beacon中每个Region的功率和
     */
    public static void regionPowerUpdate(){
        sRegionPower.clear();
        for(Beacon b : BeaconLinkLayer.getBeaconsOnline()){
            int regionID = b.getRegionID();
            sRegionPower.put(regionID, sRegionPower.get(regionID,0D)+b.getRsmw());
        }
    }

    /**
     * 定时器任务: 地点判断与转化
     */
    static TimerTask sRegionSwift = new TimerTask() {
        @Override
        public void run() {
            regionPowerUpdate();
            int regionID = selectMaxPowerRegion();
            if(regionID!=sRegionNow){
                sLock = true;
                sBeaconsCurrent.clear();
                for(Beacon beacon : BeaconLinkLayer.getBeaconsOnline()) {
                    if (beacon.getRegionID() == regionID) sBeaconsCurrent.add(beacon);
                }
                sRegionNow=regionID;
                sLock = false;
            }
            Log.i("RegionLayer","Power: "+sRegionPower.toString()+"\t"+"RegionNow: "+sRegionNow);
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
     * 获得BeaconsCurrent列表
     * @return BeaconsCurrent列表
     */
    public static List<Beacon> getBeaconsCurrent() {
        return sBeaconsCurrent;
    }

    /**
     * 获得Region功率稀疏数组
     * @return Region功率稀疏数组
     */
    public static SparseArray<Double> getRegionPower() {
        return sRegionPower;
    }
}
