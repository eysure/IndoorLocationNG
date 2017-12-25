package com.zjut.henry.indoorlocationng;

import android.graphics.PointF;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 *  the <code>EyTool</code> class contain several useful tools to help you (generally specking, Eysure)
 *  write code more RAPIDLY. Tools are increasing and refining regularly.
 *
 *  @author  Eysure
 */
class EyTool {

    /**
     * A lovely Stop watch test class, use to measure system initTime passed, for multi-initTime use
     */
    static class EyStopWatch{
        private long initTime;
        private long lapTime;
        private ArrayList<Long> records;

        public EyStopWatch() {
        }

        /**
         * Start the mTimer, initiate the start initTime and array list
         * this return type let you use <code>new EyStopWatch().start()</code>
         * @return current StopWatch
         */
        public EyStopWatch start(){
            initTime = System.nanoTime();
            records = new ArrayList<>();
            return this;
        }

        /**
         * Get the record array list
         * @return record array list
         */
        public ArrayList<Long> getRecord(){
            return records;
        }

        /**
         * Peek at watch a glance, know the initTime passed by
         * @return delta initTime
         */
        public long peek(){
            return System.nanoTime()- initTime;
        }

        /**
         * Record current initTime in to array
         * @return delta initTime
         */
        public long record(){
            long now = System.nanoTime();
            records.add(now - initTime);
            return now - initTime;
        }

        /**
         * Catch every lap initTime in a row, use in mostly loops
         * @return lap initTime
         */
        public long lap(){
            long now = System.nanoTime();
            lapTime=now-lapTime;
            return lapTime;
        }

    }

    static class EyFilter{
        private Queue<Double> mQueue;
        private int mWindow;
        private double sum;
        private double last;

        public EyFilter(int window) {
            mQueue = new LinkedList<>();
            mWindow = window;
            sum = 0;
        }

        public double f(double d){
            last = d;
            if(mQueue.size()>mWindow)sum -= mQueue.poll();
            mQueue.offer(d);
            sum += d;
            return sum/mQueue.size();
        }

        public double get(){
            return sum/mQueue.size();
        }

        public int size(){
            return mQueue.size();
        }

        public double peek(){
            return mQueue.peek();
        }

        public double getLast(){
            return last;
        }

        @Override
        public String toString(){
            return String.valueOf(get());
        }
    }

    static class EyMath {
        // 根号好几个数的平方
        public static double sqrtPowSum(double... num) {
            double sum = 0;
            for (double n : num) sum += Math.pow(n, 2);
            return Math.sqrt(sum);
        }

        // 两个三维向量夹角余弦
        public static double cos2Vectors(double[] vector1, double[] vector2 )throws NullPointerException{
            return (vector1[0] * vector2[0] + vector1[1] * vector2[1]+ vector1[2] * vector2[2])
                    / (sqrtPowSum(vector1[0],vector1[1],vector1[2]) * sqrtPowSum(vector2[0],vector2[1],vector2[2]));
        }

        // 三维向量
        static class Vector3{
            public double x;
            public double y;
            public double z;

            // (构造) 以给定的x,y,z值生成新向量
            public Vector3(double x,double y,double z) {
                this.x = x;
                this.y = y;
                this.z = z;
            }

            // (构造) 从数组构造
            public Vector3(float[] floatArray)throws ArrayIndexOutOfBoundsException{
                this.x = floatArray[0];
                this.y = floatArray[1];
                this.z = floatArray[2];
            }

            // (构造) 新空向量
            public Vector3(){
            }

            // (构造) 从已有向量生成向量
            public Vector3(Vector3 vector3){
                this.x = vector3.x;
                this.y = vector3.y;
                this.z = vector3.z;
            }

            // 向量加向量
            public Vector3 add(Vector3 vector3){
                x += vector3.x;
                y += vector3.y;
                z += vector3.z;
                return this;
            }

            // 向量位移
            public Vector3 add(double offsetX, double offsetY, double offsetZ){
                x += offsetX;
                y += offsetY;
                z += offsetZ;
                return this;
            }

