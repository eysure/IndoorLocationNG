package com.zjut.henry.indoorlocationng;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static java.lang.Thread.sleep;

/**
 * 服务器连接
 * Created by henry on 12/5/17.
 */

class ServerConnection implements Runnable {

    public static final int START_NETWORK = 944;
    public static final int CONNECTION_STATUS = 961;
    public static final int SEND_UTF = 967;
    public static final int SERVER_DATA = 161;
    public static final int SERVER_BYTES = 162;

    private Socket mSocket;
    private DataInputStream mDataInputStream;
    private DataOutputStream mDataOutputStream;

    private static Handler sHandler;

    @Override
    public void run() {
        Looper.prepare();
        sHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                try {
                    switch (msg.what) {
                        case START_NETWORK: {
                            String ip = msg.getData().getString("server_ip");
                            int port = msg.getData().getInt("server_port");
                            linkStart(ip, port);
                            break;
                        }
                        case SERVER_DATA: {
                            String data = msg.getData().getString("cmd");
                            resultProcess(data);
                            break;
                        }
                        case SEND_UTF: {
                            String send = msg.getData().getString("data");
                            if (send != null && send.length() > 0)
                                mDataOutputStream.writeUTF(send);
                            Log.d("O", send);
                            break;
                        }
                        case SERVER_BYTES: {
                            break;
                        }
                        default:
                            break;
                    }
                } catch (IOException e) {
                    Log.e("Network", "-x- sending failed, restart the connection...");
                    e.printStackTrace();
                    linkStart(GlobalParameter.SERVER_IP, GlobalParameter.SERVER_PORT);
                }
                super.handleMessage(msg);
            }
        };
        Looper.loop();
    }

    // Network Thread：Connect to server
    private void linkStart(String ip, int port) {
        try {
            mSocket = new Socket(ip, port);
            mDataInputStream = new DataInputStream(mSocket.getInputStream());
            mDataOutputStream = new DataOutputStream(mSocket.getOutputStream());
            Log.d("Network", "--- Connected.");
            new Thread(new pullServer()).start();   // Start the server getter thread
        } catch (Exception e) {
            Log.e("Network", "-x- Connection failed. Retry in 3s...");
            try {
                sleep(1000);
                linkStart(ip, port);
            } catch (InterruptedException I) {
                Log.e("Network", "-x- Interrupted. Exit.");
                System.exit(0);
            } catch (StackOverflowError SOF) {
                Log.e("Network", "-x- Too much retry times, Exit.");
                System.exit(0);
            }
        }
    }

    /**
     * 处理不带有字节流信息的服务器返回, 以及JSON数据
     * NTS:
     *
     * @param s UTF命令行
     */
    private void resultProcess(String s) {
        // Process JSON
        if (s.startsWith("[") || s.startsWith("{")) JSONProcess.process(s);
        else {
            // Process Command
            String[] result = s.split(",");
            switch (result[0]) {
                case "HAT": break;
                default: Log.w("IN", "Unknown command: " + s); break;
            }
        }
    }

    //--------Attention: Below are MainThread functions

    // start to connect
    public static void activateLinkStart() {
        Message message = new Message();
        message.what = START_NETWORK;
        Bundle b = new Bundle();
        b.putString("server_ip", GlobalParameter.SERVER_IP);
        b.putInt("server_port", GlobalParameter.SERVER_PORT);
        message.setData(b);
        while (sHandler == null) {
        }
        sHandler.sendMessage(message);
    }

    // send basic UTF message
    private static void sendUTF(String s) {
        Message message = new Message();
        message.what = SEND_UTF;
        Bundle b = new Bundle();
        b.putString("data", s);
        message.setData(b);
        sHandler.sendMessage(message);
    }

    static void requestBeacon(String mac) {
        sendUTF("REQA," + mac);
    }

    // 轮询服务器
    private class pullServer implements Runnable {
        @Override
        public void run() {
            while (!mSocket.isClosed() && mSocket.isConnected()) {
                try {
                    while (mDataInputStream.available() > 0) {
                        String utf = mDataInputStream.readUTF();
                        Log.d("I", utf);

                        if (utf.startsWith("BYTES/")) {
                            String[] result = utf.split("/");
                            long byteLength = Long.parseLong(result[1]);
                            // Bytes in
                            Log.d("I", "--> Bytes " + byteLength + " downloading...");

                            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                            byte[] buf = new byte[1024 * 4];
                            int len;
                            while (baos.size() < byteLength) {
                                len = mDataInputStream.read(buf);
                                baos.write(buf, 0, len);
                            }
                            mDataOutputStream.writeUTF("DLD");
                            Log.d("I", "--> Bytes downloaded!");
                            byte[] bytes = baos.toByteArray();

                            Message msg = new Message();
                            msg.what = SERVER_BYTES;
                            Bundle data = new Bundle();
                            data.putString("cmd", result[2]);
                            data.putByteArray("bytes", bytes);
                            msg.setData(data);
                            sHandler.sendMessage(msg);
                        } else {
                            Message msg = new Message();
                            msg.what = SERVER_DATA;
                            Bundle data = new Bundle();
                            data.putString("cmd", utf);
                            msg.setData(data);
                            sHandler.sendMessage(msg);
                        }
                    }
                    sleep(1);
                } catch (IOException e) {
                    Log.e("Network", "-x- Connection cut!");
                    e.printStackTrace();
                    linkStart(GlobalParameter.SERVER_IP, GlobalParameter.SERVER_PORT);
                } catch (InterruptedException i) {
                    Log.e("Network", "-x- Server Puller Interrupted!");
                }
            }
        }
    }

}

