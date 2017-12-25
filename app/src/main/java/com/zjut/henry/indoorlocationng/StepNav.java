package com.zjut.henry.indoorlocationng;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.zjut.henry.indoorlocationng.EyTool.EyFilter;
import com.zjut.henry.indoorlocationng.EyTool.EyMath.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * 惯性导航
 * (这个类一下子看不懂先别看, 把其他的搞定)
 *
 * Created by henry on 10/26/17.
 */

public class StepNav implements SensorEventListener {

    // Listener
    private OnStepUpdateListener sOnStepUpdateListener;

    // Callback
    public interface OnStepUpdateListener {
        void onStepUpdate();
    }

    // Listener Setter
    void setOnStepUpdateListener(OnStepUpdateListener onStepUpdateListener) {
        sOnStepUpdateListener = onStepUpdateListener;
    }

    // Window
    private static final int ACCELERATION_WINDOW = 3;
    private static final int MAGNETIC_FIELD_WINDOW = 30;
    private static final int GRAVITY_WINDOW = 30;
    private static final int ORIENTATION_WINDOW = 10;

    // trigger acceleration of one step (ALWAYS >0)
    private static final float STEP_TRIGGER_ACCEL = 1.9f;

    // Min/Max interval of two steps
    private static final int MIN_STEP_INTERVAL = 450;
    private static final int INT_STEP_INTERVAL = 600;
    private static final int MAX_STEP_INTERVAL = 1800;

    private float dynamicMinInterval = INT_STEP_INTERVAL;

    // Data
    private long mLastStepTime = System.currentTimeMillis();
    private float[] mCoord = new float[2];

    // Step Length Data
    private float mStepLength = 0.68f;

    // Orientation
    private boolean mRestrictOrthogonal = true;
    private float[] mR = new float[9];
    private float[] mOrientationValues = new float[3];

    private static float sOrientationResult;

    // StepCounters
    private List<StepCounter> mStepCounterList = new ArrayList<>();

    // Value
    private Vector3 mAccV = new Vector3();
    private Vector3 mMagV = new Vector3();
    private Vector3 mGrvV = new Vector3();

    // Filter
    private EyFilter mAccX = new EyFilter(ACCELERATION_WINDOW);
    private EyFilter mAccY = new EyFilter(ACCELERATION_WINDOW);
    private EyFilter mAccZ = new EyFilter(ACCELERATION_WINDOW);
    private EyFilter mMagX = new EyFilter(MAGNETIC_FIELD_WINDOW);
    private EyFilter mMagY = new EyFilter(MAGNETIC_FIELD_WINDOW);
    private EyFilter mMagZ = new EyFilter(MAGNETIC_FIELD_WINDOW);
    private EyFilter mGrvX = new EyFilter(GRAVITY_WINDOW);
    private EyFilter mGrvY = new EyFilter(GRAVITY_WINDOW);
    private EyFilter mGrvZ = new EyFilter(GRAVITY_WINDOW);
    private EyFilter mSinFilter = new EyFilter(ORIENTATION_WINDOW);
    private EyFilter mCosFilter = new EyFilter(ORIENTATION_WINDOW);

    StepNav(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if(sensorManager==null)return;
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if(accelerometer !=null && magneticField !=null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()){
            case Sensor.TYPE_LINEAR_ACCELERATION:{
                mAccV.x = mAccX.f(event.values[0]);
                mAccV.y = mAccY.f(event.values[1]);
                mAccV.z = mAccZ.f(event.values[2]);
                if(mGrvV.getNorm()!=0)mAccV = mAccV.getProjectVector(mGrvV);
                break;
            }
            case Sensor.TYPE_MAGNETIC_FIELD:{
                mMagV.x = mMagX.f(event.values[0]);
                mMagV.y = mMagY.f(event.values[1]);
                mMagV.z = mMagZ.f(event.values[2]);
                break;
            }
            case Sensor.TYPE_GRAVITY:{
                mGrvV.x = mGrvX.f(event.values[0]);
                mGrvV.y = mGrvY.f(event.values[1]);
                mGrvV.z = mGrvZ.f(event.values[2]);
                break;
            }
            default:break;
        }

        // Step Detect
        long currentTime = System.currentTimeMillis();
        if(mAccV.getNorm()>STEP_TRIGGER_ACCEL){
            long t = currentTime - mLastStepTime;
            if(dynamicMinInterval<t)newStep();
            else if(MAX_STEP_INTERVAL<=t)dynamicMinInterval = INT_STEP_INTERVAL;
            else if(dynamicMinInterval>=t && MIN_STEP_INTERVAL<t)dynamicMinInterval-=50;
            else return;
            mLastStepTime = currentTime;
        }

        // Deal with angle
        if(mAccV.getNorm()!=0 && mMagV.getNorm()!=0){
            SensorManager.getRotationMatrix(mR,null,mGrvV.toFloatArray(),mMagV.toFloatArray());
            SensorManager.getOrientation(mR,mOrientationValues);

            mSinFilter.f(Math.sin(mOrientationValues[0]));
            mCosFilter.f(Math.cos(mOrientationValues[0]));
            float deg = (float)Math.toDegrees(Math.asin(mSinFilter.get()));
            if(mSinFilter.get()>0 && mCosFilter.get()<0)deg = 180 - deg;
            if(mSinFilter.get()<0 && mCosFilter.get()<0)deg = -180 - deg;
            sOrientationResult = deg;
        }
    }

