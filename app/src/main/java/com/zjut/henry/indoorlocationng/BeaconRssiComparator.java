package com.zjut.henry.indoorlocationng;


import java.util.Comparator;

/**
 * Comparator for comparing Beacons by their RSSI. (FROM MAX to MIN)
 * 比较两个Beacon的RSSI值大小, 主要用于Collection.sort(...)
 * Created by Henry on 17/06/15.
 */
class BeaconRssiComparator implements Comparator<Beacon> {
    @Override
    public int compare(Beacon beacon1, Beacon beacon2) {
        double diff = beacon1.getRssi() - beacon2.getRssi();
        if (diff == 0f) return 0;
        else {
            if (diff > 0f) return -1;
            else return 1;
        }
    }
}