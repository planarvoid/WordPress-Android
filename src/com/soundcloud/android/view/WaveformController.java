package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.InputObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class WaveformController extends RelativeLayout implements OnTouchListener {
    private static final String TAG = "WaveformController";

    protected static final long CLOSE_COMMENT_DELAY = 5000;

    protected PlayerAvatarBar mPlayerAvatarBar;

    private ImageView mOverlay;
    protected ProgressBar mProgressBar;
    protected WaveformHolder mWaveformHolder;
    protected RelativeLayout mWaveformFrame;
    private PlayerTouchBar mPlayerTouchBar;
    protected WaveformCommentLines mCommentLines;
    protected PlayerTime mCurrentTimeDisplay;

    protected ScPlayer mPlayer;
    protected Track mPlayingTrack;
    protected boolean mSuspendTimeDisplay, mOnScreen;
    protected List<Comment> mCurrentComments;
    protected List<Comment> mCurrentTopComments;
    protected Comment mCurrentShowingComment;
    public ImageLoader.BindResult waveformResult;

    protected CommentPanel mCurrentCommentPanel;

    protected Comment mAddComment;
    protected Comment mLastAutoComment;

    private ArrayBlockingQueue<InputObject> mInputObjectPool;
    private TouchThread mTouchThread;

    private int mWaveformErrorCount, mDuration;

    private float mSeekPercent;

    protected final Handler mHandler = new Handler();

    private static final int MAX_WAVEFORM_RETRIES = 2;
    private static final int INPUT_QUEUE_SIZE = 20;

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

    int mode = TOUCH_MODE_NONE;

    private PlayerTrackView mPlayerTrackView;
    protected boolean mShowComment;
    private static final long MIN_COMMENT_DISPLAY_TIME = 2000;

    // only allow smooth progress updates on 9 or greater because they have buffering events for proper displaying
    public static final int MINIMUM_SMOOTH_PROGRESS_SDK = 9;
    private static final long MINIMUM_PROGRESS_PERIOD = 40;
    private boolean mShowingSmoothProgress;
    private boolean mIsBuffering, mWaitingForSeekComplete;
    private int mTouchSlop;


    public WaveformController(Context context, AttributeSet attrs) {
        super(context, attrs);

        setWillNotDraw(false);

        mPlayer = (ScPlayer) context;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.wave_form_controller, this);

        mInputObjectPool = new ArrayBlockingQueue<InputObject>(INPUT_QUEUE_SIZE);
        for (int i = 0; i < INPUT_QUEUE_SIZE; i++) {
            mInputObjectPool.add(new InputObject(mInputObjectPool));
        }

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();

        mTouchThread = new TouchThread();
        mTouchThread.start();

        mWaveformFrame = (RelativeLayout) findViewById(R.id.waveform_frame);
        mWaveformHolder = (WaveformHolder) findViewById(R.id.waveform_holder);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mCurrentTimeDisplay = (PlayerTime) findViewById(R.id.currenttime);
        mOverlay = (ImageView) findViewById(R.id.progress_overlay);

        mPlayerTouchBar = (PlayerTouchBar) findViewById(R.id.track_touch_bar);
        mPlayerTouchBar.setOnTouchListener(this);

        mPlayerAvatarBar = (PlayerAvatarBar) findViewById(R.id.player_avatar_bar);
        mPlayerAvatarBar.setOnTouchListener(this);
        mPlayerAvatarBar.setIsLandscape(isLandscape());

        mCommentLines = new WaveformCommentLines(mPlayer, null);
        mWaveformHolder.addView(mCommentLines);

        mOverlay.setVisibility(View.INVISIBLE);
        mCurrentTimeDisplay.setVisibility(View.INVISIBLE);

        LightingColorFilter lcf = new LightingColorFilter(1, mPlayer.getResources().getColor(
                R.color.playerControlBackground));
        mOverlay.setBackgroundColor(Color.TRANSPARENT);
        mOverlay.setColorFilter(lcf);
        mOverlay.setScaleType(ScaleType.FIT_XY);
        setStaticTransformationsEnabled(true);
        mPlayerTouchBar.setLandscape(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

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

    }

    public void startSmoothProgress(){
        mShowingSmoothProgress = true;
        mHandler.postDelayed(mSmoothProgress, 0);
    }

    public void stopSmoothProgress(){
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
            if (mPlayingTrack != null) {
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
            //mProgressBar.setVisibility(View.INVISIBLE);
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

    @SuppressLint("NewApi")
    public void resetCommentDisplay(){
        if (mCurrentCommentPanel != null) {
            if (mCurrentCommentPanel.getAnimation() != null){
                if (Build.VERSION.SDK_INT > 7) mCurrentCommentPanel.getAnimation().cancel();
                mCurrentCommentPanel.clearAnimation();
            }
            if (mCurrentCommentPanel.getParent() == mWaveformFrame){
                mWaveformFrame.removeView(mCurrentCommentPanel);
            }
            mCurrentCommentPanel = null;
        }
    }

    public void onBufferingStart() {
        mIsBuffering = true;
        showWaiting();
    }

    public void onBufferingStop(){
        mIsBuffering = false;
        if (waveformResult != BindResult.LOADING && !mWaitingForSeekComplete){
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
        if (waveformResult != BindResult.LOADING){
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
        if (mDuration == 0)
            return;

       mProgressBar.setProgress((int) (pos * 1000 / mDuration));
        if (mode != TOUCH_MODE_SEEK_DRAG) {
            setCurrentTime(pos);
        }

        if (mode == TOUCH_MODE_NONE && mCurrentTopComments != null) {
            final Comment last = lastCommentBeforeTimestamp(pos);
            if (last != null) {
                if (mLastAutoComment != last && pos - last.timestamp < 2000) {
                    if (mPlayerTrackView != null && mPlayerTrackView.waveformVisible() && (mCurrentShowingComment == null || (mCurrentShowingComment == mLastAutoComment &&
                            (last.timestamp - mLastAutoComment.timestamp > MIN_COMMENT_DISPLAY_TIME)))) {
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
            if (getWidth() == 0){
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

    public boolean showingSmoothProgress(){
        return  mShowingSmoothProgress;
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

    public void updateTrack(Track track, boolean postAtFront) {
        if (track == null || (mPlayingTrack != null &&
                mPlayingTrack.id == track.id
                && waveformResult != BindResult.ERROR)) {
            return;
        }

        final boolean changed = mPlayingTrack != track;
        mPlayingTrack = track;
        mDuration = mPlayingTrack.duration;
        mCurrentTimeDisplay.setDuration(mDuration);

        if (changed) {
            stopSmoothProgress();
            determineProgressInterval();
            ImageLoader.get(mPlayer).unbind(mOverlay);
            // TODO best place to do this?
            if (mPlayer.isConnected()) ImageLoader.get(mPlayer).clearErrors();
        }

        if (TextUtils.isEmpty(track.waveform_url)){
            waveformResult = BindResult.ERROR;
            mOverlay.setImageDrawable(mPlayer.getResources().getDrawable(R.drawable.player_wave_bg));
            showWaveform(false);
            return;
        }

        if (waveformResult == BindResult.ERROR) {
            // clear loader errors so we can try to reload
            ImageLoader.get(mPlayer).clearErrors();
        } else {
            mWaveformErrorCount = 0;
        }

        waveformResult = ImageLoader.get(mPlayer).bind(mOverlay, track.waveform_url,
                new ImageLoader.Callback() {
                    @Override
                    public void onImageError(ImageView view, String url, Throwable error) {
                        waveformResult = BindResult.ERROR;
                        onWaveformError();
                    }

                    @Override
                    public void onImageLoaded(ImageView view, String url) {
                        waveformResult = BindResult.OK;
                        showWaveform(mOnScreen);
                    }
                },new ImageLoader.Options(true, postAtFront));


        switch (waveformResult) {
            case OK:
                showWaveform(false);
                break;
            case LOADING:
                showWaiting();
                mOverlay.setVisibility(View.INVISIBLE);
                //mProgressBar.setVisibility(View.INVISIBLE);
                mCurrentTimeDisplay.setVisibility(View.INVISIBLE);
                break;
            case ERROR:
                showWaiting();
                mOverlay.setVisibility(View.INVISIBLE);
                //mProgressBar.setVisibility(View.INVISIBLE);
                mCurrentTimeDisplay.setVisibility(View.INVISIBLE);
                onWaveformError();
                break;
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        try {
            // Fix scrolling inside workspace view
            if ((event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) && getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            // history first
            int hist = event.getHistorySize();
            if (hist > 0) {
                // add from oldest to newest
                for (int i = 0; i < hist; i++) {
                    InputObject input = mInputObjectPool.take();
                    input.useEventHistory(v, event, i);
                    mTouchThread.feedInput(input);
                }
            }
            // current last
            InputObject input = mInputObjectPool.take();
            input.useEvent(v, event);
            mTouchThread.feedInput(input);
        } catch (InterruptedException ignored) {
        }
        return true; // indicate event was handled
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

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        boolean ret = super.getChildStaticTransformation(child, t);
        if (child == mWaveformFrame) {
            t.setAlpha((float) 0.95);
            return true;
        }
        return ret;
    }


    private void onWaveformError() {
        mWaveformErrorCount++;
        if (mWaveformErrorCount < MAX_WAVEFORM_RETRIES) {
            updateTrack(mPlayingTrack, mOnScreen);
        } else {
            mOverlay.setImageDrawable(mPlayer.getResources()
                    .getDrawable(R.drawable.player_wave_bg));
            showWaveform(mOnScreen);
        }
    }


    private void showWaveform(boolean animate) {
        mPlayer.onWaveformLoaded();
        if (!mIsBuffering) hideWaiting();

        if (mOverlay.getVisibility() == View.INVISIBLE) {
            mOverlay.setVisibility(View.VISIBLE);
            //mProgressBar.setVisibility(View.VISIBLE);
            mCurrentTimeDisplay.setVisibility(View.VISIBLE);

            if (animate){
                AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
                aa.setDuration(500);
                mOverlay.startAnimation(aa);
                //mProgressBar.startAnimation(aa);
                mCurrentTimeDisplay.startAnimation(aa);
            }
        }
    }


    private Comment lastCommentBeforeTimestamp(long timestamp) {
        for (Comment comment : mCurrentTopComments)
            if (comment.timestamp < timestamp)
                return comment;

        return null;
    }

    protected Comment nextCommentAfterTimestamp(long timestamp) {
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
        mCurrentTopComments = new ArrayList<Comment>();

        Collections.sort(comments, Comment.CompareTimestamp.INSTANCE);

        for (int i = 0; i < mCurrentComments.size(); i++) {
            if (mCurrentComments.get(i).timestamp > 0 && (i == mCurrentComments.size() - 1 || mCurrentComments.get(i).timestamp != mCurrentComments.get(i + 1).timestamp)) {
                mCurrentComments.get(i).topLevelComment = true;
                mCurrentTopComments.add(mCurrentComments.get(i));
            } else if (mCurrentComments.get(i).timestamp > 0)
                mCurrentComments.get(i + 1).nextComment = mCurrentComments.get(i);

            if (getWidth() == 0 && mDuration <= 0) {
                mCurrentComments.get(i).xPos = -1;
            } else if (mCurrentComments.get(i).xPos == -1 && mDuration > 0) {
                mCurrentComments.get(i).calculateXPos(getWidth(), mDuration);
            }
        }

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


    private void processInputObject(InputObject input) {
        switch (input.action) {
            case InputObject.ACTION_TOUCH_DOWN:
                processDownInput(input);
                break;

            case InputObject.ACTION_TOUCH_MOVE:
                processMoveInput(input);
                break;
            case InputObject.ACTION_TOUCH_UP:
                processUpInput(input);
                break;
        }
    }

    protected void processDownInput(InputObject input) {
        if (mode == TOUCH_MODE_COMMENT_DRAG) {
            mSeekPercent = ((float) input.x) / mWaveformHolder.getWidth();
            queueUnique(UI_UPDATE_COMMENT_POSITION);
        } else if (input.view == mPlayerTouchBar && mPlayer.isSeekable()) {
            mode = TOUCH_MODE_SEEK_DRAG;
            if (mPlayer != null && mPlayer.isSeekable()) {
                mLastAutoComment = null; //reset auto comment in case they seek backward
                mSeekPercent = ((float) input.x) / mWaveformHolder.getWidth();
                queueUnique(UI_UPDATE_SEEK);
            }
        }
    }

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

    protected void processUpInput(InputObject input) {
        switch (mode) {
            case TOUCH_MODE_COMMENT_DRAG:
                if (isOnTouchBar(input.y)) {
                    mAddComment = Comment.build(
                            mPlayingTrack,
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

    private boolean isOnTouchBar(int y){
        return (y > mPlayerTouchBar.getTop() - mTouchSlop && y < mPlayerTouchBar.getBottom() + mTouchSlop);
    }

    protected void queueUnique(int what) {
        if (!mTouchHandler.hasMessages(what)) mTouchHandler.sendEmptyMessage(what);
    }

    Handler mTouchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_UPDATE_SEEK:
                    long seekTime = mPlayer.setSeekMarker(mSeekPercent);
                    mPlayerTouchBar.setSeekPosition((int) (mSeekPercent * getWidth()), mPlayerTouchBar.getHeight(), false);
                    //setProgressInternal(seekTime);
                    mCurrentTimeDisplay.setCurrentTime(seekTime,false);
                    mWaveformHolder.invalidate();
                    break;

                case UI_SEND_SEEK:
                    if (mPlayer != null && mPlayer.isSeekable()){
                        mPlayer.sendSeek(mSeekPercent);
                    }
                    mPlayerTouchBar.clearSeek();
                    break;

                case UI_CLEAR_SEEK:
                    setProgressInternal(lastTrackTime + System.currentTimeMillis() - lastProgressTimestamp);
                    mPlayerTouchBar.clearSeek();
                    break;

                case UI_UPDATE_COMMENT_POSITION:
                    mCurrentTimeDisplay.setByPercent(mSeekPercent, true);
                    mPlayerTouchBar.setSeekPosition((int) (mSeekPercent * getWidth()), mPlayerTouchBar.getHeight(), true);
                    break;

                case UI_ADD_COMMENT:
                    mPlayer.addNewComment(mAddComment);
                    mPlayerTrackView.toggleCommentMode();
                    break;

                case UI_UPDATE_COMMENT:
                    if (mShowComment)
                        showCurrentComment(true);
                    else
                        closeComment(false);
                    break;
            }
        }
    };

    public void onDestroy() {
        if (mCurrentComments != null) {
            for (Comment c : mCurrentComments) {
                c.xPos = -1;
            }
        }

        if (mTouchThread != null) {
            mTouchThread.stopped = true;
            mTouchThread.interrupt();
        }

        mPlayerAvatarBar.clearTrackData();
        mCommentLines.clearTrackData();
    }

    protected long stampFromPosition(int x) {
        return (long) (Math.min(Math.max(.001, (((float) x) / getWidth())), 1) * mPlayingTrack.duration);
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

    private class TouchThread extends Thread {
        private ArrayBlockingQueue<InputObject> inputQueue = new ArrayBlockingQueue<InputObject>(INPUT_QUEUE_SIZE);
        private boolean stopped = false;

        public synchronized void feedInput(InputObject input) {
            try {
                inputQueue.put(input);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }

        @Override
        public void run() {
            while (!stopped) {
                InputObject input = null;
                try {
                    input = inputQueue.take();
                    if (input.eventType == InputObject.EVENT_TYPE_TOUCH) {
                        processInputObject(input);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    if (input != null) input.returnToPool();
                }
            }
        }
    }
}
