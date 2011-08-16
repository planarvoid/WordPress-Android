package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.CloudUtils;

public class PlayerTime extends LinearLayout {
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private int mDuration;

    public PlayerTime(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_time, this);

        mCurrentTime = (TextView) findViewById(R.id.txt_current);
        mTotalTime = (TextView) findViewById(R.id.txt_total);

        final int pad = (int) (5 * getResources().getDisplayMetrics().density);
        setPadding(pad, 0, pad, pad);
        setOrientation(HORIZONTAL);
    }

    public void setCurrentTime(long time) {
        mCurrentTime.setText(CloudUtils.formatTimestamp(time));
        requestLayout();
        invalidate();
    }

    public void setByPercent(float seekPercent) {
        setCurrentTime((long) (mDuration * seekPercent));
    }

    public void setDuration(int time) {
        mDuration = time;
        mTotalTime.setText(CloudUtils.formatTimestamp(time));
        invalidate();
    }


}