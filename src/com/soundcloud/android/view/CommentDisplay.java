package com.soundcloud.android.view;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;

public class CommentDisplay extends RelativeLayout {

    protected Comment mComment;
    protected Track mTrack;

    protected TextView mTxtUsername;
    protected TextView mTxtTimestamp;
    protected TextView mTxtElapsed;

    protected Button mBtnReadOn;
    protected ImageButton mBtnClose;
    protected TextView mTxtComment;
    protected Button mBtnReply;

    protected WaveformController mController;
    protected ScPlayer mPlayer;

    public Comment show_comment;

    protected boolean interacted;
    protected boolean closing;

    private String at_timestamp;

    public CommentDisplay(Context context) {
        super(context);
        init();
    }

    public CommentDisplay(Context context, AttributeSet attr) {
        super(context, attr);
        init();
    }

    protected void init() {

        at_timestamp = getResources().getString(R.string.at_timestamp);

        mTxtUsername = (TextView) findViewById(R.id.txt_username);
        mTxtTimestamp = (TextView) findViewById(R.id.txt_timestamp);
        mTxtElapsed = (TextView) findViewById(R.id.txt_elapsed);
        mBtnClose = (ImageButton) findViewById(R.id.btn_close);
        mBtnReadOn = (Button) findViewById(R.id.btn_read_on);
        mTxtComment = (TextView) findViewById(R.id.txt_comment);
        mBtnReply = (Button) findViewById(R.id.btn_reply);

        if (mBtnReply != null)
            mBtnReply.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPlayer.addNewComment(CloudUtils.buildComment(mPlayer, mPlayer.getCurrentUserId(), mComment.track_id,
                            mComment.timestamp, "", mComment.id, mComment.user.username), mPlayer.addCommentListener);
                }

            });

        if (mBtnReadOn != null)
            mBtnReadOn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.nextCommentInThread();
                }

            });

        mTxtUsername.setFocusable(true);
        mTxtUsername.setClickable(true);
        mTxtUsername.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mPlayer, UserBrowser.class);
                intent.putExtra("user", mComment.user);
                mPlayer.startActivity(intent);
            }

        });

        if (mBtnClose != null)
            mBtnClose.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mController.closeComment();
                }
            });

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                interacted = true;
            }
        });

        interacted = false;
        closing = false;
    }

    protected void setControllers(ScPlayer player, WaveformController controller) {
        mPlayer = player;
        mController = controller;
    }

    protected void showComment(Comment currentShowingComment) {
        mComment = currentShowingComment;

        mTxtUsername.setText(mComment.user.username);
        mTxtTimestamp.setText(String.format(at_timestamp, CloudUtils.formatTimestamp(mComment.timestamp)));
        mTxtComment.setText(mComment.body);
        mTxtElapsed.setText(CloudUtils.getElapsedTimeString(getResources(), mComment.created_at.getTime(), true));
        mTxtUsername.setVisibility(View.VISIBLE);
        mTxtTimestamp.setVisibility(View.VISIBLE);
        mTxtElapsed.setVisibility(View.VISIBLE);
        mTxtComment.setVisibility(View.VISIBLE);

        if (mBtnReply != null) mBtnReply.setVisibility(View.VISIBLE);
        if (mBtnClose != null) mBtnClose.setVisibility(View.VISIBLE);
        if (mBtnReadOn != null) {
            if (currentShowingComment.nextComment != null)
                mBtnReadOn.setVisibility(View.VISIBLE);
            else
                mBtnReadOn.setVisibility(View.GONE);

        }

    }

    public Comment getComment() {
        return mComment;
    }
}
