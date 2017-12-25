package com.zjut.henry.indoorlocationng;

import android.graphics.PointF;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 平滑步行寻路器
 * Created by henry on 11/21/17.
 */

class Router {
    public static double A = 0.5;                                               // 加速度(正)
    public static double VMax = 1.2;                                            // 速度最大值
    public static double VMin = 0.3;                                            // 初速度(速度最小值)
    public static double S0 = 100 * (Math.pow(VMax,2)-Math.pow(VMin,2))/(2*A);  // 最高速度降到最低速度的面积

    // 移动速度(m/s)
    private double V = 0;       // 瞬时速度
    // private double D = 0;       // 瞬时距离
    private double S = 0;       // 降到最低速度的距离

    // 点们
    private PointF mNowPoint = new PointF(0,0);
    private PointF mTargetPoint;

    // 刷新频率(秒)
    private double deltaT = 0.02D;

    /**
     * 制造一个新的平滑步行寻路器
     */
    public Router(PointF bindTarget) {
        new Timer().schedule(new RouterTimeTask(), 0, (long)(deltaT *1000));
        mTargetPoint = bindTarget;
    }

    /**
     * 平滑器线程
     */
    private class RouterTimeTask extends TimerTask {
        @Override
        public void run() {
            if (mNowPoint.equals(0,0) && !mTargetPoint.equals(0,0)){
                mNowPoint.set(mTargetPoint);
                V = VMin;
            }
            else if (!mNowPoint.equals(0,0) && !mTargetPoint.equals(0,0)) {
                double Dx = mTargetPoint.x - mNowPoint.x;
                double Dy = mTargetPoint.y - mNowPoint.y;
                double D = Math.hypot(Dx,Dy);

                // Velocity change
                S = 100 * (Math.pow(V,2)-Math.pow(VMin,2))/(2*A);
                if(V<VMax){
                    if(S<D)V+=A*deltaT;
                    else V-=A*deltaT;
                }
                else{
                    if(S0<D)V=VMax;
                    else V-=A*deltaT;
                }

                // Move
                double unitMove = V * deltaT * 100;
                if(D>unitMove)mNowPoint.offset((float)(unitMove*(Dx/D)),(float)(unitMove*(Dy/D)));
                else {
                    mNowPoint.set(mTargetPoint);
                    V = VMin;
                }
            }
        }
    }

    /**
     * 得到平滑后的位置
     * @return 当前位置PointF
     */
    public PointF get() {
        return mNowPoint;
    }

    /**
     * 手动设置当前位置
     * @param pointF 当前位置PointF
     */
    public void set(PointF pointF) {
        mNowPoint.set(pointF);
        V = VMin;
    }

    /**
     * 手动清除当前位置(置为0点)
     */
    public void clear() {
        mNowPoint.set(0,0);
        V = VMin;
    }
}
