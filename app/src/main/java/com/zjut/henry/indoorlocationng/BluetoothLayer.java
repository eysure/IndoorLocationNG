package com.zjut.henry.indoorlocationng;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.widget.Toast;

import java.util.Timer;

/**
 * 蓝牙硬件层
 *
 * 此类用于监测,配置,控制蓝牙适配器
 * 保证源源不断地传入蓝牙BLE信号
 */
class BluetoothLayer {
    private static BluetoothAdapter sBluetoothAdapter;

    /**
     * 蓝牙状态广播，用于在蓝牙关闭的时候提醒用户开启，并重新开始扫描BLE设备
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 1);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:{
                    Toast.makeText(context, R.string.bluetooth_off_warning, Toast.LENGTH_SHORT).show();
                    sBluetoothAdapter.enable();
                }break;
                case BluetoothAdapter.STATE_ON:{
                    startScan();
                }break;
                default:break;
            }
        }
    };

    /**
     * 构造
     * @param context 环境
     */
    BluetoothLayer(Context context) {
        sBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        new Timer()
                .schedule(BeaconLinkLayer.sScavenger,GlobalParameter.BEACON_ONLINE_LIFE,GlobalParameter.BEACON_ONLINE_LIFE);   // 启动BeaconLinkLayer的Beacon过期清理任务
        new Timer()
                .schedule(RegionLayer.sRegionSwift,GlobalParameter.REGION_SWIFT_PERIOD,GlobalParameter.REGION_SWIFT_PERIOD);   // 启动地点切换器扫描任务
        new Timer()
                .schedule(new LocationLayer(),GlobalParameter.LOCATION_PERIOD,GlobalParameter.LOCATION_PERIOD);   // 启动定位任务
        if(isBluetoothLeSupport(context)) {
            context.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            if(sBluetoothAdapter.isEnabled())startScan();
            else sBluetoothAdapter.enable();
        }
    }

    /**
     * 检测蓝牙硬件支持
     * @return 是否支持蓝牙4.0 BLE功能
     */
    private static boolean isBluetoothLeSupport(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * 开始蓝牙BLE扫描
     * 设置BLE扫描模式为低延时
     * 当扫描到信号时调用BeaconLinkLayer层的sScanCallback处理结果
     */
    private void startScan() {
        BluetoothLeScanner bluetoothLeScanner = sBluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(
                null,
                new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                BeaconLinkLayer.sScanCallback);
    }

    /**
     * 用以Activity注销广播服务时使用
     * @return 蓝牙广播接收器
     */
    public BroadcastReceiver getReceiver() {
        return mReceiver;
    }

    /**
     * 获得蓝牙适配器
     * @return 蓝牙适配器
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return sBluetoothAdapter;
    }

}