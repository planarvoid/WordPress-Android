package com.soundcloud.android.view;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
import android.view.animation.ScaleAnimation;
import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.InputObject;

public class WaveformControllerLand extends WaveformController {

    private CommentBubble mCommentBubble;
        private Animation mBubbleAnimation;
    private boolean mShowBubble;
    private boolean mShowBubbleForceAnimation;
    private int mAvatarOffsetY, mCommentBarOffsetY;

    protected static final int UI_UPDATE_BUBBLE = 1;
    protected static final int UI_UPDATE_BUBBLE_DISPLAY_NEW_COMMENT = 2;
    protected static final int UI_SHOW_CURRENT_COMMENT = 3;
    protected static final int UI_ADD_COMMENT = 4;
    private static final int UI_TOGGLE_COMMENTS = 5;



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

    private long stampFromPosition(int x){
        return (long) (Math.min(Math.max(.001, (((float)x)/getWidth())),1) * mPlayingTrack.duration);
    }

    private void showBubble(float offsetX){

        if (mCommentBubble == null) {
            return;
        }

        mCommentBubble.closing = false;

        if (mCommentBubble.getParent() != mPlayer.getCommentHolder() || mShowBubbleForceAnimation){
            if (mCommentBubble.getParent() != mPlayer.getCommentHolder()){
                    mPlayer.getCommentHolder().addView(mCommentBubble);
                    offsetX = mCommentBubble.update();
            }

            if (mBubbleAnimation != null && Build.VERSION.SDK_INT > 7)
                mBubbleAnimation.cancel();

            mBubbleAnimation = new ScaleAnimation((float).6, (float)1.0, (float).6, (float)1.0, Animation.RELATIVE_TO_SELF, offsetX,Animation.RELATIVE_TO_SELF, (float) 1.0);
            mBubbleAnimation.setFillAfter(true);
            mBubbleAnimation.setDuration(100);
            mBubbleAnimation.setAnimationListener(new Animation.AnimationListener(){

                @Override
                public void onAnimationEnd(Animation arg0) {
                    mBubbleAnimation = null;
                    if (mode == TOUCH_MODE_NONE && mCurrentShowingComment == null)
                        removeBubble();
                }

                @Override
                public void onAnimationRepeat(Animation arg0) { }

                @Override
                public void onAnimationStart(Animation arg0) {

                }});
            mCommentBubble.startAnimation(mBubbleAnimation);
        }
    }

    private void removeBubble(){
        if (mCommentBubble != null && mCommentBubble.getParent() != null){
            ((ViewGroup) mCommentBubble.getParent()).removeView(mCommentBubble);
        }
    }

    @Override
    protected void showCurrentComment(){
        showCurrentComment(false);
    }

    @Override
    protected void showCurrentComment(boolean waitForInteraction){
        if (mCommentBubble == null) return;

        if (mCurrentShowingComment != null){

            mPlayerAvatarBar.setCurrentComment(mCurrentShowingComment);
            mCommentLines.setCurrentComment(mCurrentShowingComment);

            mCommentBubble.comment_mode = CommentBubble.MODE_SHOW_COMMENT;
            mCommentBubble.show_comment = mCurrentShowingComment;
            mCommentBubble.interacted = !waitForInteraction;
            mCommentBubble.xPos = mCurrentShowingComment.xPos;
            mCommentBubble.yPos = mAvatarOffsetY;

            mShowBubbleForceAnimation = false;
            showBubble(mCommentBubble.update());

        }
    }

    protected void autoCloseComment(){
            if (mCommentBubble != null && mCurrentShowingComment != null)
            if (mCurrentShowingComment == mCommentBubble.mComment && !mCommentBubble.interacted){
                closeComment();
            }
    }

