package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class WaveformControllerLand extends WaveformController {


    public WaveformControllerLand(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPlayerCommentBar = (RelativeLayout) findViewById(R.id.new_comment_bar);
        mPlayerCommentBar.setOnTouchListener(this);
        if (!mShowingComments) {
            ((TextView) mPlayerCommentBar.findViewById(R.id.txt_instructions))
                    .setText(getResources().getString(R.string.player_touch_bar_disabled));
        }

        mToggleComments = (ImageButton) findViewById(R.id.btn_toggle);
        mToggleComments.setImageDrawable((mShowingComments) ? mPlayer.getResources()
                .getDrawable(R.drawable.ic_hide_comments_states) : mPlayer.getResources()
                .getDrawable(R.drawable.ic_show_comments_states));
        mToggleComments.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleComments();
            }
        });
        setStaticTransformationsEnabled(false);
    }


    public void showConnectingLayout() {
        mWaveformHolder.showConnectingLayout(false);
        invalidate();
    }

    @Override
    protected void hideCommenters(boolean instant) {
        hideCommenters(mPlayerAvatarBar, instant);
    }

    @Override
    protected void showCommenters(boolean instant) {

        showCommenters(mPlayerAvatarBar, instant, (int) -getResources().getDimension(R.dimen.player_avatar_bar_height_land));
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        if (animation == mHideCommentersAnimation) {
            ((LayoutParams) mPlayerAvatarBar.getLayoutParams()).topMargin = 0;
            mClearAnimationOnLayout = true;
            requestLayout();
        } else if (animation == mShowCommentersAnimation) {
            ((LayoutParams) mPlayerAvatarBar.getLayoutParams()).topMargin = (int) -getResources().getDimension(R.dimen.player_avatar_bar_height_land);
            mClearAnimationOnLayout = true;
            mPlayerAvatarBar.requestLayout();

        }
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mClearAnimationOnLayout) {
            mPlayerAvatarBar.clearAnimation();
            mPlayerAvatarBar.invalidate();
            mClearAnimationOnLayout = false;
        }
        super.onLayout(changed, l, t, r, b);
    }
}
