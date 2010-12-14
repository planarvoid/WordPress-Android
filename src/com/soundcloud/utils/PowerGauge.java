package com.soundcloud.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;

public class PowerGauge extends View {

	public PowerGauge(Context context) {
		super(context);
		
		mPaint = new Paint();
		mPaint.setStyle(Style.FILL);
	}
	
	
	public final void update(double power) {
        synchronized (this) {
        	
        	mLastSquareOn = (int) Math.floor((power - POWER_BASE)/(POWER_PEAK-POWER_BASE)*METER_COLORS.length);
        	postInvalidate();
        }
    }
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		
		if (changed && this.getWidth() > 0){
			sqWidth = (this.getWidth() - GUTTER_WIDTH * METER_COLORS.length)/METER_COLORS.length;
		} else if (changed) {
			sqWidth = 0;
		}
	}
    
	@Override
	protected void onDraw(Canvas canvas) {
		
		drawMeter(canvas);
		
	}
	
	
	
	private void drawMeter(Canvas canvas){
		
		if (sqWidth == 0)
			return;
		
		
		int nextX = 0;
		
		for (int i = 0; i < METER_COLORS.length; i++){
			//mPaint.setColor(i <= mLastSquareOn ? METER_COLORS_ON[i] : METER_COLORS[i]);
			mPaint.setColor(METER_COLORS[i]);
			if (i > mLastSquareOn) mPaint.setAlpha(50);
			canvas.drawRect(nextX,0,nextX+sqWidth,SQUARE_HEIGHT, mPaint);
			nextX += sqWidth + GUTTER_WIDTH;
		}
	}
	
	
	
	
	
	
	// ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "PowerGauge";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	private Paint mPaint;
	
	private int mLastSquareOn;
	private static final Double POWER_BASE = -70.0;
	private static final Double POWER_PEAK = 0.0;
	private static final int SQUARE_HEIGHT = 20;
	private static final int GUTTER_WIDTH = 3;
	
	
	
	private int sqWidth = 0;
	
	
	private static final int[] METER_COLORS = {0xFF389717
		, 0xFF3B9617
		, 0xFF3D9416
		, 0xFF409316
		, 0xFF449116
		, 0xFF488E15
		, 0xFF4D8C15
		, 0xFF518915
		, 0xFF568714
		, 0xFF5C8414
		, 0xFF618114
		, 0xFF677E13
		, 0xFF6C7C13
		, 0xFF717813
		, 0xFF777513
		, 0xFF7E7213
		, 0xFF846F13
		, 0xFF8B6C13
		, 0xFF916813
		, 0xFF976513
		, 0xFFAD5B13
		, 0xFFBE5214
		, 0xFFCE4A16
		, 0xFFE34017
		, 0xFFEE3919
		, 0xFFF63519};
	
	/*private static final int[] METER_COLORS = {
		0xFF389717
		, 0xFF3D9416
		, 0xFF449116
		, 0xFF4D8C15
		, 0xFF568714
		, 0xFF618114
		, 0xFF6C7C13
		, 0xFF777513
		, 0xFF846F13
		, 0xFF916813
		, 0xFFAD5B13
		, 0xFFCE4A16
		, 0xFFEE3919};
	
	private static final int[] METER_COLORS_ON = {
		0xFF3B9617
		, 0xFF409316
		, 0xFF488E15
		, 0xFF518915
		, 0xFF5C8414
		, 0xFF677E13
		, 0xFF717813
		, 0xFF7E7213
		, 0xFF8B6C13
		, 0xFF976513
		, 0xFFBE5214
		, 0xFFE34017
		, 0xFFF63519};
*/
}
