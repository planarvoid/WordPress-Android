package com.soundcloud.android.view;

import android.os.Handler;
import android.os.Message;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

import com.soundcloud.android.model.Comment;
import com.soundcloud.android.utils.InputObject;

public class WaveformControllerLand extends WaveformController {
    private static final int UI_SHOW_CURRENT_COMMENT = 3;
    private static final int UI_ADD_COMMENT = 4;
    private static final int UI_TOGGLE_COMMENTS = 5;

    private int mAvatarOffsetY, mCommentBarOffsetY;
    private CommentPanel mCommentPanel;

    private static final int COMMENT_ANIMATE_DURATION = 300;

    private boolean mCommentsVisible;

    private final Handler mCommentHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
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



    public WaveformControllerLand(Context context, AttributeSet attrs) {
        super(context, attrs);
        setStaticTransformationsEnabled(false);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            int[] calc = new int[2];

            mPlayer.getCommentHolder().getLocationInWindow(calc);
            int topOffset = calc[1];

            if (mPlayerAvatarBar != null) {
                mPlayerAvatarBar.getLocationInWindow(calc);
                mAvatarOffsetY = calc[1] - topOffset;
            }
        }
    }

     protected void processDownInput(InputObject input) {
        if (input.view == mPlayerAvatarBar) {
            if (mCurrentComments != null) {
                mode = TOUCH_MODE_AVATAR_DRAG;
                //mCommentBubble.comment_mode = CommentBubble.MODE_SHOW_COMMENT;
                calcAvatarHit(input.x, true);
            }
        } else {
         super.processDownInput(input);
        }
    }

    private void calcAvatarHit(float xPos, boolean down) {
        Comment skipComment = null;
        if (mCurrentShowingComment != null) {
            if (isHitting(mCurrentShowingComment, xPos)) {
                if (down)
                    skipComment = mCurrentShowingComment;
                else
                    return;
            } else {
                if (mCommentPanel != null && !mCommentPanel.closing) {
                    mCommentPanel.interacted = false;
                    mHandler.postDelayed(mAutoCloseComment, 100);
                }
            }
        }
        Comment c = isHitting(xPos, skipComment);
        if (c != null) {
            mCurrentShowingComment = c;
            queueCommentUnique(UI_SHOW_CURRENT_COMMENT);
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

    void queueCommentUnique(int what){
        if (!mCommentHandler.hasMessages(what)) mCommentHandler.sendEmptyMessage(what);
    }

    protected void processMoveInput(InputObject input) {
        switch (mode) {
            case TOUCH_MODE_AVATAR_DRAG:
                calcAvatarHit(input.x, false);
                break;
        }
        super.processMoveInput(input);
    }

    public void showConnectingLayout() {
        mWaveformHolder.showConnectingLayout(false);
        invalidate();
    }

    @Override
    protected void showCurrentComment(){
        showCurrentComment(false);
    }

     protected void showCurrentComment(boolean waitForInteraction) {


        if (mCurrentShowingComment != null) {
            mPlayerAvatarBar.setCurrentComment(mCurrentShowingComment);
            mCommentLines.setCurrentComment(mCurrentShowingComment);

            if (mCommentPanel == null){
                mCommentPanel = new CommentPanel(mPlayer, true);
                mCommentPanel.setControllers(mPlayer, this);
                mCommentPanel.interacted = !waitForInteraction;
                mCurrentCommentPanel = mCommentPanel;
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        LayoutParams.FILL_PARENT, mWaveformHolder.getHeight()/2);
                lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                addView(mCommentPanel, lp);

                toggleCommentVisibility(true, true);
            }

            mCommentPanel.showComment(mCurrentShowingComment);
        }
    }

    private void toggleCommentVisibility(boolean visible, boolean animatePanel){

        if (visible && !mCommentsVisible) {
            mCommentsVisible = true;
            Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            animation.setFillEnabled(true);
            animation.setFillAfter(true);
            animation.setDuration(COMMENT_ANIMATE_DURATION);
            mWaveformHolder.startAnimation(animation);
            if (animatePanel && mCommentPanel != null) {
                animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f);
                animation.setDuration(COMMENT_ANIMATE_DURATION);
                mCommentPanel.startAnimation(animation);
            }
        } else if (!visible && mCommentsVisible) {
            mCommentsVisible = false;
            Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.0f);
            animation.setFillEnabled(true);
            animation.setFillAfter(true);
            animation.setDuration(COMMENT_ANIMATE_DURATION);
            mWaveformHolder.startAnimation(animation);

            if (mCommentPanel != null) {
                if (mCommentPanel.getParent() == this && animatePanel) {
                    animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                            Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                            Animation.RELATIVE_TO_SELF, -1.0f);
                    animation.setDuration(COMMENT_ANIMATE_DURATION);
                    mCommentPanel.setAnimation(animation);
                    removeView(mCommentPanel);
                }
                mCommentPanel = null;
            }

        }
    }

    @Override
    public void closeComment() {
        super.closeComment();
        toggleCommentVisibility(false, true);
    }
}
