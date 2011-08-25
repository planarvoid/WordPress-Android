package com.soundcloud.android.view;

import static com.soundcloud.android.utils.CloudUtils.mkdirs;

import android.view.animation.*;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.InputObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class WaveformController extends RelativeLayout implements OnTouchListener {
    private static final String TAG = "WaveformController";

    private static final long CLOSE_COMMENT_DELAY = 5000;

    protected PlayerAvatarBar mPlayerAvatarBar;

    private ImageView mOverlay;
    private ProgressBar mProgressBar;
    protected WaveformHolder mWaveformHolder;
    private RelativeLayout mWaveformFrame;
    private PlayerTouchBar mPlayerTouchBar;
    protected WaveformCommentLines mCommentLines;
    private PlayerTime mCurrentTimeDisplay;
    protected ImageButton mToggleComments;

    protected ScPlayer mPlayer;
    protected Track mPlayingTrack;
    protected boolean mShowingComments, mSuspendTimeDisplay;
    protected List<Comment> mCurrentComments;
    protected List<Comment> mCurrentTopComments;
    protected Comment mCurrentShowingComment;
    public ImageLoader.BindResult waveformResult;

    protected CommentDisplay mCommentDisplay;
    private CommentPanel mCommentPanel;

    protected Comment mAddComment;
    private Comment mLastAutoComment;

    private ArrayBlockingQueue<InputObject> mInputObjectPool;
    private TouchThread mTouchThread;

    private int mWaveformErrorCount, mDuration;

    private float mSeekPercent;

    private SharedPreferences mPreferences;
    protected final Handler mHandler = new Handler();

    private static final int MAX_WAVEFORM_RETRIES = 2;
    private static final int INPUT_QUEUE_SIZE = 20;

    private static final int UI_UPDATE_SEEK = 1;
    private static final int UI_SEND_SEEK = 2;
    private static final int UI_UPDATE_COMMENT_POSITION = 3;
    private static final int UI_ADD_COMMENT = 4;
    private static final int UI_TOGGLE_COMMENTS = 5;

    static final int TOUCH_MODE_NONE = 0;
    static final int TOUCH_MODE_SEEK_DRAG = 1;
    static final int TOUCH_MODE_COMMENT_DRAG = 2;
    static final int TOUCH_MODE_AVATAR_DRAG = 3;
    int mode = TOUCH_MODE_NONE;
    private long mCurrentTime;


    public WaveformController(Context context, AttributeSet attrs) {
        super(context, attrs);

        setWillNotDraw(false);


        mPlayer = (ScPlayer) context;

        mPreferences = PreferenceManager.getDefaultSharedPreferences(mPlayer);

        mShowingComments = mPreferences.getBoolean("playerShowingCOmments", true);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.wave_form_controller, this);

        mInputObjectPool = new ArrayBlockingQueue<InputObject>(INPUT_QUEUE_SIZE);
        for (int i = 0; i < INPUT_QUEUE_SIZE; i++) {
            mInputObjectPool.add(new InputObject(mInputObjectPool));
        }

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

        mCommentLines = new WaveformCommentLines(mPlayer, null);
        mCommentLines.setVisibility(mShowingComments ? View.INVISIBLE : View.GONE);
        mWaveformHolder.addView(mCommentLines);

        mOverlay.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);
        mCurrentTimeDisplay.setVisibility(View.INVISIBLE);

        LightingColorFilter lcf = new LightingColorFilter(1, mPlayer.getResources().getColor(
                R.color.playerControlBackground));
        mOverlay.setBackgroundColor(Color.TRANSPARENT);
        mOverlay.setColorFilter(lcf);
        mOverlay.setScaleType(ScaleType.FIT_XY);

        File dirFile = new File(CloudUtils.getCacheDirPath(mPlayer) + "/waves/");

        setStaticTransformationsEnabled(true);
        mkdirs(dirFile);

        mCommentPanel = (CommentPanel) findViewById(R.id.comment_panel);
        if (mCommentPanel != null) {
            mCommentDisplay = mCommentPanel;
            mCommentPanel.setControllers(mPlayer, this);
            mCommentPanel.setVisibility(View.GONE);
        } else {
            mPlayerTouchBar.setLandscape(true);
        }
    }

    private void createCommentPanel(){

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mCurrentTimeDisplay.setWaveHeight(mWaveformHolder.getHeight());
        if (mCurrentComments != null && mDuration > 0){
            for (Comment c : mCurrentComments){
                c.calculateXPos(getWidth(), mDuration);
            }
        }
    }

    public void onStop() {
        if (mPlayerAvatarBar != null) mPlayerAvatarBar.onStop(); //stops avatar loading
    }

    public void showConnectingLayout() {
        mWaveformHolder.showConnectingLayout(true);
        invalidate();
    }

    public void hideConnectingLayout() {
        mWaveformHolder.hideConnectingLayout();
        invalidate();
    }

    public void setCommentMode(boolean commenting) {
        if (commenting) {
            mSuspendTimeDisplay = true;
            mode = TOUCH_MODE_COMMENT_DRAG;
            mPlayerTouchBar.setSeekPosition((int) ((((float) mCurrentTime) / mDuration) * getWidth()), mPlayerTouchBar.getHeight());
            mCurrentTimeDisplay.setByPercent((((float) mCurrentTime) / mDuration), true);
        } else {
            mSuspendTimeDisplay = false;
            mode = TOUCH_MODE_NONE;
            mPlayerTouchBar.clearSeek();
            setCurrentTime(mCurrentTime);
        }
    }

    public void setProgress(long pos) {
        if (mDuration == 0)
            return;

        mProgressBar.setProgress((int) (1000 * pos / mDuration));

        if (mode == TOUCH_MODE_NONE && mCurrentTopComments != null) {
            Comment last = lastCommentBeforeTimestamp(pos);
            if (last != null) {
                if (mLastAutoComment != last && pos - last.timestamp < 2000) {
                    mLastAutoComment = last;
                    if (mCurrentShowingComment == null && mPlayer.waveformVisible()) {
                        mCurrentShowingComment = last;
                        showCurrentComment(true);
                        mHandler.postDelayed(mAutoCloseComment, CLOSE_COMMENT_DELAY);
                    }
                }
            }
        }
    }

    public void setSecondaryProgress(int percent) {
        mProgressBar.setSecondaryProgress(percent);
    }

    public void setCurrentTime(long time) {
        mCurrentTime = time;
        if (mode != TOUCH_MODE_SEEK_DRAG && !mSuspendTimeDisplay) {
            mCurrentTimeDisplay.setCurrentTime(time, false);
        }
    }

    public void updateTrack(Track track) {
        if (mPlayingTrack != null &&
                mPlayingTrack.id == track.id
                && waveformResult != BindResult.ERROR) {
            return;
        }

        if (mPlayingTrack != track) {
            ImageLoader.get(mPlayer).unbind(mOverlay);
        }

        mPlayingTrack = track;
        mDuration = mPlayingTrack != null ? mPlayingTrack.duration : 0;
        mCurrentTimeDisplay.setDuration(mDuration);

        if (TextUtils.isEmpty(track.waveform_url)) {
            waveformResult = BindResult.ERROR;
            mOverlay.setImageDrawable(mPlayer.getResources().getDrawable(R.drawable.player_wave_bg));
            showWaveform();
            return;
        }

        if (waveformResult == BindResult.ERROR) {
            // clear loader errors so we can try to reload
            ImageLoader.get(mPlayer).clearErrors();
        } else {
            mWaveformErrorCount = 0;
        }
        waveformResult = ImageLoader.get(mPlayer).bind(mOverlay, track.waveform_url,
                new ImageLoader.ImageViewCallback() {
                    @Override
                    public void onImageError(ImageView view, String url, Throwable error) {
                        waveformResult = BindResult.ERROR;
                        onWaveformError();
                    }

                    @Override
                    public void onImageLoaded(ImageView view, String url) {
                        showWaveform();
                    }
                });


        switch (waveformResult) {
            case OK:
                showWaveform();
                break;
            case LOADING:
            case ERROR:
                mOverlay.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
                mCurrentTimeDisplay.setVisibility(View.INVISIBLE);
                break;
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        try {
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

    protected void showCurrentComment() {
        showCurrentComment(false);
    }

    protected void showCurrentComment(boolean waitForInteraction) {
        if (mCommentPanel == null) return;

        if (mCurrentShowingComment != null) {
            mPlayerAvatarBar.setCurrentComment(mCurrentShowingComment);
            mCommentLines.setCurrentComment(mCurrentShowingComment);

            mCommentPanel.showComment(mCurrentShowingComment);
            mCommentPanel.interacted = !waitForInteraction;
            mCommentPanel.setVisibility(View.VISIBLE);

            AnimationSet set = new AnimationSet(true);

            Animation animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration(500);
            set.addAnimation(animation);

            animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f);
            animation.setDuration(500);
            set.addAnimation(animation);

            mCommentPanel.startAnimation(set);
        }
    }

    public void closeComment() {
        mCurrentShowingComment = null;
        if (mPlayerAvatarBar != null) mPlayerAvatarBar.setCurrentComment(null);
        if (mCommentLines != null) mCommentLines.setCurrentComment(null);
        if (mCommentPanel != null) mCommentPanel.setVisibility(View.GONE);
    }

    protected void autoCloseComment() {
        if (mCommentDisplay != null && mCurrentShowingComment != null)
            if (mCurrentShowingComment == mCommentDisplay.getComment() && !mCommentDisplay.interacted) {
                closeComment();
            }
    }

    protected void cancelAutoCloseComment() {
        mHandler.removeCallbacks(mAutoCloseComment);
    }

    final Runnable mAutoCloseComment = new Runnable() {
        public void run() {
            autoCloseComment();
        }
    };

    public void nextCommentInThread() {
        if (mCurrentShowingComment.nextComment != null) {
            mCurrentShowingComment = mCurrentShowingComment.nextComment;
            showCurrentComment();
        }
    }

    public void clearTrack() {
        cancelAutoCloseComment();
        closeComment();

        if (mPlayerAvatarBar != null) {
            if (mShowingComments) mPlayerAvatarBar.setVisibility(View.INVISIBLE);
            mPlayerAvatarBar.clearTrackData();
        }
        if (mCommentLines != null) {
            if (mShowingComments) mCommentLines.setVisibility(View.INVISIBLE);
            mCommentLines.clearTrackData();
        }

        mCurrentComments = null;
        mCurrentTopComments = null;
        mode = TOUCH_MODE_NONE;
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
            updateTrack(mPlayingTrack);
        } else {
            mOverlay.setImageDrawable(mPlayer.getResources()
                    .getDrawable(R.drawable.player_wave_bg));
            showWaveform();
        }
    }


    private void showWaveform() {
        mPlayer.onWaveformLoaded();
        if (mOverlay.getVisibility() == View.INVISIBLE) {
            AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
            aa.setDuration(500);

            mOverlay.startAnimation(aa);
            mOverlay.setVisibility(View.VISIBLE);

            mProgressBar.startAnimation(aa);
            mProgressBar.setVisibility(View.VISIBLE);

            mCurrentTimeDisplay.startAnimation(aa);
            mCurrentTimeDisplay.setVisibility(View.VISIBLE);
        }
    }


    private Comment lastCommentBeforeTimestamp(long timestamp) {
        for (Comment comment : mCurrentTopComments)
            if (comment.timestamp < timestamp)
                return comment;

        return null;
    }


    public void setComments(List<Comment> comments, boolean animateIn) {
        mCurrentComments = comments;

        if (!mShowingComments || mCurrentComments == null)
            return;

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
            } else if (mCurrentComments.get(i).xPos == -1) {
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


    protected void toggleComments() {
        mShowingComments = !mShowingComments;
        mPreferences.edit().putBoolean("playerShowingCOmments", mShowingComments).commit();
        if (mShowingComments) {
            mCommentLines.setVisibility(View.INVISIBLE);
            setComments(mCurrentComments, true);
        } else {
            mCommentLines.setVisibility(View.GONE);
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
        } else if (input.view == mPlayerTouchBar) {
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
                mSeekPercent = ((float) input.x) / mWaveformHolder.getWidth();
                queueUnique(UI_UPDATE_COMMENT_POSITION);
                break;
            case TOUCH_MODE_SEEK_DRAG:
                if (mPlayer != null && mPlayer.isSeekable()) {
                    mLastAutoComment = null; //reset auto comment in case they seek backward
                    mSeekPercent = ((float) input.x) / mWaveformHolder.getWidth();
                    queueUnique(UI_UPDATE_SEEK);
                }
                break;
        }
    }

    protected void processUpInput(InputObject input) {
        switch (mode) {
            case TOUCH_MODE_COMMENT_DRAG:
                if (Math.abs(mPlayerTouchBar.getTop() - input.y) < 200) {
                    mAddComment = CloudUtils.buildComment(mPlayer, mPlayer.getCurrentUserId(),
                            mPlayingTrack.id, stampFromPosition(input.x), "", 0);
                    queueUnique(UI_ADD_COMMENT);
                }
                break;
            case TOUCH_MODE_SEEK_DRAG:
                queueUnique(UI_SEND_SEEK);
                break;
        }
        mode = TOUCH_MODE_NONE;
    }

    protected void queueUnique(int what) {
        if (!mTouchHandler.hasMessages(what)) mTouchHandler.sendEmptyMessage(what);
    }

    Handler mTouchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_UPDATE_SEEK:
                    setProgress(mPlayer.setSeekMarker(mSeekPercent));
                    mCurrentTimeDisplay.setByPercent(mSeekPercent, false);
                    mWaveformHolder.invalidate();
                    break;

                case UI_SEND_SEEK:
                    if (mPlayer != null)
                        mPlayer.sendSeek(mSeekPercent);
                    mCurrentTimeDisplay.setByPercent(mSeekPercent, false);
                    mPlayerTouchBar.clearSeek();
                    break;

                case UI_UPDATE_COMMENT_POSITION:
                    mCurrentTimeDisplay.setByPercent(mSeekPercent, true);
                    mPlayerTouchBar.setSeekPosition((int) (mSeekPercent * getWidth()), mPlayerTouchBar.getHeight());
                    break;

                case UI_ADD_COMMENT:
                    mPlayer.addNewComment(mAddComment, mPlayer.addCommentListener);
                    mPlayer.toggleCommentMode();
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
    }

    protected long stampFromPosition(int x) {
        return (long) (Math.min(Math.max(.001, (((float) x) / getWidth())), 1) * mPlayingTrack.duration);
    }

    public void showComment(Comment c) {
        if (mCommentDisplay != null && mCurrentShowingComment != null) closeComment();
        mCurrentShowingComment = c;
        showCurrentComment(true);
        mHandler.postDelayed(mAutoCloseComment, CLOSE_COMMENT_DELAY);
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