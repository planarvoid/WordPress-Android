package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.soundcloud.android.R;

public class PlayerTouchBar extends View {

    private boolean mSeeking;
    private boolean mLandscape;
    private Rect mMarkerRect;
    private Paint mMarkerPaint;

    public PlayerTouchBar(Context context) {
        super(context);
        init();
    }

    public PlayerTouchBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlayerTouchBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init(){
        mMarkerPaint = new Paint();
        mMarkerPaint.setStyle(Paint.Style.FILL);
        mMarkerPaint.setColor(getResources().getColor(R.color.portraitPlayerCommentLine));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);    //To change body of overridden methods use File | Settings | File Templates.
        if (mSeeking) {
            canvas.drawColor(0xAAFFFFFF);
            canvas.drawRect(mMarkerRect, mMarkerPaint);
        }
    }

    public void clearSeek() {
        mSeeking = false;
        invalidate();
    }

    public void setLandscape(boolean landscape) {
        mLandscape = landscape;
    }

    public void setSeekPosition(int seekPosition, int waveHeight) {
        mMarkerRect = new Rect(seekPosition,0,seekPosition+1,waveHeight);
        mSeeking = true;
        invalidate();
    }

}
