
package com.soundcloud.android.view;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;

public class CommentBubble extends CommentDisplay {

    private CommentBubbleArrow mArrow;

    private static final int NEW_MODE_WIDTH = 180;
    private static final int NEW_MODE_HEIGHT = 100;

    private static final int SHOW_MODE_WIDTH = 280;
    private static final int SHOW_MODE_HEIGHT = 145;

    public static int HARD_WIDTH;
    public static final int CORNER_MARGIN = 20;

    public static final int MODE_NEW_COMMENT = 1;
    public static final int MODE_SHOW_COMMENT = 2;

    protected TextView mTxtNewTime;
    protected TextView mTxtNewInstructions;

    public long new_comment_timestamp;
    public Track new_comment_track;
    public Comment show_comment;

    private int mRealWidth = 1;

    public int xPos;
    public int yPos;
    public int parentWidth;
    public int comment_mode;


    public CommentBubble(Context context, WaveformController controller) {
        super(context, controller);

        mTxtNewTime = (TextView) findViewById(R.id.txt_new_time);
        mTxtNewInstructions = (TextView) findViewById(R.id.txt_new_instructions);

        mArrow = new CommentBubbleArrow(context);
        setDimensions(NEW_MODE_WIDTH,NEW_MODE_HEIGHT);
        this.addView(mArrow);

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

    protected void onNewCommentMode(Track track) {
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

    @Override
    protected void onShowCommentMode(Comment currentShowingComment) {
        super.onShowCommentMode(currentShowingComment);
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
        mTxtNewTime.setText(CloudUtils.formatTimestamp(new_comment_timestamp));
    }
}
