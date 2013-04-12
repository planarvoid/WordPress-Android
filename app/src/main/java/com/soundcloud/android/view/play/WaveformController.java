package com.soundcloud.android.view.play;


import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.cache.WaveformCache;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.WaveformData;
import com.soundcloud.android.utils.InputObject;
import com.soundcloud.android.view.TouchLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WaveformController extends TouchLayout {
    private static final String TAG = WaveformController.class.getSimpleName();

    protected static final long CLOSE_COMMENT_DELAY = 5000;
    private static final int OVERLAY_BG_COLOR = Color.WHITE;

    protected @Nullable PlayerAvatarBar mPlayerAvatarBar;

    private View mOverlay;
    protected ProgressBar mProgressBar;
    protected WaveformHolder mWaveformHolder;
    protected RelativeLayout mWaveformFrame;
    private PlayerTouchBar mPlayerTouchBar;
    protected @Nullable WaveformCommentLines mCommentLines;
    protected PlayerTime mCurrentTimeDisplay;

    protected @NotNull ScPlayer mPlayer;
    protected @Nullable Track mTrack;
    protected int mQueuePosition;

    protected boolean mSuspendTimeDisplay, mOnScreen;
    protected @Nullable List<Comment> mCurrentComments;
    protected List<Comment> mCurrentTopComments;
    protected @Nullable Comment mCurrentShowingComment;

    private WaveformState mWaveformState;

    protected @Nullable CommentPanel mCurrentCommentPanel;

    protected Comment mAddComment;
    protected Comment mLastAutoComment;

    private int mWaveformErrorCount, mDuration;
    private float mSeekPercent;

    protected final Handler mHandler = new Handler();
    private Handler mTouchHandler = new TouchHandler(this);

    private static final int MAX_WAVEFORM_RETRIES = 2;

    private static final int UI_UPDATE_SEEK = 1;
    private static final int UI_SEND_SEEK   = 2;
    private static final int UI_UPDATE_COMMENT_POSITION = 3;
    protected static final int UI_ADD_COMMENT = 4;
    protected static final int UI_UPDATE_COMMENT = 5;
    protected static final int UI_CLEAR_SEEK = 6;
    // used by landscape
    protected static final int UI_SHOW_CURRENT_COMMENT = 7;
    protected static final int UI_TOGGLE_COMMENTS = 8;

    static final int TOUCH_MODE_NONE = 0;
    static final int TOUCH_MODE_SEEK_DRAG = 1;
    static final int TOUCH_MODE_COMMENT_DRAG = 2;
    static final int TOUCH_MODE_AVATAR_DRAG = 3;
    static final int TOUCH_MODE_SEEK_CLEAR_DRAG = 4;

    protected int mode = TOUCH_MODE_NONE;

    private @Nullable PlayerTrackView mPlayerTrackView;
    protected boolean mShowComment;
    private static final long MIN_COMMENT_DISPLAY_TIME = 2000;

    // only allow smooth progress updates on 9 or greater because they have buffering events for proper displaying
    public static final int MINIMUM_SMOOTH_PROGRESS_SDK = 9;
    private static final long MINIMUM_PROGRESS_PERIOD = 40;
    private boolean mShowingSmoothProgress;
    private boolean mIsBuffering, mWaitingForSeekComplete;
    private int mTouchSlop;
    private int mWaveformColor;


    public WaveformController(Context context, AttributeSet attrs) {
        super(context, attrs);

        setWillNotDraw(false);

        mPlayer = (ScPlayer) context;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.wave_form_controller, this);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        mWaveformFrame = (RelativeLayout) findViewById(R.id.waveform_frame);
        mWaveformHolder = (WaveformHolder) findViewById(R.id.waveform_holder);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mCurrentTimeDisplay = (PlayerTime) findViewById(R.id.currenttime);

        mWaveformColor = mPlayer.getResources().getColor(R.color.playerControlBackground);
        mOverlay = findViewById(R.id.progress_overlay);
        mOverlay.setBackgroundColor(OVERLAY_BG_COLOR);

        mPlayerTouchBar = (PlayerTouchBar) findViewById(R.id.track_touch_bar);
        mPlayerAvatarBar = (PlayerAvatarBar) findViewById(R.id.player_avatar_bar);
        mPlayerAvatarBar.setIsLandscape(isLandscape());

        mCommentLines = new WaveformCommentLines(mPlayer, null);
        mWaveformHolder.addView(mCommentLines);

        mCurrentTimeDisplay.setVisibility(View.INVISIBLE);
        mPlayerTouchBar.setLandscape(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        // override default touch layout functionality, these views are all we want to respond
        mPlayerTouchBar.setOnTouchListener(this);
        mPlayerAvatarBar.setOnTouchListener(this);
        setOnTouchListener(null);
    }

    public void setOnScreen(boolean onScreen){
        mOnScreen = onScreen;
    }

    protected boolean isLandscape(){
        return false;
    }

    private long mProgressPeriod = 500;
    private long lastProgressTimestamp;
    private long lastTrackTime;

    private Runnable mSmoothProgress = new Runnable() {
        public void run() {
            setProgressInternal(lastTrackTime + System.currentTimeMillis() - lastProgressTimestamp);
            mHandler.postDelayed(this, mProgressPeriod);
        }
    };

    public void setPlaybackStatus(boolean isPlaying, long pos){
        if (mShowingSmoothProgress != isPlaying){
            stopSmoothProgress();
            setProgress(pos);
            if (Build.VERSION.SDK_INT >= MINIMUM_SMOOTH_PROGRESS_SDK && isPlaying && mProgressPeriod < ScPlayer.REFRESH_DELAY){
                startSmoothProgress();
            }
        }
        if (!isPlaying) {
            mWaitingForSeekComplete = false;
            setBufferingState(false);
        }
    }

    private void startSmoothProgress(){
        mShowingSmoothProgress = true;
        mHandler.postDelayed(mSmoothProgress, 0);
    }

    private void stopSmoothProgress(){
        mShowingSmoothProgress = false;
        mHandler.removeCallbacks(mSmoothProgress);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && getWidth()  > 0) {
            if (mCurrentComments != null && mDuration > 0) {
                for (Comment c : mCurrentComments) {
                    c.calculateXPos(getWidth(), mDuration);
                }
            }
            if (mTrack != null) {
                determineProgressInterval();
            }
        }
    }

    public void reset(boolean hide){
        mWaitingForSeekComplete = mIsBuffering = false;
        setProgressInternal(0);
        setSecondaryProgress(0);
        onStop(false);

        if (hide){
            showWaiting();
            mOverlay.setVisibility(View.INVISIBLE);
            mCurrentTimeDisplay.setVisibility(View.INVISIBLE);
        }
    }

     public void onStop(boolean killLoading) {
         // timed events
         stopSmoothProgress();
        cancelAutoCloseComment();

         // comment states
        if (mPlayerAvatarBar != null) mPlayerAvatarBar.setCurrentComment(null);
        if (mCommentLines != null) mCommentLines.setCurrentComment(null);
        mLastAutoComment = null;
        mCurrentShowingComment = null;
        resetCommentDisplay();

         //only performed on activity stop
         if (mPlayerAvatarBar != null && killLoading) mPlayerAvatarBar.onStop();
    }

    public void resetCommentDisplay(){
        if (mCurrentCommentPanel != null) {
            if (mCurrentCommentPanel.getAnimation() != null){
                mCurrentCommentPanel.getAnimation().cancel();
                mCurrentCommentPanel.clearAnimation();
            }
            if (mCurrentCommentPanel.getParent() == mWaveformFrame){
                mWaveformFrame.removeView(mCurrentCommentPanel);
            }
            mCurrentCommentPanel = null;
        }
    }

    public void setBufferingState(boolean isBuffering) {
        mIsBuffering = isBuffering;
        if (mIsBuffering){
            stopSmoothProgress();
            showWaiting();
        } else if (mWaveformState != WaveformState.LOADING && !mWaitingForSeekComplete){
            hideWaiting();
        }
    }

    public void onSeek(long seekTime){
        setProgressInternal(seekTime);
        setProgress(seekTime);
        stopSmoothProgress();
        mWaitingForSeekComplete = true;
        mHandler.postDelayed(mShowWaiting,500);
    }

    public void onSeekComplete(){
        stopSmoothProgress();
        mWaitingForSeekComplete = false;
        if (mWaveformState != WaveformState.LOADING) {
            hideWaiting();
        }
    }

    private void showWaiting() {
        mWaveformHolder.showWaitingLayout(true);
        mHandler.removeCallbacks(mShowWaiting);
        invalidate();
    }

    private void hideWaiting() {
        mHandler.removeCallbacks(mShowWaiting);
        mWaveformHolder.hideWaitingLayout();
        invalidate();

    }

    public void setCommentMode(boolean commenting) {
        if (commenting) {
            if (mCurrentShowingComment != null && !isLandscape()) {
                closeComment(false);
            }
            mSuspendTimeDisplay = true;
            mode = TOUCH_MODE_COMMENT_DRAG;
            mPlayerTouchBar.setSeekPosition((int) ((((float) lastTrackTime) / mDuration) * getWidth()), mPlayerTouchBar.getHeight(), true);
            mCurrentTimeDisplay.setByPercent((((float) lastTrackTime) / mDuration), true);
        } else {
            mSuspendTimeDisplay = false;
            mode = TOUCH_MODE_NONE;
            mPlayerTouchBar.clearSeek();
            setCurrentTime(lastTrackTime);
        }
    }

    public void setProgress(long pos) {
        if (pos < 0) return;

        lastProgressTimestamp = System.currentTimeMillis();
        lastTrackTime = pos;
        if (mode != TOUCH_MODE_SEEK_DRAG){
            setProgressInternal(pos);
        }
    }

    protected void setProgressInternal(long pos) {
        if (mDuration <= 0)
            return;

        mProgressBar.setProgress((int) (pos * 1000 / mDuration));
        if (mode != TOUCH_MODE_SEEK_DRAG) {
            setCurrentTime(pos);
        }

        if (mode == TOUCH_MODE_NONE && mCurrentTopComments != null) {
            final Comment last = lastCommentBeforeTimestamp(pos);
            if (last != null) {
                if (mLastAutoComment != last && pos - last.timestamp < 2000) {
                    if (mPlayerTrackView != null
                            && mPlayerTrackView.waveformVisible()
                            && (mCurrentShowingComment == null ||
                                    (mCurrentShowingComment == mLastAutoComment &&
                                    last.timestamp - mLastAutoComment.timestamp > MIN_COMMENT_DISPLAY_TIME))) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                autoShowComment(last);
                            }
                        });
                        mLastAutoComment = last;
                    }
                }
            }
        }
    }

    private void setCurrentTime(final long pos){
        if (mode != TOUCH_MODE_SEEK_DRAG && !mSuspendTimeDisplay) {
            if (getWidth() == 0) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        mCurrentTimeDisplay.setCurrentTime(pos, false);
                    }
                });
            } else {
                mCurrentTimeDisplay.setCurrentTime(pos, false);
            }
        }
    }

    public void setSmoothProgress(boolean showSmoothProgress){
        if (mShowingSmoothProgress != showSmoothProgress){
            mShowingSmoothProgress = showSmoothProgress;
            if (mShowingSmoothProgress){
                startSmoothProgress();
            } else {
                stopSmoothProgress();
            }
        }
    }

    protected void autoShowComment(Comment c) {
        autoCloseComment();
        cancelAutoCloseComment();

        mCurrentShowingComment = c;
        showCurrentComment(false);

        final Comment nextComment = nextCommentAfterTimestamp(mCurrentShowingComment.timestamp);
        if (nextComment != null) nextComment.prefetchAvatar(getContext());
        mHandler.postDelayed(mAutoCloseComment, CLOSE_COMMENT_DELAY);
    }

    public void setSecondaryProgress(int percent) {
        mProgressBar.setSecondaryProgress(percent);
    }

    private void determineProgressInterval(){
        if (getWidth() == 0) return;
        mProgressPeriod = Math.max(MINIMUM_PROGRESS_PERIOD,mDuration/getWidth());
        if (mProgressPeriod >= ScPlayer.REFRESH_DELAY){
            // don't bother with the extra refreshes, will happen at the regular intervals anyways
            stopSmoothProgress();
        }
    }

    public void updateTrack(@Nullable Track track, int queuePosition, boolean visibleNow) {
        mQueuePosition = queuePosition;
        if (track == null || (mTrack != null
                && mTrack.id == track.id
                && mWaveformState != WaveformState.ERROR
                && mDuration == mTrack.duration)) {
            return;
        }

        final boolean changed = mTrack != track;
        mTrack = track;
        mDuration = mTrack.duration;
        mCurrentTimeDisplay.setDuration(mDuration);

        if (changed) {
            hideWaiting();
            stopSmoothProgress();
            determineProgressInterval();
        }

        if (!track.hasWaveform()) {
            Log.w(TAG, "track " + track.title + " has no waveform");
            mWaveformState = WaveformState .ERROR;
            mOverlay.setBackgroundColor(OVERLAY_BG_COLOR);
            onDoneLoadingWaveform(false, false);
            return;
        }

        if (WaveformCache.get().getData(track, new WaveformCache.WaveformCallback() {
            @Override
            public void onWaveformDataLoaded(Track track, WaveformData data, boolean fromCache) {
                if (track.equals(mTrack)) {
                    mWaveformErrorCount = 0;
                    mWaveformState = WaveformState.OK;
                    mOverlay.setBackgroundDrawable(new WaveformDrawable(data, mWaveformColor, !isLandscape()));
                    onDoneLoadingWaveform(true, !fromCache && mOnScreen);
                }
            }
            @Override
            public void onWaveformError(Track track) {
                if (track.equals(mTrack)) {
                    mWaveformState = WaveformState.ERROR;
                    WaveformController.this.onWaveformError();
                }
            }

        }) == null) {
            // loading
            showWaiting();
            mWaveformState = WaveformState.LOADING;
            mOverlay.setVisibility(View.INVISIBLE);
            mCurrentTimeDisplay.setVisibility(View.INVISIBLE);
        }
    }

    protected void showCurrentComment(boolean userTriggered) {
        if (mCurrentShowingComment != null) {
            mPlayerAvatarBar.setCurrentComment(mCurrentShowingComment);
            mCommentLines.setCurrentComment(mCurrentShowingComment);

            CommentPanel commentPanel = new CommentPanel(mPlayer, false);
            commentPanel.setControllers(mPlayer, this);
            commentPanel.showComment(mCurrentShowingComment);
            commentPanel.interacted = userTriggered;
            mCurrentCommentPanel = commentPanel;
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ABOVE, mWaveformHolder.getId());
            lp.bottomMargin = (int) -(getResources().getDisplayMetrics().density * 10);
            mWaveformFrame.addView(commentPanel, mWaveformFrame.indexOfChild(mCurrentTimeDisplay), lp);

            AnimationSet set = new AnimationSet(true);
            Animation animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration(300);
            set.addAnimation(animation);

            animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.2f,
                    Animation.RELATIVE_TO_SELF, 0.0f);
            animation.setDuration(300);
            set.addAnimation(animation);
            commentPanel.startAnimation(set);
        }
    }

    public void closeComment(boolean userTriggered) {

        mCurrentShowingComment = null;
        if (mPlayerAvatarBar != null) mPlayerAvatarBar.setCurrentComment(null);
        if (mCommentLines != null) mCommentLines.setCurrentComment(null);

        if (mCurrentCommentPanel != null) {
            Animation animation = new AlphaAnimation(1.0f, 0.0f);
            animation.setDuration(300);
            mCurrentCommentPanel.setAnimation(animation);
            mWaveformFrame.removeView(mCurrentCommentPanel);
            mCurrentCommentPanel = null;
        }
    }

    protected void autoCloseComment() {
        if (mCurrentCommentPanel != null && mCurrentShowingComment != null)
            if (mCurrentShowingComment == mCurrentCommentPanel.getComment() && !mCurrentCommentPanel.interacted) {
                closeComment(false);
            }
    }

    protected void cancelAutoCloseComment() {
        mHandler.removeCallbacks(mAutoCloseComment);
    }

    final Runnable mShowWaiting = new Runnable() {
        public void run() {
            showWaiting();
        }
    };

    final Runnable mAutoCloseComment = new Runnable() {
        public void run() {
            autoCloseComment();
        }
    };

    public void nextCommentInThread() {
        if (mCurrentShowingComment != null && mCurrentShowingComment.nextComment != null) {
            final Comment nextComment = mCurrentShowingComment.nextComment;
            if (!isLandscape()) closeComment(false);
            mCurrentShowingComment = nextComment;
            showCurrentComment(true);
        }
    }

    public void clearTrackComments() {
        cancelAutoCloseComment();
        closeComment(false);

        if (mPlayerAvatarBar != null) {
            mPlayerAvatarBar.setVisibility(View.INVISIBLE);
            mPlayerAvatarBar.clearTrackData();
        }
        if (mCommentLines != null) {
            mCommentLines.setVisibility(View.INVISIBLE);
            mCommentLines.clearTrackData();
        }

        mCurrentComments = null;
        mCurrentTopComments = null;
        if (mode == TOUCH_MODE_AVATAR_DRAG) mode = TOUCH_MODE_NONE;
    }

    private void onWaveformError() {
        mWaveformErrorCount++;
        if (mWaveformErrorCount < MAX_WAVEFORM_RETRIES) {
            updateTrack(mTrack, mQueuePosition, mOnScreen);
        } else {
            mOverlay.setBackgroundColor(OVERLAY_BG_COLOR);
            onDoneLoadingWaveform(false, mOnScreen);
        }
    }


    private void onDoneLoadingWaveform(boolean success, boolean animate) {
        if (!mIsBuffering) hideWaiting();

        final AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
        aa.setDuration(500);

        // only show the image if the load was successful, otherwise it will obscure the progress
        if (success && mOverlay.getVisibility() != View.VISIBLE) {
            if (animate) mOverlay.startAnimation(aa);
            mOverlay.setVisibility(View.VISIBLE);
        }

        if (mCurrentTimeDisplay.getVisibility() != View.VISIBLE) {
            if (animate) mCurrentTimeDisplay.startAnimation(aa);
            mCurrentTimeDisplay.setVisibility(View.VISIBLE);
        }
    }


    private @Nullable Comment lastCommentBeforeTimestamp(long timestamp) {
        for (Comment comment : mCurrentTopComments)
            if (comment.timestamp < timestamp)
                return comment;

        return null;
    }

    protected @Nullable Comment nextCommentAfterTimestamp(long timestamp) {
        if (mCurrentTopComments != null) {
            for (int i = mCurrentTopComments.size() - 1; i >= 0; i--) {
                if (mCurrentTopComments.get(i).timestamp > timestamp)
                    return mCurrentTopComments.get(i);
            }
        }
        return null;
    }


    public void setComments(List<Comment> comments, boolean animateIn) {
        setComments(comments, animateIn, false);
    }

    public void setComments(List<Comment> comments, boolean animateIn, boolean forceRefresh) {
        if (comments.equals(mCurrentComments) && !forceRefresh){
            return;
        }
        mCurrentComments = comments;
        mCurrentTopComments = getTopComments(comments, mDuration);

        if (mPlayerAvatarBar != null) {
            mPlayerAvatarBar.setTrackData(mDuration, comments);
            mPlayerAvatarBar.invalidate();
        }

        if (mCommentLines != null) {
            mCommentLines.setTrackData(mDuration, comments);
            mCommentLines.invalidate();
        }

        if (mPlayerAvatarBar != null && mPlayerAvatarBar.getVisibility() == View.INVISIBLE) {
            if (animateIn) {
                AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
                aa.setStartOffset(500);
                aa.setDuration(500);

                mPlayerAvatarBar.startAnimation(aa);
                mCommentLines.startAnimation(aa);
            }

            mPlayerAvatarBar.setVisibility(View.VISIBLE);
            mCommentLines.setVisibility(View.VISIBLE);
        }
    }

    private List<Comment> getTopComments(List<Comment> comments, int duration) {
        List<Comment> topComments = new ArrayList<Comment>();
        Collections.sort(comments, Comment.CompareTimestamp.INSTANCE);
        for (int i = 0; i < comments.size(); i++) {
            final Comment comment = comments.get(i);

            if (comment.timestamp > 0 && (i == comments.size() - 1 || comment.timestamp != comments.get(i + 1).timestamp)) {
                comment.topLevelComment = true;
                topComments.add(comment);
            } else if (comment.timestamp > 0) {
                comments.get(i + 1).nextComment = comment;
            }
            if (getWidth() == 0 && duration <= 0) {
                comment.xPos = -1;
            } else if (comment.xPos == -1 && duration > 0) {
                comment.calculateXPos(getWidth(), duration);
            }
        }
        return topComments;
    }


    @Override
    protected void processDownInput(InputObject input) {
        if (mode == TOUCH_MODE_COMMENT_DRAG) {
            mSeekPercent = ((float) input.x) / mWaveformHolder.getWidth();
            queueUnique(UI_UPDATE_COMMENT_POSITION);
        } else if (input.view == mPlayerTouchBar && mPlayer.isSeekable()) {
            mode = TOUCH_MODE_SEEK_DRAG;
            if (mPlayer.isSeekable()) {
                mLastAutoComment = null; //reset auto comment in case they seek backward
                mSeekPercent = ((float) input.x) / mWaveformHolder.getWidth();
                queueUnique(UI_UPDATE_SEEK);
            }
        }
    }

    @Override
    protected void processMoveInput(InputObject input) {
        switch (mode) {
            case TOUCH_MODE_COMMENT_DRAG:
                if (isOnTouchBar(input.y)) {
                    mSeekPercent = ((float) input.x) / mWaveformHolder.getWidth();
                    queueUnique(UI_UPDATE_COMMENT_POSITION);
                }
                break;
            case TOUCH_MODE_SEEK_DRAG:
                if (isOnTouchBar(input.y)) {
                    mSeekPercent = ((float) input.x) / mWaveformHolder.getWidth();
                    queueUnique(UI_UPDATE_SEEK);
                } else {
                    queueUnique(UI_CLEAR_SEEK);
                    mode = TOUCH_MODE_SEEK_CLEAR_DRAG;
                }
                break;

            case TOUCH_MODE_SEEK_CLEAR_DRAG:
                if (isOnTouchBar(input.y)) {
                    mSeekPercent = ((float) input.x) / mWaveformHolder.getWidth();
                    queueUnique(UI_UPDATE_SEEK);
                    mode = TOUCH_MODE_SEEK_DRAG;
                }
                break;
        }
    }

    @Override
    protected void processUpInput(InputObject input) {
        switch (mode) {
            case TOUCH_MODE_COMMENT_DRAG:
                if (isOnTouchBar(input.y)) {
                    mAddComment = Comment.build(
                            mTrack,
                            mPlayer.getApp().getLoggedInUser(),
                            stampFromPosition(input.x),
                            "",
                            0,
                            "");
                    queueUnique(UI_ADD_COMMENT);
                } else return;

                break;
            case TOUCH_MODE_SEEK_DRAG:
            case TOUCH_MODE_SEEK_CLEAR_DRAG:
                if (isOnTouchBar(input.y)) {
                    queueUnique(UI_SEND_SEEK);
                } else {
                    queueUnique(UI_CLEAR_SEEK);
                }
                break;
        }
        mode = TOUCH_MODE_NONE;
    }

    @Override
    protected void processPointer1DownInput(InputObject input) {
    }

    @Override
    protected void processPointer1UpInput(InputObject input) {
    }

    private boolean isOnTouchBar(int y){
        return (y > mPlayerTouchBar.getTop() - mTouchSlop && y < mPlayerTouchBar.getBottom() + mTouchSlop);
    }

    protected void queueUnique(int what) {
        if (!mTouchHandler.hasMessages(what)) mTouchHandler.sendEmptyMessage(what);
    }

    public void onDestroy() {
        super.onDestroy();

        if (mCurrentComments != null) {
            for (Comment c : mCurrentComments) {
                c.xPos = -1;
            }
        }
        mPlayerAvatarBar.clearTrackData();
        mCommentLines.clearTrackData();
    }

    protected long stampFromPosition(int x) {
        return (long) (Math.min(Math.max(.001, (((float) x) / getWidth())), 1) * mTrack.duration);
    }

    public void showNewComment(Comment c) {
        if (c.xPos == -1){
            if (getWidth() == 0 || mDuration <= 0) return;
            c.calculateXPos(getWidth(), mDuration);
        }
        if (mCurrentCommentPanel != null && mCurrentShowingComment != null) closeComment(false);
        mCurrentShowingComment = mLastAutoComment = c;
        showCurrentComment(false);
        mHandler.postDelayed(mAutoCloseComment, CLOSE_COMMENT_DELAY);
    }

    public void setPlayerTrackView(PlayerTrackView playerTrackView) {
        mPlayerTrackView = playerTrackView;
    }

    public void onDataConnected() {
        if (mWaveformState == WaveformController.WaveformState.ERROR) {
            updateTrack(mTrack, mQueuePosition, mOnScreen);
        }
    }

    public enum WaveformState {
        OK, LOADING, ERROR
    }

    private static final class TouchHandler extends Handler {
        private WeakReference<WaveformController> mRef;

        private TouchHandler(WaveformController controller) {
            this.mRef = new WeakReference<WaveformController>(controller);
        }

        @Override
        public void handleMessage(Message msg) {
            final WaveformController controller = mRef.get();
            if (controller == null) {
                return;
            }

            final float seekPercent = controller.mSeekPercent;
            final ScPlayer player = controller.mPlayer;
            final PlayerTouchBar touchBar = controller.mPlayerTouchBar;

            switch (msg.what) {
                case UI_UPDATE_SEEK:
                    long seekTime = player.setSeekMarker(controller.mQueuePosition, seekPercent);
                    if (seekTime == -1){
                        // the seek did not work, abort
                        controller.mode = TOUCH_MODE_NONE;
                    } else {
                        touchBar.setSeekPosition((int) (seekPercent * controller.getWidth()), touchBar.getHeight(), false);
                        controller.mCurrentTimeDisplay.setCurrentTime(seekTime, false);
                    }

                    controller.mWaveformHolder.invalidate();
                    break;

                case UI_SEND_SEEK:
                    if (player.isSeekable()){
                        player.sendSeek(seekPercent);
                    }
                    touchBar.clearSeek();
                    break;

                case UI_CLEAR_SEEK:
                    long progress = controller.lastTrackTime + System.currentTimeMillis() - controller.lastProgressTimestamp;
                    controller.setProgressInternal(progress);
                    touchBar.clearSeek();
                    break;

                case UI_UPDATE_COMMENT_POSITION:
                    controller.mCurrentTimeDisplay.setByPercent(seekPercent, true);
                    touchBar.setSeekPosition((int) (seekPercent * controller.getWidth()), touchBar.getHeight(), true);
                    break;

                case UI_ADD_COMMENT:
                    player.addNewComment(controller.mAddComment);
                    controller.mPlayerTrackView.setCommentMode(false);
                    break;

                case UI_UPDATE_COMMENT:
                    if (controller.mShowComment) {
                        controller.showCurrentComment(true);
                    } else {
                        controller.closeComment(false);
                    }
                    break;
            }
        }
    }
}
