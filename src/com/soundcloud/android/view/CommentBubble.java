
package com.soundcloud.android.view;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CommentBubble extends RelativeLayout {

    private CommentBubbleArrow mArrow;
    
    
    private static int NEW_MODE_WIDTH = 170;
    private static int NEW_MODE_HEIGHT = 150;
    
    private static int SHOW_MODE_WIDTH = 250;
    private static int SHOW_MODE_HEIGHT = 220;
    
    public static int HARD_WIDTH;
    public static int CORNER_MARGIN = 20;
    
    private int mRealWidth = 1;
    
    // 0 = add
    // 1 = show
    private int mCurrentMode = 0;
    
    public Comment mComment;
    
    public Float mDensity;
    
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
                mPlayer.replyToComment(mComment);
            }
            
        });
        
        mBtnReadOn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mController.nextCommentInThread();
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
    
    public void loadComment(Comment comment) {
        mComment = comment;
    }
    
    public float setPosition(int x, int y, int edge) {
        int arrowOffset = x - HARD_WIDTH/4 < 0 ? x : x + 3*HARD_WIDTH/4 > edge ? x - (edge - HARD_WIDTH) : HARD_WIDTH/4; 
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams ) this.getLayoutParams();
        lp.leftMargin =(int) x - arrowOffset;
        lp.topMargin = (int) (y - lp.height);
        this.setLayoutParams(lp);
        
        mArrow.setPosition(arrowOffset);
        
        return ((float)arrowOffset)/mRealWidth;
    }

    public void onNewCommentMode(Track track, long l) {
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
        
        updateNewCommentTime(l);
    }
    
    public void onShowCommentMode(Comment currentShowingComment) {
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
        this.setLayoutParams(new RelativeLayout.LayoutParams(mRealWidth, newHeight));
    }
    
    public void updateNewCommentTime(long pos) {
        mTxtNewTime.setText(CloudUtils.makeTimeString(pos < 3600000 ? mDurationFormatShort
                : mDurationFormatLong, pos / 1000));
    }
    
}
