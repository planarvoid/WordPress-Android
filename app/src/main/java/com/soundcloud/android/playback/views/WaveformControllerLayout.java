package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.InputObject;
import com.soundcloud.android.view.TouchLayout;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.android.waveform.WaveformResult;
import org.jetbrains.annotations.Nullable;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
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

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WaveformControllerLayout extends TouchLayout implements CommentPanelLayout.CommentPanelListener {
    private static final String TAG = WaveformControllerLayout.class.getSimpleName();

    protected static final long CLOSE_COMMENT_DELAY = 5000;
    private static final int OVERLAY_BG_COLOR = Color.WHITE;

    protected @Nullable
    PlayerAvatarBarView playerAvatarBar;

    private View overlay;
    protected ProgressBar progressBar;
    protected WaveformHolderLayout waveformHolder;
    protected RelativeLayout waveformFrame;
    private PlayerTouchBarView playerTouchBar;
    protected @Nullable
    WaveformCommentLinesView commentLines;
    protected PlayerTimeView currentTimeDisplay;

    protected @Nullable Track track;
    protected int queuePosition;

    protected boolean suspendTimeDisplay, onScreen;
    protected @Nullable List<Comment> currentComments;
    protected List<Comment> currentTopComments;
    protected @Nullable Comment currentShowingComment;

    private WaveformState waveformState;

    protected @Nullable
    CommentPanelLayout currentCommentPanel;

    protected Comment lastAutoComment;

    private int waveformErrorCount, duration;
    private float seekPercent;

    protected final Handler handler = new Handler();
    private Handler touchHandler = new TouchHandler(this);

    private static final int MAX_WAVEFORM_RETRIES = 2;

    private static final int UI_UPDATE_SEEK = 1;
    private static final int UI_SEND_SEEK   = 2;
    private static final int UI_UPDATE_COMMENT_POSITION = 3;
    protected static final int UI_UPDATE_COMMENT = 5;
    protected static final int UI_CLEAR_SEEK = 6;
    // used by landscape
    protected static final int UI_SHOW_CURRENT_COMMENT = 7;

    static final int TOUCH_MODE_NONE = 0;
    static final int TOUCH_MODE_SEEK_DRAG = 1;
    static final int TOUCH_MODE_COMMENT_DRAG = 2;
    static final int TOUCH_MODE_AVATAR_DRAG = 3;
    static final int TOUCH_MODE_SEEK_CLEAR_DRAG = 4;

    private boolean suppressComments;

    protected int mode = TOUCH_MODE_NONE;

    protected boolean showComment;
    private static final long MIN_COMMENT_DISPLAY_TIME = 2000;

    private boolean isBuffering;
    private int touchSlop;
    private int waveformColor;

    private WaveformListener listener;

    @Inject WaveformOperations waveformOperations;
    @Inject ImageOperations imageOperations;


    public interface WaveformListener {
        long sendSeek(float seekPosition);
        long setSeekMarker(int queuePosition, float seekPosition);
    }


    public WaveformControllerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        SoundCloudApplication.getObjectGraph().inject(this);

        setWillNotDraw(false);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.wave_form_controller, this);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        touchSlop = configuration.getScaledTouchSlop();

        waveformFrame = (RelativeLayout) findViewById(R.id.waveform_frame);
        waveformHolder = (WaveformHolderLayout) findViewById(R.id.waveform_holder);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        currentTimeDisplay = (PlayerTimeView) findViewById(R.id.currenttime);

        waveformColor = context.getResources().getColor(R.color.player_control_background);
        overlay = findViewById(R.id.progress_overlay);
        overlay.setBackgroundColor(OVERLAY_BG_COLOR);

        playerTouchBar = (PlayerTouchBarView) findViewById(R.id.track_touch_bar);
        playerAvatarBar = (PlayerAvatarBarView) findViewById(R.id.player_avatar_bar);
        playerAvatarBar.setIsLandscape(isLandscape());

        commentLines = new WaveformCommentLinesView(context, null);
        waveformHolder.addView(commentLines);

        currentTimeDisplay.setVisibility(View.INVISIBLE);
        playerTouchBar.setLandscape(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        // override default touch layout functionality, these views are all we want to respond
        playerTouchBar.setOnTouchListener(this);
        playerAvatarBar.setOnTouchListener(this);
        setOnTouchListener(null);
    }

    public void setOnScreen(boolean onScreen){
        this.onScreen = onScreen;
    }

    public void setListener(WaveformListener listener){
        this.listener = listener;
    }

    @Override
    public void onNextCommentInThread() {
        nextCommentInThread();
    }

    @Override
    public void onCloseComment() {
        closeComment(true);
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
            handler.postDelayed(this, mProgressPeriod);
        }
    };

    public void setPlaybackStatus(boolean isPlaying, long pos){
        setProgress(pos);
        if (!isPlaying) {
            setBufferingState(false);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && getWidth()  > 0) {
            if (currentComments != null && duration > 0) {
                for (Comment c : currentComments) {
                    c.calculateXPos(getWidth(), duration);
                }
            }
        }
    }

    public void reset(boolean hide){
        isBuffering = false;
        setProgressInternal(0);
        setSecondaryProgress(0);
        onStop(false);

        if (hide){
            showWaiting();
            overlay.setVisibility(View.INVISIBLE);
            currentTimeDisplay.setVisibility(View.INVISIBLE);
        }
    }

     public void onStop(boolean killLoading) {
        cancelAutoCloseComment();

         // comment states
        if (playerAvatarBar != null) playerAvatarBar.setCurrentComment(null);
        if (commentLines != null) commentLines.setCurrentComment(null);
        lastAutoComment = null;
        currentShowingComment = null;
        resetCommentDisplay();

         //only performed on activity stop
         if (playerAvatarBar != null && killLoading) playerAvatarBar.stopAvatarLoading();
    }

    public void resetCommentDisplay(){
        if (currentCommentPanel != null) {
            if (currentCommentPanel.getAnimation() != null){
                currentCommentPanel.getAnimation().cancel();
                currentCommentPanel.clearAnimation();
            }
            if (currentCommentPanel.getParent() == waveformFrame){
                waveformFrame.removeView(currentCommentPanel);
            }
            currentCommentPanel = null;
        }
    }

    public void setSuppressComments(boolean suppressComments){
        this.suppressComments = suppressComments;
        closeComment(false);
    }

    public void setBufferingState(boolean isBuffering) {
        this.isBuffering = isBuffering;
        if (this.isBuffering){
            showWaiting();
        } else if (waveformState != WaveformState.LOADING){
            hideWaiting();
        }
    }

    public void onSeek(long seekTime){
        setProgressInternal(seekTime);
    }

    private void showWaiting() {
        waveformHolder.showWaitingLayout(true);
        invalidate();
    }

    private void hideWaiting() {
        waveformHolder.hideWaitingLayout();
        invalidate();

    }

    public void setCommentMode(boolean commenting) {
        currentTimeDisplay.setCommenting(commenting);
        if (commenting) {
            if (currentShowingComment != null && !isLandscape()) {
                closeComment(false);
            }
            suspendTimeDisplay = true;
            mode = TOUCH_MODE_COMMENT_DRAG;
            playerTouchBar.setSeekPosition((int) ((((float) lastTrackTime) / duration) * getWidth()), playerTouchBar.getHeight(), true);
            currentTimeDisplay.setByPercent(((float) lastTrackTime) / duration);
        } else {
            suspendTimeDisplay = false;
            mode = TOUCH_MODE_NONE;
            playerTouchBar.clearSeek();
            setCurrentTime(lastTrackTime);
        }
    }

    public void setProgress(long pos, float loadPercent) {
        setProgress(pos);
        if (duration > 0){
            setSecondaryProgress((int) (loadPercent * 10));
        } else {
            setSecondaryProgress(0);
        }
    }

    public void setProgress(long pos) {
        if (pos < 0) return;

        if (duration > 0){
            lastProgressTimestamp = System.currentTimeMillis();
            lastTrackTime = pos;
            if (mode != TOUCH_MODE_SEEK_DRAG){
                setProgressInternal(pos);
            }
        } else {
            setProgressInternal(0);
        }
    }

    protected void setProgressInternal(long pos) {
        if (duration <= 0)
            return;

        progressBar.setProgress((int) (pos * 1000 / duration));
        if (mode != TOUCH_MODE_SEEK_DRAG) {
            setCurrentTime(pos);
        }

        if (mode == TOUCH_MODE_NONE && currentTopComments != null) {
            final Comment last = lastCommentBeforeTimestamp(pos);
            if (last != null) {
                if (lastAutoComment != last && pos - last.timestamp < 2000) {
                    // todo, isShown expensive?? it traverses up the hierarchy
                    if (isShown() && (currentShowingComment == null ||
                                    (currentShowingComment == lastAutoComment &&
                                    last.timestamp - lastAutoComment.timestamp > MIN_COMMENT_DISPLAY_TIME))) {
                        if (!suppressComments){
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    autoShowComment(last);
                                }
                            });
                        }
                        lastAutoComment = last;
                    }
                }
            }
        }
    }

    private void setCurrentTime(final long pos){
        if (mode != TOUCH_MODE_SEEK_DRAG && !suspendTimeDisplay) {
            if (getWidth() == 0) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        currentTimeDisplay.setCurrentTime(pos);
                    }
                });
            } else {
                currentTimeDisplay.setCurrentTime(pos);
            }
        }
    }

    protected void autoShowComment(Comment c) {
        autoCloseComment();
        cancelAutoCloseComment();

        currentShowingComment = c;
        showCurrentComment(false);

        final Comment nextComment = nextCommentAfterTimestamp(currentShowingComment.timestamp);
        if (nextComment != null) prefetchAvatar(nextComment);
        handler.postDelayed(mAutoCloseComment, CLOSE_COMMENT_DELAY);
    }

    public void prefetchAvatar(Comment comment) {
        imageOperations.prefetch(ApiImageSize.formatUriForList(getContext(), comment.getUser().getNonDefaultAvatarUrl()));
    }

    public void setSecondaryProgress(int percent) {
        progressBar.setSecondaryProgress(percent);
    }

    public void updateTrack(@Nullable final Track track, int queuePosition, boolean visibleNow) {
        this.queuePosition = queuePosition;
        if (track == null || (this.track != null
                && this.track.getId() == track.getId()
                && waveformState != WaveformState.ERROR
                && duration == this.track.duration)) {
            return;
        }

        final boolean changed = this.track != track;
        this.track = track;
        duration = this.track.duration;
        currentTimeDisplay.setDuration(duration);

        if (changed) {
            hideWaiting();
            clearTrackComments();
            setProgress(0, 0);
        }

        if (!track.hasWaveform()) {
            Log.w(TAG, "track " + track.title + " has no waveform");
            waveformState = WaveformState .ERROR;
            overlay.setBackgroundColor(OVERLAY_BG_COLOR);
            onDoneLoadingWaveform(false, false);
            return;
        }

        // loading
        showWaiting();
        waveformState = WaveformState.LOADING;
        overlay.setVisibility(View.INVISIBLE);
        currentTimeDisplay.setVisibility(View.INVISIBLE);

        waveformOperations.waveformDataFor(track)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DefaultSubscriber<WaveformResult>() {
                    @Override
                    public void onError(Throwable e) {
                        if (track.equals(WaveformControllerLayout.this.track)) {
                            waveformState = WaveformState.ERROR;
                            WaveformControllerLayout.this.onWaveformError();
                        }
                    }

                    @Override
                    public void onNext(WaveformResult args) {
                        if (track.equals(WaveformControllerLayout.this.track)) {
                            waveformErrorCount = 0;
                            waveformState = WaveformState.OK;
                            overlay.setBackgroundDrawable(new WaveformDrawable(args.getWaveformData(), waveformColor, !isLandscape()));
                            onDoneLoadingWaveform(true, !args.isFromCache() && onScreen);
                        }
                    }
                });
    }

    protected void showCurrentComment(boolean userTriggered) {
        if (currentShowingComment != null) {
            playerAvatarBar.setCurrentComment(currentShowingComment);
            commentLines.setCurrentComment(currentShowingComment);

            CommentPanelLayout commentPanel = new CommentPanelLayout(getContext(), imageOperations, false);
            commentPanel.setListener(this);
            commentPanel.showComment(currentShowingComment);
            commentPanel.interacted = userTriggered;
            currentCommentPanel = commentPanel;
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ABOVE, waveformHolder.getId());
            lp.bottomMargin = (int) -(getResources().getDisplayMetrics().density * 10);
            waveformFrame.addView(commentPanel, waveformFrame.indexOfChild(currentTimeDisplay), lp);

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

        currentShowingComment = null;
        if (playerAvatarBar != null) playerAvatarBar.setCurrentComment(null);
        if (commentLines != null) commentLines.setCurrentComment(null);

        if (currentCommentPanel != null) {
            Animation animation = new AlphaAnimation(1.0f, 0.0f);
            animation.setDuration(300);
            currentCommentPanel.setAnimation(animation);
            waveformFrame.removeView(currentCommentPanel);
            currentCommentPanel = null;
        }
    }

    protected void autoCloseComment() {
        if (currentCommentPanel != null && currentShowingComment != null)
            if (currentShowingComment == currentCommentPanel.getComment() && !currentCommentPanel.interacted) {
                closeComment(false);
            }
    }

    protected void cancelAutoCloseComment() {
        handler.removeCallbacks(mAutoCloseComment);
    }

    final Runnable mAutoCloseComment = new Runnable() {
        public void run() {
            autoCloseComment();
        }
    };

    public void nextCommentInThread() {
        if (currentShowingComment != null && currentShowingComment.nextComment != null) {
            final Comment nextComment = currentShowingComment.nextComment;
            if (!isLandscape()) closeComment(false);
            currentShowingComment = nextComment;
            showCurrentComment(true);
        }
    }

    public void clearTrackComments() {
        cancelAutoCloseComment();
        closeComment(false);

        if (playerAvatarBar != null) {
            playerAvatarBar.setVisibility(View.INVISIBLE);
            playerAvatarBar.clearTrackData();
        }
        if (commentLines != null) {
            commentLines.setVisibility(View.INVISIBLE);
            commentLines.clearTrackData();
        }

        currentComments = null;
        currentTopComments = null;
        if (mode == TOUCH_MODE_AVATAR_DRAG) mode = TOUCH_MODE_NONE;
    }

    private void onWaveformError() {
        waveformErrorCount++;
        if (waveformErrorCount < MAX_WAVEFORM_RETRIES) {
            updateTrack(track, queuePosition, onScreen);
        } else {
            overlay.setBackgroundColor(OVERLAY_BG_COLOR);
            onDoneLoadingWaveform(false, onScreen);
        }
    }


    private void onDoneLoadingWaveform(boolean success, boolean animate) {
        if (!isBuffering) hideWaiting();

        final AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
        aa.setDuration(500);

        // only show the image if the load was successful, otherwise it will obscure the progress
        if (success && overlay.getVisibility() != View.VISIBLE) {
            if (animate) overlay.startAnimation(aa);
            overlay.setVisibility(View.VISIBLE);
        }

        if (currentTimeDisplay.getVisibility() != View.VISIBLE) {
            if (animate) currentTimeDisplay.startAnimation(aa);
            currentTimeDisplay.setVisibility(View.VISIBLE);
        }
    }


    private @Nullable Comment lastCommentBeforeTimestamp(long timestamp) {
        for (Comment comment : currentTopComments)
            if (comment.timestamp < timestamp)
                return comment;

        return null;
    }

    protected @Nullable Comment nextCommentAfterTimestamp(long timestamp) {
        if (currentTopComments != null) {
            for (int i = currentTopComments.size() - 1; i >= 0; i--) {
                if (currentTopComments.get(i).timestamp > timestamp)
                    return currentTopComments.get(i);
            }
        }
        return null;
    }


    public void setComments(List<Comment> comments, boolean animateIn) {
        setComments(comments, animateIn, false);
    }

    public void setComments(List<Comment> comments, boolean animateIn, boolean forceRefresh) {
        if (comments.equals(currentComments) && !forceRefresh){
            return;
        }
        currentComments = comments;
        currentTopComments = getTopComments(comments, duration);

        if (playerAvatarBar != null) {
            playerAvatarBar.setTrackData(duration, comments);
            playerAvatarBar.invalidate();
        }

        if (commentLines != null) {
            commentLines.setTrackData(duration, comments);
            commentLines.invalidate();
        }

        if (playerAvatarBar != null && playerAvatarBar.getVisibility() == View.INVISIBLE) {
            if (animateIn) {
                AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
                aa.setStartOffset(500);
                aa.setDuration(500);

                playerAvatarBar.startAnimation(aa);
                commentLines.startAnimation(aa);
            }

            playerAvatarBar.setVisibility(View.VISIBLE);
            commentLines.setVisibility(View.VISIBLE);
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
            seekPercent = adjustPosToBounds(((float) input.x) / waveformHolder.getWidth());
            queueUnique(UI_UPDATE_COMMENT_POSITION);

        } else if (input.view == playerTouchBar && new PlaybackStateProvider().isSeekable()) {
            mode = TOUCH_MODE_SEEK_DRAG;
            lastAutoComment = null; //reset auto comment in case they seek backward
            seekPercent = adjustPosToBounds(((float) input.x) / waveformHolder.getWidth());
            queueUnique(UI_UPDATE_SEEK);
        }
    }

    @Override
    protected void processMoveInput(InputObject input) {
        switch (mode) {
            case TOUCH_MODE_COMMENT_DRAG:
                if (isOnTouchBar(input.y)) {
                    seekPercent = adjustPosToBounds(((float) input.x) / waveformHolder.getWidth());
                    queueUnique(UI_UPDATE_COMMENT_POSITION);
                }
                break;
            case TOUCH_MODE_SEEK_DRAG:
                if (isOnTouchBar(input.y)) {
                    seekPercent = adjustPosToBounds(((float) input.x) / waveformHolder.getWidth());
                    queueUnique(UI_UPDATE_SEEK);
                } else {
                    queueUnique(UI_CLEAR_SEEK);
                    mode = TOUCH_MODE_SEEK_CLEAR_DRAG;
                }
                break;

            case TOUCH_MODE_SEEK_CLEAR_DRAG:
                if (isOnTouchBar(input.y)) {
                    seekPercent = adjustPosToBounds(((float) input.x) / waveformHolder.getWidth());
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
                    Comment comment = Comment.build(
                            track,
                            SoundCloudApplication.fromContext(getContext()).getAccountOperations().getLoggedInUser(),
                            stampFromPosition(input.x),
                            "",
                            0,
                            "");
                    getContext().sendBroadcast(new Intent(Comment.ACTION_CREATE_COMMENT)
                            .putExtra(Comment.EXTRA, comment));
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

    private float adjustPosToBounds(float f){
        return Math.min(1f, Math.max(0f, f));
    }

    @Override
    protected void processPointer1DownInput(InputObject input) {
    }

    @Override
    protected void processPointer1UpInput(InputObject input) {
    }

    private boolean isOnTouchBar(int y){
        return (y > playerTouchBar.getTop() - touchSlop && y < playerTouchBar.getBottom() + touchSlop);
    }

    protected void queueUnique(int what) {
        if (!touchHandler.hasMessages(what)) touchHandler.sendEmptyMessage(what);
    }

    public void onDestroy() {
        super.onDestroy();

        if (currentComments != null) {
            for (Comment c : currentComments) {
                c.xPos = -1;
            }
        }
        playerAvatarBar.clearTrackData();
        commentLines.clearTrackData();
    }

    protected long stampFromPosition(int x) {
        return (long) (Math.min(Math.max(.001, (((float) x) / getWidth())), 1) * track.duration);
    }

    public void showNewComment(Comment c) {
        if (c.xPos == -1){
            if (getWidth() == 0 || duration <= 0) return;
            c.calculateXPos(getWidth(), duration);
        }
        if (currentCommentPanel != null && currentShowingComment != null) closeComment(false);
        currentShowingComment = lastAutoComment = c;
        showCurrentComment(false);
        handler.postDelayed(mAutoCloseComment, CLOSE_COMMENT_DELAY);
    }

    public void onDataConnected() {
        if (waveformState == WaveformState.ERROR) {
            updateTrack(track, queuePosition, onScreen);
        }
    }

    public enum WaveformState {
        OK, LOADING, ERROR
    }


    private void processTouchMessage(int what){
        final PlayerTouchBarView touchBar = playerTouchBar;

        switch (what) {
            case UI_UPDATE_SEEK:
                if (listener != null) {
                    long seekTime = listener.setSeekMarker(queuePosition, seekPercent);
                    if (seekTime == -1) {
                        // the seek did not work, abort
                        mode = TOUCH_MODE_NONE;
                    } else {
                        playerTouchBar.setSeekPosition((int) (seekPercent * getWidth()), playerTouchBar.getHeight(), false);
                        currentTimeDisplay.setCurrentTime(seekTime);
                    }
                    waveformHolder.invalidate();
                }
                break;

            case UI_SEND_SEEK:
                onSeek((long) (seekPercent * duration));
                if (listener != null) {
                    listener.sendSeek(seekPercent);
                }
                playerTouchBar.clearSeek();
                break;

            case UI_CLEAR_SEEK:
                long progress = lastTrackTime + System.currentTimeMillis() - lastProgressTimestamp;
                setProgressInternal(progress);
                touchBar.clearSeek();
                break;

            case UI_UPDATE_COMMENT_POSITION:
                currentTimeDisplay.setByPercent(seekPercent);
                touchBar.setSeekPosition((int) (seekPercent * getWidth()), touchBar.getHeight(), true);
                break;

            case UI_UPDATE_COMMENT:
                if (showComment) {
                    showCurrentComment(true);
                } else {
                    closeComment(false);
                }
                break;
        }
    }


    private static final class TouchHandler extends Handler {
        private WeakReference<WaveformControllerLayout> mRef;

        private TouchHandler(WaveformControllerLayout controller) {
            this.mRef = new WeakReference<WaveformControllerLayout>(controller);
        }

        @Override
        public void handleMessage(Message msg) {
            final WaveformControllerLayout controller = mRef.get();
            if (controller != null) {
                controller.processTouchMessage(msg.what);
            }
        }
    }
}
