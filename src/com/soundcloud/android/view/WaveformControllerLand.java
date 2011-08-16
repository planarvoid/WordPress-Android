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
}
