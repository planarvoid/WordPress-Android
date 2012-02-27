package com.soundcloud.android.view;

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
    private ImageButton mPauseButton, mFavoriteButton, mCommentButton, mPrevButton, mNextButton;
    private Drawable mFavoriteDrawable, mFavoritedDrawable, mPlayState, mPauseState;
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
        mFavoriteButton = (ImageButton) findViewById(R.id.btn_favorite);
        mCommentButton = (ImageButton) findViewById(R.id.btn_comment);

        mPauseState = getResources().getDrawable(R.drawable.ic_pause_states);
        mPlayState = getResources().getDrawable(R.drawable.ic_play_states);

        mPrevHolder = mPrevButton.getParent();
        mNextHolder = mNextButton.getParent();

    }

    public void setCommentMode(boolean isCommenting){
        if (mCommentButton != null) {
            mCommentButton.setImageResource(isCommenting ? R.drawable.ic_commenting_states : R.drawable.ic_comment_states);
        }
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
        if (mCommentButton != null) mCommentButton.setOnClickListener(listener);
    }
    public void setOnFavoriteListener(OnClickListener listener) {
        if (mFavoriteButton != null) mFavoriteButton.setOnClickListener(listener);
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

    public void setFavoriteStatus(boolean isFavorite) {
        if (mFavoriteButton == null) return;

        if (isFavorite) {
            if (mFavoritedDrawable == null) mFavoritedDrawable = getResources().getDrawable(R.drawable.ic_liked_states);
            mFavoriteButton.setImageDrawable(mFavoritedDrawable);
        } else {
            if (mFavoriteDrawable == null) mFavoriteDrawable = getResources().getDrawable(R.drawable.ic_like_states);
            mFavoriteButton.setImageDrawable(mFavoriteDrawable);
        }
    }

    public void setPlaybackState(boolean showPlayState) {
        if (mPauseButton != null) {
            mPauseButton.setImageDrawable(showPlayState ? mPauseState : mPlayState);
        }
    }
}
