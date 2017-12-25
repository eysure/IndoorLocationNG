package com.zjut.henry.indoorlocationng;

import android.graphics.PointF;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * 处理JSON数据
 * Created by henry on 11/15/17.
 */

class JSONProcess {

    /**
     * To tell whether a string is JSON object or JSON array
     * and process every JSON object in another process function
     *
     * @param s string
     */
    public static void process(String s) {
        try {
            Object json = new JSONTokener(s).nextValue();
            if (json instanceof JSONObject) {
                JSONObject jsonObject = (JSONObject) json;
                process(jsonObject);
            } else if (json instanceof JSONArray) {
                JSONArray jsonArray = (JSONArray) json;
                // 获得jsonArray中的每个JSONObject
                for(int i=0;i<jsonArray.length();++i){
                    if(jsonArray.get(i) instanceof JSONObject) {
                        JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                        process(jsonObject);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Process the JSON objects received from server.
     * @param jo JSON object
     */
    private static void process(JSONObject jo) {
        try {
            String type = jo.getString("object_type");
            switch (type) {
                case "beacon": {
                    if(jo.isNull("mac"))break;
                    Beacon beacon = new Beacon(jo.getString("mac"));
                    beacon.setRegionID(jo.isNull("region_id")?0:jo.getInt("region_id"));
                    beacon.setBuilding(jo.isNull("building")?null:jo.getString("building"));
                    beacon.setFloor(jo.isNull("floor")?null:jo.getString("floor"));
                    beacon.setCoordination(
                            new PointF(
                                    jo.isNull("x")?0:jo.getInt("x"),
                                    jo.isNull("y")?0:jo.getInt("y")));
                    BeaconLinkLayer.addBeaconCacheFromServer(beacon);
                    break;
                }
                default: Log.w("JSON","JSON unknown: "+ jo.toString());break;
            }
        } catch (JSONException e) {
            Log.w("JSON","Bad JSON: "+ jo.toString());
        }
    }

    private JSONProcess() {
    }
}