            // 数量积（点积)
            public Vector3 dotProduct(double d){
                x *= d;
                y *= d;
                z *= d;
                return this;
            }

            // 向量取反
            public Vector3 reverse(){
                return dotProduct(-1);
            }

            // 置零
            public Vector3 clear(){
                x=y=z=0;
                return this;
            }

            // --------------------------------
            // 以上方法为改变并返回自身的方法，不产生新向量
            // 以下方法会返回新的值或新向量，不改变原向量（以get修饰）
            // --------------------------------

            // 内积
            public double getDotProduct(Vector3 vector3){
                return this.x * vector3.x + this.y * vector3.y + this.z * vector3.z;
            }

            // 向量的模
            public double getNorm(){
                return sqrtPowSum(x,y,z);
            }

            // 得到在给定向量上的投影值(负值代表反向)
            public double getProjection(Vector3 vector3){
                return cos(this, vector3) * getNorm();
            }

            // 获得在给定向量上的投影向量
            public Vector3 getProjectVector(Vector3 vector3){
                return vector3.getUnitVector().dotProduct(getProjection(vector3));
            }

            // 获得与给定向量投影垂直的向量
            public Vector3 getPerpendicular(Vector3 vector3){
                return new Vector3(this).add(getProjectVector(vector3).reverse());
            }

            // 得到单位向量
            public Vector3 getUnitVector(){
                return new Vector3(this).dotProduct(1/ getNorm());
            }

            // 返回向量字符串
            public String toString(){
                return "Vector3("+x+","+y+","+z+")";
            }

            // 转为Float数组
            public float[] toFloatArray(){
                return new float[]{(float)this.x,(float)this.y,(float)this.z};
            }

            public Vector3 getCrossProduct(Vector3 vector, Boolean isRightHand){
                return getCrossProduct(this,vector,isRightHand);
            }

            // --------------------------------
            // 以下方法为静态公共方法，与本向量无关
            // --------------------------------

            // (公共) 内积
            public static double getDotProduct(Vector3 vector3a, Vector3 vector3b){
                return vector3a.x * vector3b.x + vector3a.y * vector3b.y + vector3a.z * vector3b.z;
            }

            // (公共)夹角余弦
            public static double cos(Vector3 vector3a,Vector3 vector3b){
                return getDotProduct(vector3a,vector3b)/(norm(vector3a)*norm(vector3b));
            }

            // (公共)向量的模
            public static double norm(Vector3 vector3){
                return sqrtPowSum(vector3.x,vector3.y,vector3.z);
            }

            // (公共)向量叉积/外积/向量积(右手系)
            public static Vector3 getCrossProduct(Vector3 a,Vector3 b,Boolean isRightHand){
                double x = a.y * b.z - a.z * b.y;
                double y = a.z * b.x - a.x * b.z;
                double z = a.x * b.y - a.y * b.x;
                if(isRightHand)return new Vector3(x,y,z).reverse();
                else return new Vector3(x,y,z);
            }
            public static Vector3 getOuterProduct(Vector3 a,Vector3 b,Boolean isRightHand){
                return getCrossProduct(a, b, isRightHand);
            }
            public static Vector3 getVectorProduct(Vector3 a,Vector3 b,Boolean isRightHand){
                return getCrossProduct(a, b, isRightHand);
            }
        }

        /**
         * 二维向量
         */
        static class Vector2 {
            public double x;
            public double y;

            // (构造) 以给定的x,y值生成新向量
            public Vector2(double x, double y) {
                this.x = x;
                this.y = y;
            }

            // (构造) 从数组构造
            public Vector2(float[] floatArray) throws ArrayIndexOutOfBoundsException {
                this.x = floatArray[0];
                this.y = floatArray[1];
            }

            // (构造) 从两个点构造
            public Vector2(double ax,double ay, double bx, double by){
                this.x = bx - ax;
                this.y = by - ay;
            }

