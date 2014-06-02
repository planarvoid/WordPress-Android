package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.utils.InputObject;

import android.content.Context;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

import java.lang.ref.WeakReference;

public class WaveformControllerLayoutLand extends WaveformControllerLayout {
    private final AccountOperations accountOperations;
    private boolean shouldShowComments;
    private CommentPanelLayout commentPanel;

    private static final String PLAYER_SHOWING_COMMENTS = "playerShowingComments";
    private static final int COMMENT_ANIMATE_DURATION = 500;
    private static final long MAX_AUTO_COMMENT_DISPLAY_TIME = 30000;

    private boolean waveformHalf;
    private boolean commentPanelVisible;

    public WaveformControllerLayoutLand(Context context, AttributeSet attrs) {
        super(context, attrs);
        setStaticTransformationsEnabled(false);
        accountOperations = SoundCloudApplication.fromContext(context).getAccountOperations();
        shouldShowComments = accountOperations.getAccountDataBoolean(PLAYER_SHOWING_COMMENTS);

    }

    private static final class CommentHandler extends Handler {

        private WeakReference<WaveformControllerLayout> mRef;

        private CommentHandler(WaveformControllerLayout controller) {
            this.mRef = new WeakReference<WaveformControllerLayout>(controller);
        }

        @Override
        public void handleMessage(Message msg) {
            final WaveformControllerLayout controller = mRef.get();
            if (controller != null) {
                switch (msg.what) {
                    case UI_SHOW_CURRENT_COMMENT:
                        controller.showCurrentComment(true);
                        break;
                }
            }
        }
    };

    private CommentHandler commentHandler = new CommentHandler(this);

    @Override
    protected boolean isLandscape() {
        return true;
    }

    protected void processDownInput(InputObject input) {
        if (input.view == playerAvatarBar && mode == TOUCH_MODE_NONE) {
            if (currentComments != null) {
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
        if (currentShowingComment != null) {
            if (isHitting(currentShowingComment, xPos)) {
                if (down)
                    skipComment = currentShowingComment;
                else
                    return;
            } else {
                if (commentPanel != null && !commentPanel.closing) {
                    commentPanel.interacted = false;
                    handler.postDelayed(mAutoCloseComment, 500);
                }
            }
        }
        Comment c = isHitting(xPos, skipComment);
        if (c != null) {
            currentShowingComment = c;
            showComment = true;
            queueCommentUnique(UI_SHOW_CURRENT_COMMENT);
        } else if (skipComment == null && commentPanel != null){
            showComment = false;
            queueUnique(UI_UPDATE_COMMENT);
        }
    }

    private boolean isHitting(Comment c, float xPos){
        return (c.xPos < xPos && c.xPos + playerAvatarBar.getAvatarWidth() > xPos);
    }

    private Comment isHitting(float xPos, Comment skipComment) {
        for (int i = currentTopComments.size() - 1; i >= 0; i--) {
            if (currentTopComments.get(i).xPos > 0 &&
                    isHitting(currentTopComments.get(i), xPos) &&
                    (skipComment == null || skipComment.xPos < currentTopComments.get(i).xPos)) {
                return currentTopComments.get(i);
            } else if (currentTopComments.get(i).xPos > xPos)
                break;
        }
        return null;
    }

    void queueCommentUnique(int what){
        if (!commentHandler.hasMessages(what)) commentHandler.sendEmptyMessage(what);
    }

    protected void processMoveInput(InputObject input) {
        switch (mode) {
            case TOUCH_MODE_AVATAR_DRAG:
                calcAvatarHit(input.x, false);
                break;
        }
        super.processMoveInput(input);
    }

    protected void showCurrentComment(boolean userTriggered) {
       if (currentShowingComment != null) {
           cancelAutoCloseComment();
           playerAvatarBar.setCurrentComment(currentShowingComment);
           commentLines.setCurrentComment(currentShowingComment);

           if (commentPanel == null) {
               commentPanel = new CommentPanelLayout(getContext(), imageOperations, true);
               commentPanel.setListener(this);
               commentPanel.interacted = userTriggered;
               currentCommentPanel = commentPanel;
               RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,
                       (int) (waveformHolder.getHeight() / 2 + (getResources().getDisplayMetrics().density * 10)));
               lp.bottomMargin = (int) -(getResources().getDisplayMetrics().density * 10);
               lp.addRule(ALIGN_PARENT_TOP);
               addView(commentPanel, lp);
           }

           toggleWaveformHalf(true);
           toggleCommentsPanelVisibility(true);
           commentPanel.showComment(currentShowingComment);
       }
    }

    private void toggleWaveformHalf(boolean half){
        if (half && !waveformHalf) {
            waveformHalf = true;
            if (waveformHolder.getAnimation() != null && !waveformHolder.getAnimation().hasEnded()){
                //calculate new distances and durations based on unfinished animation
                final float transY =getAnimationTransY(waveformHolder.getAnimation());
                final long duration = (long) Math.max(0, (waveformHolder.getHeight() / 2 - transY) / (waveformHolder.getHeight() / 2) * COMMENT_ANIMATE_DURATION);

                waveformHolder.clearAnimation();
                Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, waveformHolder.getTop() + transY,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                animation.setDuration(duration);
                animation.setFillEnabled(true);
                animation.setFillAfter(true);
                waveformHolder.startAnimation(animation);
            } else {
                 Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                animation.setFillEnabled(true);
                animation.setFillAfter(true);
                animation.setDuration(COMMENT_ANIMATE_DURATION);
                waveformHolder.startAnimation(animation);
            }

        } else if (!half && waveformHalf) {
            if (waveformHolder.getAnimation() != null){
                waveformHolder.clearAnimation();
            }
            waveformHalf = false;
            Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.0f);
            animation.setFillEnabled(true);
            animation.setFillAfter(true);
            animation.setDuration(COMMENT_ANIMATE_DURATION);
            waveformHolder.startAnimation(animation);
        }
    }

