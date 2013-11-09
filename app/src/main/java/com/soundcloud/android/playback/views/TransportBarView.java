package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

public class TransportBarView extends LinearLayout {
    private ImageButton mPauseButton, mPrevButton, mNextButton;
    private ToggleButton mToggleComment;
    private Drawable mPlayState, mPauseState;
    private RelativeLayout mNextHolder;

    private boolean mNextEnabled;

    public TransportBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setStaticTransformationsEnabled(true);
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.transport_bar, this);

        setBackgroundResource(R.color.dark_bg);
        setGravity(Gravity.CENTER_VERTICAL);
        final int pad = (int) (context.getResources().getDisplayMetrics().density*5);
        setPadding(0,pad,0,pad);

        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPrevButton = (ImageButton) findViewById(R.id.prev);
        mNextButton = (ImageButton) findViewById(R.id.next);
        mToggleComment = (ToggleButton) findViewById(R.id.comment);
        mToggleComment.setSaveEnabled(false); // do not save comment state. UI too complicated to restore currently

        mPauseState = getResources().getDrawable(R.drawable.ic_pause_states);
        mPlayState = getResources().getDrawable(R.drawable.ic_play_states);
        mNextHolder = (RelativeLayout) mNextButton.getParent();
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

    public void setOnCommentListener(OnClickListener listener) {
        mToggleComment.setOnClickListener(listener);
    }
    public void setNextEnabled(boolean b) {
        mNextButton.setEnabled(b);
        mNextEnabled = b;
        mNextHolder.invalidate();
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        boolean ret = super.getChildStaticTransformation(child, t);
        if (child == mNextHolder && !mNextEnabled){
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

    public void setIsCommenting(boolean isCommenting) {
        if (mToggleComment != null){
            mToggleComment.setChecked(isCommenting);
        }
    }
}
