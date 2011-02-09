
package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class CommentBubble extends RelativeLayout {

    private CommentBubbleArrow mArrow;
    
    public static int HARD_WIDTH = 200;
    public static int CORNER_MARGIN = 20;
    
    public Comment mComment;
    
    public Float mDensity;
    
    public Track mTrack;

    public CommentBubble(Context context) {
        
        super(context);

        LayoutInflater inflater = (LayoutInflater) context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_comment_bubble, this);
        
        HARD_WIDTH *= getContext().getResources().getDisplayMetrics().density;
        //CORNER_MARGIN *= getContext().getResources().getDisplayMetrics().density;
        
        mArrow = new CommentBubbleArrow(context);        
        setHeight(200);
        this.addView(mArrow);
    }
    
    public void loadComment(Comment comment) {
        mComment = comment;
    }
    
    public void setHeight(int height) {
        this.setLayoutParams(new RelativeLayout.LayoutParams(HARD_WIDTH+CORNER_MARGIN*2, height));
        //this.setPadding(CORNER_MARGIN, 0, CORNER_MARGIN, 0);
    }


    public void setPosition(int x, int y, int edge) {
        
        int arrowOffset = x - HARD_WIDTH/4 < 0 ? x : x + 3*HARD_WIDTH/4 > edge ? x - (edge - HARD_WIDTH) : HARD_WIDTH/4; 
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams ) this.getLayoutParams();
        lp.leftMargin =(int) x - arrowOffset;
        lp.topMargin = (int) (y - lp.height);
        this.setLayoutParams(lp);
        
        mArrow.setPosition(arrowOffset);
    }

    public void onNewCommentMode(Track mPlayingTrack) {
        mTrack = mPlayingTrack;
    }
    
}