    private void toggleCommentsPanelVisibility(boolean visible) {

        if (visible && !commentPanelVisible) {
            commentPanelVisible = true;
            clearDisappearingChildren();
            cancelAutoCloseComment();

            if (commentPanel.getParent() != this) addView(commentPanel);

            if (commentPanel.getAnimation() != null && ! commentPanel.getAnimation().hasEnded()) {
                //calculate new distances and durations based on unfinished animation
                final float transY =getAnimationTransY(commentPanel.getAnimation());
                final long duration = (long) (-transY/(commentPanel.getHeight()) * COMMENT_ANIMATE_DURATION);

                commentPanel.clearAnimation();
                Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, commentPanel.getTop() + transY,
                        Animation.RELATIVE_TO_SELF, 0.0f);
                animation.setDuration(duration);
                commentPanel.startAnimation(animation);
            } else {
                Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f);
                animation.setDuration(COMMENT_ANIMATE_DURATION);
                commentPanel.startAnimation(animation);
            }

        } else if (!visible && commentPanelVisible && commentPanel.getParent() == this) {
            if (commentPanel.getAnimation() != null) {
                commentPanel.getAnimation().cancel();
            }
            commentPanelVisible = false;
            Animation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, -1.0f);
            animation.setDuration(COMMENT_ANIMATE_DURATION);
            commentPanel.setAnimation(animation);
            removeView(commentPanel);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed){
            currentTimeDisplay.setCommentingHeight(waveformHolder.getHeight()/2);
        }
    }

    private float getAnimationTransY(Animation a) {
        Transformation outTransformation = new Transformation();
        a.getTransformation(waveformHolder.getDrawingTime(), outTransformation);

        Matrix transformationMatrix = outTransformation.getMatrix();
        float[] matrixValues = new float[9];
        transformationMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MTRANS_Y];
    }

    @Override
    public void closeComment(boolean userTriggered) {
        if (userTriggered && currentShowingComment == lastAutoComment && shouldShowComments) {
            toggleShowingComments();
        }

        currentShowingComment = null;
        if (playerAvatarBar != null) playerAvatarBar.setCurrentComment(null);
        if (commentLines != null) commentLines.setCurrentComment(null);

        toggleWaveformHalf(false);
        toggleCommentsPanelVisibility(false);
    }

    @Override
    public void setCommentMode(boolean commenting) {
        if (commenting){
            toggleWaveformHalf(true);
            toggleCommentsPanelVisibility(false);
            ((RelativeLayout.LayoutParams) currentTimeDisplay.getLayoutParams()).addRule(ALIGN_PARENT_TOP);
            ((RelativeLayout.LayoutParams) currentTimeDisplay.getLayoutParams()).addRule(ABOVE,0);
            currentTimeDisplay.setRoundTop(false);
        } else {
            toggleWaveformHalf(false);
            ((RelativeLayout.LayoutParams) currentTimeDisplay.getLayoutParams()).addRule(ALIGN_PARENT_TOP, 0);
            ((RelativeLayout.LayoutParams) currentTimeDisplay.getLayoutParams()).addRule(ABOVE, R.id.player_avatar_bar_holder);
            currentTimeDisplay.setRoundTop(true);
        }
        currentTimeDisplay.setCommenting(commenting);
        super.setCommentMode(commenting);
    }

    @Override
    public void resetCommentDisplay(){
        if (commentPanel != null && commentPanel.getParent() == this) {
            if (commentPanel.getAnimation() != null) {
                commentPanel.getAnimation().cancel();
            }
            commentPanelVisible = false;
            removeView(commentPanel);
        }
        waveformHolder.clearAnimation();
    }

     @Override
     protected void autoShowComment(Comment c) {
         if (shouldShowComments) {

            cancelAutoCloseComment();
            currentShowingComment = c;
            showCurrentComment(true);

            final Comment nextComment = nextCommentAfterTimestamp(currentShowingComment.timestamp);
            if (nextComment != null) prefetchAvatar(nextComment);

            if (nextComment == null || nextComment.timestamp - c.timestamp > MAX_AUTO_COMMENT_DISPLAY_TIME) {
                handler.postDelayed(mAutoCloseComment, CLOSE_COMMENT_DELAY);
            }
        }
    }

    private void toggleShowingComments() {
        shouldShowComments = !shouldShowComments;
        accountOperations.setAccountData(PLAYER_SHOWING_COMMENTS, Boolean.toString(shouldShowComments));
    }
}
