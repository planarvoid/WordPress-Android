
package com.soundcloud.android.view;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CommentBubble extends RelativeLayout {

    private CommentBubbleArrow mArrow;
    
    
    private static int NEW_MODE_WIDTH = 170;
    private static int NEW_MODE_HEIGHT = 150;
    
    private static int SHOW_MODE_WIDTH = 250;
    private static int SHOW_MODE_HEIGHT = 210;
    
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
    
    private ImageView mImgClose;
    
    private TextView mTxtComment;
    
    private TextView mReply;
    
    private WaveformController mController;
    
    private ScPlayer mPlayer;
    

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
        mImgClose = (ImageView) findViewById(R.id.close);
        mTxtComment = (TextView) findViewById(R.id.txt_comment);
        
        mReply = (TextView) findViewById(R.id.txt_reply);
        
        mReply.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mPlayer.replyToComment(mComment);
            }
            
        });
        
        mImgClose.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mController.closeComment();
            }
            
        });
        
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
        mImgClose.setVisibility(View.GONE);
        mTxtComment.setVisibility(View.GONE);
        mReply.setVisibility(View.GONE);
        
        mTxtNewTime.setVisibility(View.VISIBLE);
        mTxtNewInstructions.setVisibility(View.VISIBLE);
        
        updateNewCommentTime(l);
    }
    
    public void onShowCommentMode(Comment currentShowingComment) {
        mComment = currentShowingComment;
        
        mTxtUsername.setVisibility(View.VISIBLE);
        mTxtDate.setVisibility(View.VISIBLE);
        mTxtElapsed.setVisibility(View.VISIBLE);
        mImgClose.setVisibility(View.VISIBLE);
        mTxtComment.setVisibility(View.VISIBLE);
        mReply.setVisibility(View.VISIBLE);
        
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
