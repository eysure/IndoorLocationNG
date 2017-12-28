package com.zjut.henry.indoorlocationng;

import android.graphics.PointF;

import com.zjut.henry.indoorlocationng.EyTool.*;

import java.util.Date;

/**
 * 精简的Beacon类
 * 仅提供MAC地址, 所属地点id, 楼层name
 * Created by henry on 12/6/17.
 */

public class Beacon {
    private String mac;

    private int regionID;
    private String building,floor;
    private PointF coordination;

    private EyFilter rssi = new EyFilter(4);
    private Date onlineDate;
    private Date cacheDate;

    public Beacon(String mac) {
        this.mac = mac;
        onlineDate = new Date();
    }

    public String getMac() {
        return mac;
    }

    public int getRegionID() {
        return regionID;
    }

    public void setRegionID(int regionID) {
        this.regionID = regionID;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public PointF getCoordination() {
        return coordination;
    }

    public void setCoordination(PointF coordination) {
        this.coordination = coordination;
    }

    public void setCoordination(float x, float y){
        this.coordination = new PointF(x,y);
    }

    public double getRssi() {
        return rssi.get();
    }

    public void setRssi(double rssi) {
        this.rssi.f(rssi);
        onlineDate = new Date();
    }

    public Date getCacheDate() {
        return cacheDate;
    }

    public Date getOnlineDate() {
        return onlineDate;
    }

    public void setCacheDate(Date cacheDate) {
        this.cacheDate = cacheDate;
    }

    /**
     * 返回此Beacon是否经过注册
     * @return true - 已注册 / false - 未注册(注册信息为空)
     */
    public boolean isReg(){
        return regionID!=0
                && coordination!=null
                && coordination.x!=0
                && coordination.y!=0;
    }

    /**
     * 生成Beacon字符串
     * @return Beacon字符串
     */
    public String toString(){
        return (mac != null && !"".equals(mac) ? mac : "null") + "\t" +
                (building != null && !"".equals(building) ? building : "null") + ":" +
                (floor != null && !"".equals(floor) ? floor : "null") + "\t" +
                (coordination != null ? "("+coordination.x+","+coordination.y+")" : "N/A") + "\t" +
                (rssi!=null?rssi:"N/A");
    }

    /**
     * 将RSSI(dBm)转化为功率(mW)
     * @return 接收功率(mW)
     */
    public double getRsmw() {
        return Math.pow(10,getRssi()/10D);
    }

    public void copy(Beacon beacon) {
        this.mac = beacon.getMac();
        this.regionID = beacon.getRegionID();
        this.building = beacon.getBuilding();
        this.floor = beacon.getFloor();
        this.coordination = new PointF();
        this.coordination.set(beacon.getCoordination());

    }
}
