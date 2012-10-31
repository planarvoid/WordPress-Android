package com.soundcloud.android.view.play;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class TransportBar extends LinearLayout{
    private ImageButton mPauseButton, mPrevButton, mNextButton;
    private Drawable mPlayState, mPauseState;
    private ViewParent mPrevHolder;
    private ViewParent mNextHolder;

    public TransportBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.transport_bar, this);

        setBackgroundResource(R.drawable.black_transport_bar_gradient);
        setGravity(Gravity.CENTER_VERTICAL);
        final int pad = (int) (context.getResources().getDisplayMetrics().density*5);
        setPadding(0,pad,0,pad);

        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPrevButton = (ImageButton) findViewById(R.id.prev);
        mNextButton = (ImageButton) findViewById(R.id.next);

        mPauseState = getResources().getDrawable(R.drawable.ic_pause_states);
        mPlayState = getResources().getDrawable(R.drawable.ic_play_states);

        mPrevHolder = mPrevButton.getParent();
        mNextHolder = mNextButton.getParent();

    }

    public void setOnNextListener(OnClickListener listener) {
        mNextButton.setOnClickListener(listener);
    }

    public void setOnPauseListener(OnClickListener listener) {
        mPauseButton.setOnClickListener(listener);
    }

    public void setOnPrevListener(OnClickListener listener) {
        mPrevButton.setOnClickListener(listener);
    }

    public void setNavEnabled(boolean b) {
        setStaticTransformationsEnabled(!b);
        invalidate();
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        boolean ret = super.getChildStaticTransformation(child, t);
        if (child == mPrevHolder || child == mNextHolder){
            t.setAlpha(0.5f);
            return true;
        }
        return ret;
    }

    public void setPlaybackState(boolean showPlayState) {
        if (mPauseButton != null) {
            mPauseButton.setImageDrawable(showPlayState ? mPauseState : mPlayState);
        }
    }
}