    private void newStep(){

        sOnStepUpdateListener.onStepUpdate();

        float[] currentStep = {0f,0f};
        double sin = mSinFilter.get();
        double cos = mCosFilter.get();
        if(mRestrictOrthogonal){

            if(Math.abs(sin)<0.5){
                currentStep[1] = mStepLength * (cos>0?1:-1);
            }
            else if(sin>= 0.5 && sin<Math.sqrt(3)/2){
                currentStep[0] = (float)(mStepLength * Math.sqrt(2)/2);
                currentStep[1] = (float)(mStepLength * (cos>0?1:-1) * Math.sqrt(2)/2);
            }
            else if(sin>-Math.sqrt(3)/2 && sin<=-0.5){
                currentStep[0] = (float)(-mStepLength * Math.sqrt(2)/2);
                currentStep[1] = (float)(mStepLength * (cos>0?1:-1) * Math.sqrt(2)/2);
            }
            else if(sin>=Math.sqrt(3)/2)
                currentStep[0] = mStepLength;
            else if(sin<=-Math.sqrt(3)/2)
                currentStep[0] = -mStepLength;
        }
        else {
            currentStep[0] = (float)(mStepLength * sin);
            currentStep[1] = (float)(mStepLength * cos);
        }

        // count step for every single counter
        for(StepCounter counter : mStepCounterList){
            if(counter.isActive())counter.add(currentStep);
        }
    }

    public float getStepLength() {
        return mStepLength;
    }

    public void setStepLength(float stepLength) {
        mStepLength = stepLength;
    }

    // -------------Public method

    // return and flash distance
    public float[] getCoord(){
        float[] coord = mCoord;
        mCoord = new float[2];
        return coord;
    }

    public void setRestrictOrthogonal(boolean restrictOrthogonal) {
        mRestrictOrthogonal = restrictOrthogonal;
    }

    /**
     * 获取当前指南针实时的角度
     * 角度为degree, 正北为0度
     * @return 角度
     */
    public static float getCompassOrientation(){
        return sOrientationResult;
    }

    // ------------STEP COUNTER
    public class StepCounter {
        private float[] mStepVector;
        private int mStepCount;
        private boolean mActive;
        private boolean mDeleted;

        public StepCounter() {
            mDeleted = false;
            mActive = false;
            mStepCounterList.add(this);
            mStepVector = new float[2];
            mStepCount = 0;
        }

        public StepCounter start(){
            if(mDeleted)return null;
            mActive = true;
            return this;
        }

        public StepCounter stop(){
            if(mDeleted)return null;
            mActive = false;
            return this;
        }

        public StepCounter flush(){
            if(mDeleted)return null;
            mStepVector = new float[2];
            mStepCount = 0;
            return this;
        }

        public boolean isActive() {
            return !mDeleted && mActive;
        }

        public StepCounter add(float[] step)throws NullPointerException{
            mStepVector[0] += step[0];
            mStepVector[1] += step[1];
            mStepCount++;
            return this;
        }

        public float[] get(){
            if(mDeleted)return null;
            return mStepVector;
        }

        public int getCount(){
            return mStepCount;
        }

        public void delete(){
            mStepCounterList.remove(this);
            mDeleted = true;
        }

        public boolean isDeleted(){
            return mDeleted;
        }
    }
}