package com.zjut.henry.demo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.zjut.henry.indoorlocationng.Beacon;
import com.zjut.henry.indoorlocationng.BeaconLinkLayer;
import com.zjut.henry.indoorlocationng.LocationController;
import com.zjut.henry.indoorlocationng.LocationLayer;
import com.zjut.henry.indoorlocationng.R;
import com.zjut.henry.indoorlocationng.StepNav;

import java.util.ConcurrentModificationException;
import java.util.Locale;

/**
 * MapView
 * Created by 张剑 on 2017/10/16.
 * Updated by Henry on 2017/10/20
 */

class MapView extends SubsamplingScaleImageView {

    private static Paint dotPaint,locationBitmapPaint,labelPaint;
    private static Bitmap locationBitmap;
    private static Matrix locationBitmapMatrix;

    public MapView(Context context, AttributeSet attr) {
        super(context, attr);
        initialise();
    }

    private void initialise() {
        paintInitial();
        setMaxScale(1f);
        setDoubleTapZoomDpi(100);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Map is not ready
        if (!isReady())return;

        // Draw current beacons
        try {
            for (Beacon beacon : BeaconLinkLayer.getBeacons()) {
                if(beacon.getStatus()!=4)continue;
                PointF p = beacon.getCoordination();
                p = sourceToViewCoord(p.x, p.y);
                dotPaint.setStrokeWidth(5);
                dotPaint.setAlpha(80);
                canvas.drawCircle(p.x, p.y, 0.5F, dotPaint);
                labelPaint.setFakeBoldText(true);
                canvas.drawText(beacon.getMac(), p.x, p.y + labelPaint.getTextSize() * 1.5f, labelPaint);
                labelPaint.setFakeBoldText(false);
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", beacon.getRssi()), p.x, p.y + labelPaint.getTextSize() * 2.5f, labelPaint);
            }

            for (Beacon beacon : LocationLayer.getBeaconsActive()) {
                PointF p = beacon.getCoordination();
                p = sourceToViewCoord(p.x, p.y);
                dotPaint.setStrokeWidth(10);
                dotPaint.setAlpha(255);
                canvas.drawCircle(p.x, p.y, 0.5F, dotPaint);
            }
        } catch (ConcurrentModificationException cme) {
            Log.e("Map", "Concurrent Modification Exception.");
        }

        // 绘制Router产生的位置
        PointF p0 = LocationController.getLocationResult().getOriginCoordination();
        if (p0 != null && !p0.equals(0,0)) {
            // p0 = LocationFragment.barrierProcess(p0);
            PointF point0 = sourceToViewCoord(p0);

            locationBitmapMatrix.reset();
            locationBitmapMatrix.setTranslate(point0.x - locationBitmap.getWidth() / 2, point0.y - locationBitmap.getHeight() / 2);
            locationBitmapMatrix.postRotate(StepNav.getCompassOrientation(), point0.x, point0.y);
            canvas.drawBitmap(locationBitmap, locationBitmapMatrix, locationBitmapPaint);
        }

        // DEBUG: 绘制精确位置
        PointF perP0 = LocationLayer.getLocationRaw();
        // perP0 = LocationFragment.barrierProcess(perP0);
        PointF perPoint0 = sourceToViewCoord(perP0);

        dotPaint.setColor(Color.RED);
        dotPaint.setStrokeWidth(10);
        dotPaint.setAlpha(128);
        canvas.drawPoint(perPoint0.x, perPoint0.y, dotPaint);
        dotPaint.setColor(Color.parseColor("#303F9F"));

        invalidate();
    }

//    /**
//     * from source RectF to view RectF
//     * @param rectF Source RectF
//     * @return View RectF
//     */
//    private RectF sourceToViewRectF(RectF rectF) {
//        PointF start = sourceToViewCoord(rectF.left, rectF.top);
//        PointF end = sourceToViewCoord(rectF.right, rectF.bottom);
//        if (start != null && end != null) return new RectF(start.x, start.y, end.x, end.y);
//        else return null;
//    }

    /**
     * 初始化画笔
     * Initial Paint pattern
     */
    private void paintInitial() {
        float density = getResources().getDisplayMetrics().densityDpi;
        int strokeWidth = (int) (density / 60f);

        // Beacon Paint
        dotPaint = new Paint();
        dotPaint.setColor(Color.parseColor("#303F9F"));
        dotPaint.setAlpha(255);
        dotPaint.setStrokeWidth(strokeWidth * 2);
        dotPaint.setStyle(Paint.Style.STROKE);
        dotPaint.setStrokeCap(Paint.Cap.ROUND);
        dotPaint.setAntiAlias(true);

        // Text
        labelPaint = new Paint();
        labelPaint.setTextSize(16);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setAntiAlias(true);

        // location pin
        locationBitmapPaint = new Paint();
        locationBitmapPaint.setAntiAlias(true);
        locationBitmapMatrix = new Matrix();
        locationBitmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_pin_new);
    }
}