            @SuppressWarnings("Android Only")
            public Vector2(PointF a,PointF b){
                this.x = b.x - a.x;
                this.y = b.y - a.y;
            }

            // (构造) 新空向量
            public Vector2() {
            }

            // (构造) 从已有向量生成向量
            public Vector2(Vector2 vector2) {
                this.x = vector2.x;
                this.y = vector2.y;
            }

            // 向量加向量
            public Vector2 add(Vector2 vector2) {
                x += vector2.x;
                y += vector2.y;
                return this;
            }

            // 向量位移
            public Vector2 add(double offsetX, double offsetY) {
                x += offsetX;
                y += offsetY;
                return this;
            }

            // 数量积（点积)
            public Vector2 dotProduct(double d) {
                x *= d;
                y *= d;
                return this;
            }

            // 向量取反
            public Vector2 reverse() {
                return dotProduct(-1);
            }

            // 置零
            public Vector2 clear(){
                x=y=0;
                return this;
            }

            // --------------------------------
            // 以上方法为改变并返回自身的方法，不产生新向量
            // 以下方法会返回新的值或新向量，不改变原向量（以get修饰）
            // --------------------------------

            // 内积
            public double getDotProduct(Vector2 vector2) {
                return this.x * vector2.x + this.y * vector2.y;
            }

            // 向量的模
            public double getNorm() {
                return sqrtPowSum(x, y);
            }

            // 获得与自身垂直的向量（右手系）
            public Vector2 getSelfPerpendicular(Boolean isRightHand){
                if(isRightHand)return new Vector2(-y,x);
                else return new Vector2(y,-x);
            }

            // 获得在给定向量上的投影
            public Vector2 getProjection(Vector2 vector2) {
                double p = cos(this, vector2) * getNorm();
                return vector2.getUnitVector2().dotProduct(p);
            }

            // 获得与给定向量投影垂直的向量
            public Vector2 getPerpendicular(Vector2 vector2) {
                return new Vector2(this).add(getProjection(vector2).reverse());
            }

            // 得到单位向量
            public Vector2 getUnitVector2() {
                return new Vector2(this).dotProduct(1 / getNorm());
            }

            // 返回向量字符串
            public String toString() {
                return "Vector2(" + x + "," + y + ")";
            }

            // 转为Float数组
            public float[] toFloatArray() {
                return new float[]{(float) this.x, (float) this.y};
            }

            // 转为角度（注意，这个角度中N为0度，N左为负右为正，值域为-180~180）
            public double toRadius(){
                return (Math.acos(y/getNorm()))*(x>0?1:-1);
            }

            // --------------------------------
            // 以下方法为静态公共方法，与本向量无关
            // --------------------------------

            // (公共) 内积
            public static double getDotProduct(Vector2 vector2A, Vector2 vector2B) {
                return vector2A.x * vector2B.x + vector2A.y * vector2B.y;
            }

            // (公共)夹角余弦
            public static double cos(Vector2 vector2A, Vector2 vector2B) {
                return getDotProduct(vector2A, vector2B) / (norm(vector2A) * norm(vector2B));
            }

            // (公共)向量的模
            public static double norm(Vector2 vector2) {
                return sqrtPowSum(vector2.x, vector2.y);
            }
        }

    }

    public static class Converter{

        /**
         * Convert bytes to Hex string.
         * e.g. transmission package analyse
         * @param bytes bytes need to convert
         * @return hex string
         */
        public static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte oneB : bytes) {
                String hex = Integer.toHexString(oneB & 0xFF);
                if (hex.length() == 1) hex = '0' + hex;
                sb.append(hex.toUpperCase());
            }
            return sb.toString();
        }

        /**
         * Convert dBm to mW
         * used mostly in RSSI(Received Signal Strength Indication) conversion
         * @param dBm signal strength in dBm
         * @return signal power in mW
         */
        public static double dBm2mw(double dBm){
            return Math.pow(10,dBm/10D);
        }
    }
}