package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.CloudUtils;

public class PlayerTime extends RelativeLayout {
    protected TextView mCurrentTime;
    protected TextView mTotalTime;
    protected int mDuration;
    protected int mMaxTextWidth;
    protected int mMeasuredMaxWidth;

    public PlayerTime(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_time, this);

        mCurrentTime = (TextView) findViewById(R.id.txt_current);
        mTotalTime = (TextView) findViewById(R.id.txt_total);

        final int pad = (int) (5 * getResources().getDisplayMetrics().density);
        setPadding(pad, 0, pad, pad);

    }

    public void setCurrentTime(long time, boolean commenting) {
        mCurrentTime.setText(CloudUtils.formatTimestamp(time));
    }

    public void setByPercent(float seekPercent, boolean commenting) {
        setCurrentTime((long) (mDuration * seekPercent), commenting);
    }

    public void setDuration(int time) {
        final int digits = CloudUtils.getDigitsFromSeconds(time / 1000);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits; i++) {
            sb.append("8");
            if (i % 2 == 1 && i < 5) sb.append(".");
        }
        mCurrentTime.setText(sb);
        mTotalTime.setText(sb);

        getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        mMeasuredMaxWidth = getMeasuredWidth();
        getLayoutParams().width = mMeasuredMaxWidth;
        requestLayout();

        mDuration = time;
        mTotalTime.setText(CloudUtils.formatTimestamp(time));
        invalidate();
    }



    public void setWaveHeight(int height) {
    }
}