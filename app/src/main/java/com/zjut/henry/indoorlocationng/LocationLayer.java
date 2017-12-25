package com.zjut.henry.indoorlocationng;

import android.graphics.PointF;

import java.util.Collections;
import java.util.List;
import java.util.TimerTask;

/**
 * 定位算法层
 *
 * 本层负责将当前Region的在线Beacon(BeaconCurrent)取出RSSI值最大的若干个, 进行定位
 * 本层继承了定时器任务, 独立线程执行
 * Created by henry on 12/8/17.
 */

public class LocationLayer extends TimerTask{
    private static List<Beacon> sBeaconsActive = null;
    private static PointF sLocation = new PointF(0,0);
    private static Router sRouter = new Router(sLocation);

    /**
     * 定时器任务: 定位
     */
    @Override
    public void run() {
        if(RegionLayer.getRegionNow()!=0){
            updateBeaconActive();
            if(sBeaconsActive.size()>0)weightedCentroid(sBeaconsActive,sLocation);
        }
        else sRouter.clear();

        // 新位置产生, 发送到回调
        LocationResult result = new LocationResult();
        result.setCoordination(sRouter.get(),GlobalParameter.RESULT_SCALE);
        result.setNearestDevice((sBeaconsActive!=null && sBeaconsActive.size()>0)?sBeaconsActive.get(0).getMac():null);
        result.setAngle(StepNav.getCompassOrientation()<0?(int)StepNav.getCompassOrientation()+360:(int)StepNav.getCompassOrientation());
        LocationController.updateLocationResult(result);
    }

    /**
     * 将当前Region的在线Beacon(BeaconCurrent)取出RSSI值最大的若干个, 作为新表BeaconActive
     */
    private static void updateBeaconActive(){
        List<Beacon> beaconCurrent = RegionLayer.getBeaconsCurrent();
        Collections.sort(beaconCurrent, new BeaconRssiComparator());
        int quantity = Math.min(beaconCurrent.size(),GlobalParameter.LOCATION_BEACON_QUANTITY);
        sBeaconsActive = beaconCurrent.subList(0,quantity);
    }

    /**
     * Weighted Centroid Algorithm - 内存节约版
     *
     * Formula:
     *
     *         (P1)^(1/alpha) * X1 + ... + (PN)^(1/alpha) * XN   (P1)^(1/alpha) * Y1 + ... + (PN)^(1/alpha) * YN
     * (x,y) = ----------------------------------------------- , -----------------------------------------------
     *              (P1)^(1/alpha) + ... + (PN)^(1/alpha)              (P1)^(1/alpha) + ... + (PN)^(1/alpha)
     *
     *   Px - power of beacon-x, mW
     *   Xx - x-coordination of beacon-x, cm
     *   Yx - y-coordination of beacon-x, cm
     *
     * @param beacons beacon list
     */
    private static void weightedCentroid(List<Beacon> beacons, PointF location){
        double sumSqrt =0,sumX =0,sumY =0;
        for(Beacon beacon : beacons){
            double sqrt = Math.pow(beacon.getRsmw(),1/GlobalParameter.LOCATION_WEIGHTED_CENTROID_ALPHA);
            sumSqrt += sqrt;
            sumX += sqrt * beacon.getCoordination().x;
            sumY += sqrt * beacon.getCoordination().y;
        }
        if(sumSqrt==0 && sumX==0 && sumY==0) location.set(0,0);
        else location.set((float)(sumX/sumSqrt),(float)(sumY/sumSqrt));
    }

    /**
     * 获得当前进行定位的节点
     * @return BeaconsActive
     */
    public static List<Beacon> getBeaconsActive() {
        return sBeaconsActive;
    }

    /**
     * 获取平滑器位置
     * @return 平滑器当前位置
     */
    public static PointF getLocation() {
        return sRouter.get();
    }

    /**
     * 获取原始定位结果
     * @return 原始定位结果
     */
    public static PointF getLocationRaw() {
        return sLocation;
    }
}