    public void closeComment(){
        mCurrentShowingComment = null;
        if (mPlayerAvatarBar != null) mPlayerAvatarBar.setCurrentComment(null);
        if (mCommentLines != null) mCommentLines.setCurrentComment(null);
        removeBubble();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            int[] calc = new int[2];

            mPlayer.getCommentHolder().getLocationInWindow(calc);
            int topOffset = calc[1];

            if (mCommentBubble == null) mCommentBubble = new CommentBubble(mPlayer, this);
            mCommentBubble.parentWidth = getWidth();

            if (mPlayerAvatarBar != null) {
                mPlayerAvatarBar.getLocationInWindow(calc);
                mAvatarOffsetY = calc[1] - topOffset;
            }

            if (mPlayerCommentBar != null) {
                mPlayerCommentBar.getLocationInWindow(calc);
                mCommentBarOffsetY = calc[1] - topOffset;
            }
        }
    }

     protected void processDownInput(InputObject input) {
        if (input.view == mPlayerAvatarBar) {
            if (mCommentBubble == null) return;
            if (mCurrentComments != null) {
                mode = TOUCH_MODE_AVATAR_DRAG;
                mCommentBubble.comment_mode = CommentBubble.MODE_SHOW_COMMENT;
                calcAvatarHit(input.x, true);
            }
        } else if (input.view == mPlayerCommentBar) {
            if (!mShowingComments) {
                queueCommentUnique(UI_TOGGLE_COMMENTS);
                return;
            }

            if (mCommentBubble == null) return;

            mode = TOUCH_MODE_COMMENT_DRAG;
            mCurrentShowingComment = null;
            mCommentBubble.new_comment_track = mPlayingTrack;
            mCommentBubble.new_comment_timestamp = stampFromPosition(input.x);
            mCommentBubble.comment_mode = CommentBubble.MODE_NEW_COMMENT;
            mCommentBubble.xPos = input.x;
            mCommentBubble.yPos = mCommentBarOffsetY;

            mShowBubble = true;
            mShowBubbleForceAnimation = true;

            queueCommentUnique(UI_UPDATE_BUBBLE);

        } else {
         super.processDownInput(input);
        }

    }

    protected void processMoveInput(InputObject input) {
        switch (mode) {
            case TOUCH_MODE_COMMENT_DRAG:
                mCommentBubble.new_comment_timestamp = stampFromPosition(input.x);
                mCommentBubble.xPos = input.x;
                queueCommentUnique(UI_UPDATE_BUBBLE_DISPLAY_NEW_COMMENT);
                break;

            case TOUCH_MODE_AVATAR_DRAG:
                calcAvatarHit(input.x, false);
                break;
        }
        super.processMoveInput(input);
    }

    protected void processUpInput(InputObject input) {
        switch (mode) {
            case TOUCH_MODE_COMMENT_DRAG:
                if (mBubbleAnimation == null) {
                    if (Math.abs(mPlayerCommentBar.getTop() - input.y) < 200) {
                        mAddComment = CloudUtils.buildComment(mPlayer, mPlayer.getCurrentUserId(),
                                mPlayingTrack.id, stampFromPosition(input.x), "", 0);
                        queueCommentUnique(UI_ADD_COMMENT);
                    }
                }
                mShowBubble = false;
                queueCommentUnique(UI_UPDATE_BUBBLE);
                break;
        }
        super.processUpInput(input);
    }

     private void calcAvatarHit(float xPos, boolean down){
        Comment skipComment = null;
        if (mCurrentShowingComment != null){
            if (isHitting(mCurrentShowingComment,xPos)){
                if (down)
                    skipComment = mCurrentShowingComment;
                else
                    return;
            } else {
                if (!mCommentBubble.closing){
                    mCommentBubble.interacted = false;
                    mHandler.postDelayed(mAutoCloseComment, 500);
                }
            }
        }
        Comment c = isHitting(xPos, skipComment);
        if (c != null){
            mCurrentShowingComment = c;
            mCommentBubble.show_comment = mCurrentShowingComment;
            mShowBubble = true;
            queueCommentUnique(UI_SHOW_CURRENT_COMMENT);
        } else if (skipComment == null){
            mCommentBubble.show_comment = null;
            mShowBubble = false;
            queueCommentUnique(UI_UPDATE_BUBBLE);
        }
    }

    private boolean isHitting(Comment c, float xPos){
        return (c.xPos < xPos && c.xPos + mPlayerAvatarBar.getAvatarWidth() > xPos);
    }

    private Comment isHitting(float xPos, Comment skipComment){
       for (int i = mCurrentTopComments.size()-1; i >= 0; i-- ){
          if (mCurrentTopComments.get(i).xPos > 0 && isHitting(mCurrentTopComments.get(i),xPos) && (skipComment == null || skipComment.xPos < mCurrentTopComments.get(i).xPos)){
                  return mCurrentTopComments.get(i);
          } else if (mCurrentTopComments.get(i).xPos > xPos)
              break;
       }

      return null;
    }

    protected void queueCommentUnique(int what){
        if (!mCommentHandler.hasMessages(what)) mCommentHandler.sendEmptyMessage(what);
    }



    Handler mCommentHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_UPDATE_BUBBLE:
                    if (mShowBubble)
                        showBubble(mCommentBubble.update());
                    else
                        removeBubble();
                    break;

                case UI_UPDATE_BUBBLE_DISPLAY_NEW_COMMENT:
                    mCommentBubble.updatePosition();
                    mCommentBubble.updateNewCommentTime();
                    break;
                case UI_SHOW_CURRENT_COMMENT:
                    showCurrentComment();
                    break;

                case UI_ADD_COMMENT:
                    mPlayer.addNewComment(mAddComment, mPlayer.addCommentListener);
                    break;

                case UI_TOGGLE_COMMENTS:
                    toggleComments();
                    break;

            }
        }
    };
}
