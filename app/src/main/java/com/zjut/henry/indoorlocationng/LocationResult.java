package com.zjut.henry.indoorlocationng;

import android.graphics.PointF;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 定位结果
 * 主要是一个定位JSON对象
 * <p>
 * Created by henry on 12/14/17.
 */

public class LocationResult {
    private static SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private JSONObject json;

    private String time;
    private double x;
    private double y;
    private int buildID;
    private int floorID;
    private int angle;
    private String nearestDeviceID;
    private String userID;
    private int errorCode;
    private String info;

    LocationResult() {
        try {
            json = new JSONObject();
            time = sSimpleDateFormat.format(new Date());
            errorCode = 0;
            json.put("time", time);
            json.put("errorCode", errorCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 填入地点信息
     *
     * @param buildID 建筑ID
     * @param floorID 楼层ID
     */
    public void setRegion(int buildID, int floorID) {
        try {
            this.buildID = buildID;
            this.floorID = floorID;
            json.put("buildID", this.buildID);
            json.put("floorID", this.floorID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 填入坐标信息
     *
     * @param coordination 坐标PointF
     * @param scale        结果专用比例尺
     */
    void setCoordination(PointF coordination, double scale) {
        try {
            x = coordination.x / scale;
            y = coordination.y / scale;
            json.put("x", x);
            json.put("y", y);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void setAngle(int angle) {
        try {
            this.angle = angle;
            json.put("angle", this.angle);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void setNearestDevice(String nearestDeviceID) {
        try {
            this.nearestDeviceID = nearestDeviceID;
            json.put("nearestDeviceID", this.nearestDeviceID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 填入用户信息
     *
     * @param userID 用户ID
     */
    public void setUserInfo(String userID) {
        try {
            this.userID = userID;
            json.put("userID", this.userID);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 填入额外信息
     *
     * @param info 信息
     */
    public void setInfo(String info) {
        try {
            this.info = info;
            json.put("info", this.info);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得JSON对象
     *
     * @return 定位结果JSON对象
     */
    public JSONObject getJSONObject() {
        return json;
    }

    public String getTime() {
        return time;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getBuildID() {
        return buildID;
    }

    public int getFloorID() {
        return floorID;
    }

    public int getAngle() {
        return angle;
    }

    public String getNearestDeviceID() {
        return nearestDeviceID;
    }

    public String getUserID() {
        return userID;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getInfo() {
        return info;
    }
}