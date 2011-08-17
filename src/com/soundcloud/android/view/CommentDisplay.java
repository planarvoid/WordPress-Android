package com.soundcloud.android.view;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
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

    public Comment mComment;
    public Track mTrack;

    protected TextView mTxtUsername;
    protected TextView mTxtDate;
    protected TextView mTxtElapsed;

    protected Button mBtnReadOn;
    protected ImageButton mBtnClose;
    protected TextView mTxtComment;
    protected Button mBtnReply;

    protected WaveformController mController;
    protected ScPlayer mPlayer;

    protected boolean interacted;
    protected boolean closing;

    public CommentDisplay(Context context, WaveformController controller) {

        super(context);

        mPlayer = (ScPlayer) context;
        mController = controller;


        LayoutInflater inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.comment_bubble, this);

        mTxtUsername = (TextView) findViewById(R.id.txt_username);
        mTxtDate = (TextView) findViewById(R.id.txt_show_date);
        mTxtElapsed = (TextView) findViewById(R.id.txt_show_elapsed);
        mBtnClose = (ImageButton) findViewById(R.id.btn_close);
        mBtnReadOn = (Button) findViewById(R.id.btn_read_on);
        mTxtComment = (TextView) findViewById(R.id.txt_comment);

        mBtnReply = (Button) findViewById(R.id.btn_reply);

        mBtnReply.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                mPlayer.addNewComment(CloudUtils.buildComment(mPlayer, mPlayer.getCurrentUserId(), mComment.track_id,
                        mComment.timestamp, "", mComment.id, mComment.user.username), mPlayer.addCommentListener);
            }

        });

        mBtnReadOn.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                mController.nextCommentInThread();
            }

        });

        mTxtUsername.setFocusable(true);
        mTxtUsername.setClickable(true);
        mTxtUsername.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mPlayer, UserBrowser.class);
                intent.putExtra("user", mComment.user);
                mPlayer.startActivity(intent);
            }

        });

        mBtnClose.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                mController.closeComment();
            }
        });

        setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                interacted = true;
            }
        });

        interacted = false;
        closing = false;
    }



    protected void onShowCommentMode(Comment currentShowingComment) {
        mComment = currentShowingComment;

        mTxtUsername.setText(mComment.user.username);
        mTxtDate.setText("at " + CloudUtils.formatTimestamp(mComment.timestamp));
        mTxtComment.setText(mComment.body);
        mTxtElapsed.setText(CloudUtils.getElapsedTimeString(getResources(),mComment.created_at.getTime(), true));
        mTxtUsername.setVisibility(View.VISIBLE);
        mTxtDate.setVisibility(View.VISIBLE);
        mTxtElapsed.setVisibility(View.VISIBLE);
        mBtnClose.setVisibility(View.VISIBLE);
        mTxtComment.setVisibility(View.VISIBLE);
        mBtnReply.setVisibility(View.VISIBLE);

        if (currentShowingComment.nextComment != null)
            mBtnReadOn.setVisibility(View.VISIBLE);
        else
            mBtnReadOn.setVisibility(View.GONE);

    }
}
