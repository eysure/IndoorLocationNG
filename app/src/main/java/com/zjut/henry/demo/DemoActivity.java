package com.zjut.henry.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.tbruyelle.rxpermissions2.RxPermissions;

import com.zjut.henry.indoorlocationng.Beacon;
import com.zjut.henry.indoorlocationng.BeaconLinkLayer;
import com.zjut.henry.indoorlocationng.LocationController;
import com.zjut.henry.indoorlocationng.LocationLayer;
import com.zjut.henry.indoorlocationng.LocationResult;
import com.zjut.henry.indoorlocationng.R;
import com.zjut.henry.indoorlocationng.RegionLayer;

import io.reactivex.functions.Consumer;

/**
 * DemoActivity
 * 演示Demo
 * <p>
 * Created by henry on 12/6/17.
 */
public class DemoActivity extends Activity {

    /**
     * 演示流程:
     * <p>
     * (初始化控制台)
     * 获取用户权限
     * 实现监听器方法
     * 启动LocationController
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView(this);
        requestPermission();

        final LocationController locationController = new LocationController(this);
        locationController.setOnLocationUpdateListener(new LocationController.OnLocationUpdateListener() {
            @Override
            public void onLocationUpdate(LocationResult locationResult) {
                // 位置更新回调处理
                locationResult.setUserInfo("0200000000000");
                locationResult.setInfo("FromZJUT");
                Log.i("Location",locationResult.getJSONObject().toString());
            }
        });
        locationController.setOnBeaconUpdateListener(new LocationController.OnBeaconUpdateListener() {
            @Override
            public void onBeaconUpdate(ScanResult scanResult) {
                // Beacon回调处理
                // println(scanResult.getDevice().getAddress()+"\t"+scanResult.getDevice().getName()+"\t"+scanResult.getRssi());
            }
        });
        locationController.start();
    }

    /**
     * 获取用户权限
     */
    private void requestPermission() {
        RxPermissions rx = new RxPermissions(this);
        rx.request(
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WAKE_LOCK)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) throws Exception {
                        if (granted) {
                            println("Permission granted.");
                        } else {
                            println("Permission failed.");
                            Toast.makeText(getApplicationContext(), "请允许本软件所需的权限, 并再次运行", Toast.LENGTH_LONG).show();
                            Intent localIntent = new Intent("miui.intent.action.APP_PERM_EDITOR");
                            localIntent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
                            localIntent.putExtra("extra_pkgname", getPackageName());
                            startActivity(localIntent);
                        }
                    }
                });
    }

    // ---------------- 操作台 ------------------

    private TextView mLog;
    private EditText mCmdInput;

    /**
     * 初始化操作台
     *
     * @param activity 操作台被调用的Activity
     */
    private void initView(Activity activity) {
        activity.setContentView(R.layout.console);
        mLog = activity.findViewById(R.id.log);
        mLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        mCmdInput = activity.findViewById(R.id.cmd_input);
        Button sendButton = activity.findViewById(R.id.cmd_send_button);
        mCmdInput.setImeActionLabel("Send", KeyEvent.KEYCODE_ENTER);
        mCmdInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN && send();
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });
        mLog.append("Welcome.\n");
    }

    /**
     * 发送信息按钮
     *
     * @return true - 发送成功 / false - 发送失败
     */
    private boolean send() {
        if (mCmdInput.getText() != null) {
            commandProcess(mCmdInput.getText().toString());
            mCmdInput.setText("");
            return true;
        } else return false;
    }

    /**
     * 打印在控制台上并换行
     *
     * @param args 字符串
     */
    public void println(String... args) {
        for (String s : args) mLog.append(s);
        mLog.append("\n");
        scrollToBottom();
    }

    /**
     * 打印在控制台上
     *
     * @param s 字符串
     */
    public void print(String s) {
        mLog.append(s);
    }

    /**
     * 以特定颜色打印在控制台上
     *
     * @param s     字符串
     * @param color 颜色字符串
     */
    public void print(String s, String color) {
        mLog.append(Html.fromHtml("<font color='" + color + "'>" + s + "</font>"));
    }

    /**
     * 滚动条拉到底
     */
    private void scrollToBottom() {
        int offset = mLog.getLineCount() * mLog.getLineHeight();
        if (offset > (mLog.getHeight() - mLog.getLineHeight() - 20)) {
            mLog.scrollTo(0, offset - mLog.getHeight() + mLog.getLineHeight() + 20);
        }
    }

    /**
     * 处理控制台命令
     *
     * @param cmd 命令
     */
    private void commandProcess(String cmd) {
        String[] args = cmd.split(" ");

        print("#", "#FDD835");
        for (String arg : args) {
            print(" ");
            print(arg, "#FDD835");
        }
        println();

        try {
            switch (args[0]) {
                // Show beacons
                case "show": {
                    switch (args[1]) {
                        case "active":
                            for (Beacon beacon : LocationLayer.getBeaconsActive())
                                println(beacon.toString());
                            break;
                        case "current":
                            for (Beacon beacon : RegionLayer.getBeaconsCurrent())
                                println(beacon.toString());
                            break;
                        case "online":
                            for (Beacon beacon : BeaconLinkLayer.getBeaconsOnline())
                                println(beacon.toString());
                            break;
                        case "pending":
                            for (String mac : BeaconLinkLayer.getBeaconsPending()) println(mac);
                            break;
                        case "cache":
                            for (Beacon beacon : BeaconLinkLayer.getBeaconsCache())
                                println(beacon.toString());
                            break;
                        case "power":
                            println(RegionLayer.getRegionPower().toString());
                            break;
                        default:
                            println("show what?");
                            break;
                    }
                    break;
                }
                // clear the screen
                case "clr":
                case "clc":
                case "clear":
                    mLog.setText("");
                    break;
                // control console display
                case "display": {
                    if (args[1].matches("[0-9]+")) mLog.setTextSize(Integer.valueOf(args[1]));
                    else switch (args[1]) {
                        case "+":
                            mLog.setTextSize(mLog.getTextSize() + 2);
                            break;
                        case "-":
                            mLog.setTextSize(mLog.getTextSize() - 2);
                            break;
                        default:
                            println("display should be +/- or size.");
                            break;
                    }
                    break;
                }
                case "map":
                    mapInitial();
                    break;
                default:
                    println("unknown command " + args[0]);
                    break;
            }
        } catch (Exception e) {
            print(e.getLocalizedMessage(), "#EF5350");
        }
    }

    // -------------- 地图调试与演示 --------------

    private static final int REQUEST_PHOTO = 849;

    /**
     * 地图初始化
     * 选择照片
     */
    private void mapInitial() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, REQUEST_PHOTO);
        Toast.makeText(this, "选择 Region #" + RegionLayer.getRegionNow() + "的地图", Toast.LENGTH_SHORT).show();
    }

    /**
     * 选择照片成功后返回
     *
     * @param requestCode REQUEST码
     * @param resultCode  RESULT_OK 或 RESULT_CANCELED
     * @param data        图片Uri
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_PHOTO: {
                if (resultCode == RESULT_OK && data.getData() != null) {
                    MapView map = (MapView) View.inflate(this, R.layout.map, null);
                    map.setImage(ImageSource.uri(data.getData()));
                    new AlertDialog.Builder(this)
                            .setView(map)
                            .create().show();
                }
            }
        }
    }
}
