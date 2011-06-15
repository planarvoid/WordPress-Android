
package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CommentBubble extends RelativeLayout {

    private CommentBubbleArrow mArrow;

    private static int NEW_MODE_WIDTH = 180;
    private static int NEW_MODE_HEIGHT = 100;

    private static int SHOW_MODE_WIDTH = 280;
    private static int SHOW_MODE_HEIGHT = 145;

    public static int HARD_WIDTH;
    public static int CORNER_MARGIN = 20;

    public static int MODE_NEW_COMMENT = 1;
    public static int MODE_SHOW_COMMENT = 2;

    private int mRealWidth = 1;

    public Comment mComment;

    public Track mTrack;

    private TextView mTxtNewTime;

    private TextView mTxtNewInstructions;

    private String mDurationFormatShort;

    private String mDurationFormatLong;

    private TextView mTxtUsername;

    private TextView mTxtDate;

    private TextView mTxtElapsed;

    private Button mBtnReadOn;

    private ImageButton mBtnClose;

    private TextView mTxtComment;

    private Button mBtnReply;

    private WaveformController mController;

    private ScPlayer mPlayer;

    public boolean interacted;

    public boolean closing;

    public long new_comment_timestamp;

    public Track new_comment_track;

    public Comment show_comment;

    public int comment_mode;

    public int xPos;

    public int yPos;

    public int parentWidth;


    public CommentBubble(Context context, WaveformController controller) {

        super(context);

        mPlayer = (ScPlayer) context;
        mController = controller;


        LayoutInflater inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_comment_bubble, this);

        mTxtNewTime = (TextView) findViewById(R.id.txt_new_time);
        mTxtNewInstructions = (TextView) findViewById(R.id.txt_new_instructions);

        mDurationFormatLong = context.getString(R.string.durationformatlong);
        mDurationFormatShort = context.getString(R.string.durationformatshort);

        mArrow = new CommentBubbleArrow(context);
        setDimensions(NEW_MODE_WIDTH,NEW_MODE_HEIGHT);
        this.addView(mArrow);

        mTxtUsername = (TextView) findViewById(R.id.txt_username);
        mTxtDate = (TextView) findViewById(R.id.txt_show_date);
        mTxtElapsed = (TextView) findViewById(R.id.txt_show_elapsed);
        mBtnClose = (ImageButton) findViewById(R.id.btn_close);
        mBtnReadOn = (Button) findViewById(R.id.btn_read_on);
        mTxtComment = (TextView) findViewById(R.id.txt_comment);

        mBtnReply = (Button) findViewById(R.id.btn_reply);

        mBtnReply.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mPlayer.addNewComment(CloudUtils.buildComment(mPlayer, mPlayer.getCurrentUserId(), mComment.track_id,
                        mComment.timestamp, "", mComment.id, mComment.user.username), mPlayer.addCommentListener);
            }

        });

        mBtnReadOn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mController.nextCommentInThread();
            }

        });

        mTxtUsername.setFocusable(true);
        mTxtUsername.setClickable(true);
        mTxtUsername.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mPlayer, UserBrowser.class);
                intent.putExtra("userId", mComment.user.id);
                mPlayer.startActivity(intent);
            }

        });

        mBtnClose.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mController.closeComment();
            }
        });

        setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                interacted = true;
            }
        });

        interacted = false;
        closing = false;
    }

    public float update() {
        switch (comment_mode){
            case 1 : onNewCommentMode(new_comment_track); break;
            case 2 : onShowCommentMode(show_comment); break;
        }
        return updatePosition();
    }

    public float updatePosition(){
        int arrowOffset = (xPos - HARD_WIDTH / 4 < 0) ? xPos :
                (xPos + 3 * HARD_WIDTH / 4 > parentWidth) ? xPos - (parentWidth - HARD_WIDTH) :
                HARD_WIDTH / 4;
        RelativeLayout.LayoutParams lp = (LayoutParams) getLayoutParams();
        lp.leftMargin = xPos - arrowOffset;
        lp.topMargin = yPos - lp.height + (int)(5*getContext().getResources().getDisplayMetrics().density);
        setLayoutParams(lp);
        mArrow.setPosition(arrowOffset);
        return ((float)arrowOffset)/mRealWidth;
    }

    private void onNewCommentMode(Track track) {
        mTrack = track;
        setDimensions(NEW_MODE_WIDTH,NEW_MODE_HEIGHT);

        mTxtUsername.setVisibility(View.GONE);
        mTxtDate.setVisibility(View.GONE);
        mTxtElapsed.setVisibility(View.GONE);
        mBtnClose.setVisibility(View.GONE);
        mTxtComment.setVisibility(View.GONE);
        mBtnReply.setVisibility(View.GONE);
        mBtnReadOn.setVisibility(View.GONE);

        mTxtNewTime.setVisibility(View.VISIBLE);
        mTxtNewInstructions.setVisibility(View.VISIBLE);

        updateNewCommentTime();
    }

    private void onShowCommentMode(Comment currentShowingComment) {
        mComment = currentShowingComment;

        mTxtUsername.setText(mComment.user.username);
        mTxtDate.setText("at " + CloudUtils.formatTimestamp(mComment.timestamp));
        mTxtComment.setText(mComment.body);

        long elapsed = (System.currentTimeMillis() - mComment.created_at.getTime())/1000;

        if (elapsed < 60)
            mTxtElapsed.setText(mPlayer.getResources().getQuantityString(R.plurals.elapsed_seconds, (int) elapsed,(int) elapsed));
        else if (elapsed < 3600)
            mTxtElapsed.setText(mPlayer.getResources().getQuantityString(R.plurals.elapsed_minutes, (int) (elapsed/60),(int) (elapsed/60)));
        else if (elapsed < 86400)
            mTxtElapsed.setText(mPlayer.getResources().getQuantityString(R.plurals.elapsed_hours, (int) (elapsed/3600),(int) (elapsed/3600)));
        else if (elapsed < 2592000)
            mTxtElapsed.setText(mPlayer.getResources().getQuantityString(R.plurals.elapsed_days, (int) (elapsed/86400),(int) (elapsed/86400)));
        else if (elapsed < 31536000)
            mTxtElapsed.setText(mPlayer.getResources().getQuantityString(R.plurals.elapsed_months, (int) (elapsed/2592000),(int) (elapsed/2592000)));
        else
            mTxtElapsed.setText(mPlayer.getResources().getQuantityString(R.plurals.elapsed_years, (int) (elapsed/31536000),(int) (elapsed/31536000)));


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

        mTxtNewTime.setVisibility(View.GONE);
        mTxtNewInstructions.setVisibility(View.GONE);

        setDimensions(SHOW_MODE_WIDTH,SHOW_MODE_HEIGHT);
    }

    private void setDimensions(int newWidth, int newHeight){
        HARD_WIDTH = newWidth;
        HARD_WIDTH *= getContext().getResources().getDisplayMetrics().density;
        mRealWidth = HARD_WIDTH+CORNER_MARGIN*2;

        this.setLayoutParams(new RelativeLayout.LayoutParams(mRealWidth, (int) (newHeight* getContext().getResources().getDisplayMetrics().density)));
    }

    public void updateNewCommentTime() {
        mTxtNewTime.setText(CloudUtils.makeTimeString(new_comment_timestamp < 3600000 ? mDurationFormatShort
                : mDurationFormatLong, new_comment_timestamp / 1000));
    }
}
