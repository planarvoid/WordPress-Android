package com.soundcloud.android.view.play;

import static com.soundcloud.android.imageloader.ImageLoader.Options;
import static com.soundcloud.android.utils.AnimUtils.runFadeInAnimationOn;
import static com.soundcloud.android.utils.AnimUtils.runFadeOutAnimationOn;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Sound;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.adapter.TrackInfoBar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewStub;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import java.lang.ref.SoftReference;
import java.util.List;

public class PlayerTrackView extends LinearLayout implements
        View.OnTouchListener,
        LoadCommentsTask.LoadCommentsListener {

    private ScPlayer mPlayer;

    private ImageView mArtwork, mAvatar;
    private FrameLayout mArtworkHolder;
    private ImageLoader.BindResult mCurrentArtBindResult;

    private WaveformController mWaveformController;
    private FrameLayout mUnplayableLayout;

    private TrackInfoBar mTrackInfoBar;
    private @Nullable ViewFlipper mTrackFlipper;            // can be null in landscape mode
    private @Nullable PlayerTrackDetails mTrackDetailsView; // ditto

    private boolean mDraggingLabel = false;
    private int mInitialX = -1;
    private int mLastX    = -1;
    private int mTextWidth = 0;
    private int mViewWidth = 0;
    private int mTouchSlop;

    private ImageLoader.BindResult mCurrentAvatarBindResult;

    private Track mTrack;
    private int mQueuePosition;
    private long mDuration;
    private final boolean mLandscape;
    private boolean mOnScreen;
    private boolean mIsCommenting;

    private ToggleButton mToggleLike;
    private ToggleButton mToggleComment;
    private ToggleButton mToggleRepost;
    private ToggleButton mToggleInfo;
    private ImageButton mShareButton;

    private SoftReference<Drawable> mArtworkBgDrawable;

    private View mArtworkOverlay;

    public PlayerTrackView(ScPlayer player) {
        super(player);
        View.inflate(player, R.layout.player_track, this);
        setOrientation(LinearLayout.VERTICAL);

        mPlayer = player;

        mTrackInfoBar = (TrackInfoBar) findViewById(R.id.track_info_bar);
        mTrackInfoBar.setEnabled(false);
        mTrackFlipper = (ViewFlipper) findViewById(R.id.vfTrackInfo);

        mTrackInfoBar.addTextShadows();
        mArtwork = (ImageView) findViewById(R.id.artwork);
        if (mArtwork != null) {
            mArtworkHolder = (FrameLayout) mArtwork.getParent();
            showDefaultArtwork();
            mArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);

            mLandscape = false;
        } else {
            mLandscape = true;
        }

        mAvatar = (ImageView) findViewById(R.id.icon);
        mAvatar.setBackgroundDrawable(getResources().getDrawable(R.drawable.avatar_badge));
        findViewById(R.id.track_info_clicker).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mTrack != null) {
                    // get a valid id somehow or don't bother
                    final long userId = mTrack.user != null ? mTrack.user.id : mTrack.user_id;
                    if (userId == -1) return;

                    if (mTrack.user != null) SoundCloudApplication.MODEL_MANAGER.cache(mTrack.user, ScResource.CacheUpdateMode.NONE);
                    Intent intent = new Intent(getContext(), UserBrowser.class)
                        .putExtra("userId", mTrack.user_id);
                    getContext().startActivity(intent);
                }
            }
        });

        mArtworkOverlay   = findViewById(R.id.artwork_overlay);

        final OnClickListener closeCommentListener = new OnClickListener(){
            @Override
            public void onClick(View v) {
                if (mIsCommenting) setCommentMode(false);
            }
        };


        if (mArtworkOverlay != null) mArtworkOverlay.setOnClickListener(closeCommentListener);

        findViewById(R.id.private_indicator).setVisibility(View.GONE);

        mToggleLike = (ToggleButton) findViewById(R.id.toggle_like);
        mToggleLike.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayer.toggleLike(mTrack);
            }
        });

        mToggleRepost = (ToggleButton) findViewById(R.id.toggle_repost);
        mToggleRepost.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlayer.toggleRepost(mTrack);
            }
        });

        mToggleComment = (ToggleButton) findViewById(R.id.toggle_comment);
        mToggleComment.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setCommentMode(mToggleComment.isChecked(), true);
            }
        });

        mToggleInfo = (ToggleButton) findViewById(R.id.toggle_info);
        if (mToggleInfo != null) {
            mToggleInfo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mTrackFlipper != null) {
                        onTrackDetailsFlip(mTrackFlipper, mToggleInfo.isChecked());
                    }
                }
            });
        }

        mShareButton = (ImageButton) findViewById(R.id.btn_share);
        mShareButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrack != null) {
                    Intent shareIntent = mTrack.getShareIntent();
                    if (shareIntent != null) {
                        mPlayer.startActivity(shareIntent);
                    }
                }
            }
        });

        ((ProgressBar) findViewById(R.id.progress_bar)).setMax(1000);
        mWaveformController = (WaveformController) findViewById(R.id.waveform_controller);
        mWaveformController.setPlayerTrackView(this);

        ((View) findViewById(R.id.track).getParent()).setOnTouchListener(this);
        mTouchSlop = ViewConfiguration.get(mPlayer).getScaledTouchSlop();
    }

    public void setOnScreen(boolean onScreen){
        mOnScreen = onScreen;
        mWaveformController.setOnScreen(onScreen);
    }

    public void setTrack(@Nullable Track track, int queuePosition, boolean forceUpdate, boolean priority) {
        mQueuePosition = queuePosition;

        final boolean changed = mTrack != track;
        if (!(forceUpdate || changed)) return;

        mTrack = track;
        if (mTrack == null) {
            mShareButton.setVisibility(View.GONE);
            mWaveformController.clearTrackComments();
            return;
        }

        if (changed && !mLandscape) updateArtwork(priority);
        mWaveformController.updateTrack(mTrack, queuePosition, priority);

        mTrackInfoBar.display(mTrack, -1, false, true, false);
        if (mTrackDetailsView != null) mTrackDetailsView.fillTrackDetails(mTrack);
        updateAvatar(priority);

        if (mDuration != mTrack.duration) {
            mDuration = mTrack.duration;
        }

        setTrackStats(mToggleComment, mTrack.comment_count, mIsCommenting);
        setTrackStats(mToggleRepost, mTrack.reposts_count, mTrack.user_repost);
        setTrackStats(mToggleLike, mTrack.likes_count, mTrack.user_like);

        mShareButton.setVisibility(mTrack.isPublic() ? View.VISIBLE : View.GONE);

        setAssociationStatus();

        if ((mTrack.isWaitingOnState() || mTrack.isStreamable()) && mTrack.last_playback_error == -1) {
            hideUnplayable();
        } else {
            showUnplayable(mTrack);
            mWaveformController.onBufferingStop();
        }

        if (changed) {
            mWaveformController.clearTrackComments();
            mWaveformController.setProgress(0);

            if (mTrack.comments != null) {
                mWaveformController.setComments(mTrack.comments, true);
            } else {
                refreshComments();
            }

            if (mTrackFlipper != null) {
                onTrackDetailsFlip(mTrackFlipper, false);
            }
        }
    }

    private void setTrackStats(ToggleButton button, int count, boolean checked) {
        final String countString = count < 0 ? "\u2014" : String.valueOf(count);
        button.setTextOff(countString);
        button.setTextOn(countString);
        button.setChecked(checked);
    }

    private void refreshComments() {
        if (mTrack == null) return;
        if (AndroidUtils.isTaskFinished(mTrack.load_comments_task)) {
            mTrack.load_comments_task = new LoadCommentsTask(mPlayer.getApp());
        }
        mTrack.load_comments_task.addListener(this);
        if (AndroidUtils.isTaskPending(mTrack.load_comments_task)) {
            mTrack.load_comments_task.execute(mTrack.id);
        }
    }

    public void onCommentsLoaded(long track_id, List<Comment> comments){
        if (mTrack != null && mTrack.id == track_id){
            mTrack.comments = comments;
            mWaveformController.setComments(mTrack.comments, true);
        }
    }

    private void updateArtwork(boolean postAtFront) {
        ImageLoader.get(getContext()).unbind(mArtwork);
        if (TextUtils.isEmpty(mTrack.getArtwork())) {
            // no artwork
            showDefaultArtwork();
        } else {
            // executeAppendTask artwork as necessary
            if ((mCurrentArtBindResult = ImageUtils.loadImageSubstitute(
                    getContext(),
                    mArtwork,
                    mTrack.getArtwork(),
                    Consts.GraphicSize.getPlayerGraphicSize(getContext()),
                    new ImageLoader.Callback() {
                        @Override
                        public void onImageError(ImageView view, String url, Throwable error) {
                            mCurrentArtBindResult = ImageLoader.BindResult.ERROR;
                            Log.e(getClass().getSimpleName(), "Error loading artwork " + error);
                        }

                        @Override
                        public void onImageLoaded(ImageView view, String url) {
                            onArtworkSet(mOnScreen);
                        }
            }, postAtFront ? Options.postAtFront() : new Options())) != ImageLoader.BindResult.OK) {
                showDefaultArtwork();
            } else {
                onArtworkSet(false);
            }
        }
    }

    private void showDefaultArtwork() {
        if (mArtwork != null) {
            mArtwork.setVisibility(View.GONE);
            mArtwork.setImageDrawable(null);
            if (mArtworkBgDrawable == null || mArtworkBgDrawable.get() == null){
                try {
                    mArtworkBgDrawable = new SoftReference<Drawable>(getResources().getDrawable(R.drawable.artwork_player));
                } catch (OutOfMemoryError e){}
            }
        }
        if (mArtworkBgDrawable == null || mArtworkBgDrawable.get() == null) {
            mArtwork.setBackgroundColor(0xFFFFFFFF);
        } else {
            mArtworkHolder.setBackgroundDrawable(mArtworkBgDrawable.get());
        }
    }

    private void onArtworkSet(boolean animate) {
        if (mArtwork.getVisibility() != View.VISIBLE) { // keep this, presents flashing on second load
            if (animate) {
                AnimUtils.runFadeInAnimationOn(getContext(), mArtwork);
                mArtwork.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (animation.equals(mArtwork.getAnimation())) mArtworkHolder.setBackgroundDrawable(null);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                mArtwork.setVisibility(View.VISIBLE);
            } else {
                mArtwork.setVisibility(View.VISIBLE);
                mArtworkHolder.setBackgroundDrawable(null);
            }
        }
    }

    private void updateAvatar(boolean postAtFront) {
        if (mTrack.hasAvatar()) {
            mCurrentAvatarBindResult = ImageLoader.get(mPlayer).bind(
                    mAvatar,
                    Consts.GraphicSize.formatUriForList(mPlayer, mTrack.getAvatarUrl()),
                    new ImageLoader.Callback() {
                        @Override
                        public void onImageError(ImageView view, String url, Throwable error) {
                            mCurrentAvatarBindResult = ImageLoader.BindResult.ERROR;
                        }

                        @Override
                        public void onImageLoaded(ImageView view, String url) {
                        }
                    }, postAtFront ? Options.postAtFront() : new Options());
        } else {
            ImageLoader.get(mPlayer).unbind(mAvatar);
        }
    }

    public void onTrackDetailsFlip(@NotNull ViewFlipper trackFlipper, boolean showDetails) {
        if (showDetails && trackFlipper.getDisplayedChild() == 0) {
            if (mIsCommenting) setCommentMode(false, true);

            if (mTrack != null) {
                mPlayer.track(Page.Sounds_info__main, mTrack);
            }
            mWaveformController.closeComment(false);
            if (mTrackDetailsView == null) {
                mTrackDetailsView = new PlayerTrackDetails(mPlayer);
                trackFlipper.addView(mTrackDetailsView);
            }

            // according to this logic, we will only load the info if we haven't yet or there was an error
            // there is currently no manual or stale refresh logic
            if (mTrack != null) {
                if (mTrack.shouldLoadInfo()) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!mPlayer.isFinishing()) {
                                mPlayer.startService(new Intent(CloudPlaybackService.LOAD_TRACK_INFO).putExtra(Track.EXTRA_ID, mTrack.id));
                            }
                        }
                    }, 400); //flipper animation time is 250, so this should be enough to allow the animation to end

                    mTrackDetailsView.fillTrackDetails(mTrack, true);
                } else {
                    mTrackDetailsView.fillTrackDetails(mTrack);
                }
            }

            trackFlipper.setInAnimation(AnimUtils.inFromRightAnimation(new AccelerateDecelerateInterpolator()));
            trackFlipper.setOutAnimation(AnimUtils.outToLeftAnimation(new AccelerateDecelerateInterpolator()));
            trackFlipper.showNext();
        } else if (!showDetails && trackFlipper.getDisplayedChild() == 1){
            trackFlipper.setInAnimation(AnimUtils.inFromLeftAnimation(new AccelerateDecelerateInterpolator()));
            trackFlipper.setOutAnimation(AnimUtils.outToRightAnimation(new AccelerateDecelerateInterpolator()));
            trackFlipper.showPrevious();
        }
        if (mToggleInfo != null) mToggleInfo.setChecked(showDetails);
    }

    public void onDataConnected() {
        mWaveformController.onDataConnected();

        if (!mLandscape && mCurrentArtBindResult == ImageLoader.BindResult.ERROR) {
            updateArtwork(mOnScreen);
        }

        if (mCurrentAvatarBindResult == ImageLoader.BindResult.ERROR) {
            updateAvatar(mOnScreen);
        }
    }

    private void setAssociationStatus() {
        if (mTrack != null){
            mToggleLike.setChecked(mTrack.user_like);
            mToggleRepost.setChecked(mTrack.user_repost);
        }
    }

    /**
     * Handle text dragging for viewing of long track names
     */
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        TextView tv = textViewForContainer(v);
        if (tv == null) {
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            mInitialX = mLastX = (int) event.getX();
            mDraggingLabel = false;
            return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mDraggingLabel) {
                Message msg = mLabelScroller.obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(msg, 1000);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mDraggingLabel) {
                int scrollx = tv.getScrollX();
                int x = (int) event.getX();
                int delta = mLastX - x;
                if (delta != 0) {
                    mLastX = x;
                    scrollx += delta;
                    if (scrollx > mTextWidth) {
                        // scrolled the text completely off the view to the left
                        scrollx -= mTextWidth;
                        scrollx -= mViewWidth;
                    }
                    if (scrollx < -mViewWidth) {
                        // scrolled the text completely off the view to the right
                        scrollx += mViewWidth;
                        scrollx += mTextWidth;
                    }
                    tv.scrollTo(scrollx, 0);
                }
                return true;
            }
            int delta = mInitialX - (int) event.getX();
            if (Math.abs(delta) > mTouchSlop) {
                // start moving
                mLabelScroller.removeMessages(0, tv);

                // Only turn ellipsizing off when it's not already off, because it
                // causes the scroll position to be reset to 0.
                if (tv.getEllipsize() != null) {
                    tv.setEllipsize(null);
                }
                Layout ll = tv.getLayout();
                // layout might be null if the text just changed, or ellipsizing was just turned off
                if (ll == null) {
                    return false;
                }
                // get the non-ellipsized line width, to determine whether
                // scrolling should even be allowed
                mTextWidth = (int) tv.getLayout().getLineWidth(0);
                mViewWidth = tv.getWidth();
                if (mViewWidth > mTextWidth) {
                    tv.setEllipsize(TextUtils.TruncateAt.END);
                    v.cancelLongPress();
                    return false;
                }
                mDraggingLabel = true;
                tv.setHorizontalFadingEdgeEnabled(true);
                v.cancelLongPress();
                return true;
            }
        }
        return false;
    }

    private final Handler mLabelScroller = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView tv = (TextView) msg.obj;
            int x = tv.getScrollX();
            x = x * 3 / 4;
            tv.scrollTo(x, 0);
            if (x == 0) {
                tv.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                Message newmsg = obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(newmsg, 15);
            }
        }
    };

    private @Nullable TextView textViewForContainer(View v) {
        View vv = v.findViewById(R.id.track);
        if (vv != null) {
            return (TextView) vv;
        }
        return null;
    }

    public void setCommentMode(boolean  isCommenting) {
        setCommentMode(isCommenting, true);
    }

    public void setCommentMode(boolean isCommenting, boolean animated) {
        mIsCommenting = isCommenting;
        getWaveformController().setCommentMode(isCommenting);
        if (mIsCommenting != mToggleComment.isChecked()) mToggleComment.setChecked(mIsCommenting);

        if (mTrackFlipper != null && mIsCommenting) {
            onTrackDetailsFlip(mTrackFlipper, false);
        }

        if (!mLandscape) {
            if (animated) {
                if (isCommenting) {
                    mArtworkOverlay.setVisibility(VISIBLE);
                    runFadeInAnimationOn(mPlayer, mArtworkOverlay);
                } else {
                    runFadeOutAnimationOn(mPlayer, mArtworkOverlay);
                    attachVisibilityListener(mArtworkOverlay, GONE);
                }
            } else {
                int visibility = mIsCommenting ? VISIBLE : GONE;
                mArtworkOverlay.setVisibility(visibility);
            }
        }
    }

    private static void attachVisibilityListener(final View target, final int visibility) {
        target.getAnimation().setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                if (target.getAnimation().equals(animation)) {
                    target.setVisibility(visibility);
                    target.setEnabled(true);
                }
            }
        });
    }


    public int getPlayPosition() {
        return mQueuePosition;
    }

    public void onDestroy() {
        clear();
        mWaveformController.onDestroy();
    }

    public boolean waveformVisible(){
        return (mTrackFlipper == null || mTrackFlipper.getDisplayedChild() == 0);
    }

    public WaveformController getWaveformController() {
        return mWaveformController;
    }

    private void showUnplayable(Track track) {
        if (mUnplayableLayout == null) {
            mUnplayableLayout = (FrameLayout) ((ViewStub) findViewById(R.id.stub_unplayable_layout)).inflate();
        }

        if (mUnplayableLayout != null) {
            final TextView unplayableText = (TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt);
            if (unplayableText != null)  { // sometimes inflation error results in text NPE
                if (track == null || track.isStreamable()) {
                    int errorMessage = R.string.player_stream_error;
                    if (track != null) {
                        switch (mTrack.last_playback_error) {
                            case ScPlayer.PlayerError.PLAYBACK_ERROR:
                                errorMessage = R.string.player_error;
                                break;
                            case ScPlayer.PlayerError.TRACK_UNAVAILABLE:
                                errorMessage = R.string.player_track_unavailable;
                                break;
                        }
                    }
                    unplayableText.setText(errorMessage);
                } else {
                    unplayableText.setText(R.string.player_not_streamable);
                }
            }
        }
        mWaveformController.setVisibility(View.GONE);
        mUnplayableLayout.setVisibility(View.VISIBLE);
    }

    private void hideUnplayable() {
        mWaveformController.setVisibility(View.VISIBLE);
        if (mUnplayableLayout != null) mUnplayableLayout.setVisibility(View.GONE);
    }

    public void handleIdBasedIntent(Intent intent) {
        if (mTrack != null && mTrack.id == intent.getLongExtra("id", -1)) handleStatusIntent(intent);
    }

    public void handleStatusIntent(Intent intent) {
        String action = intent.getAction();
        if (CloudPlaybackService.PLAYSTATE_CHANGED.equals(action)) {

            if (intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isSupposedToBePlaying, false)) {
                hideUnplayable();
                if (mTrack != null) mTrack.last_playback_error = -1;
            } else {
                mWaveformController.setPlaybackStatus(false, intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
            }

        } else if (Sound.ACTION_TRACK_ASSOCIATION_CHANGED.equals(action)) {
            if (mTrack != null && mTrack.id == intent.getLongExtra(CloudPlaybackService.BroadcastExtras.id, -1)) {
                mTrack.user_like = intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isLike, false);
                mTrack.user_repost = intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isRepost, false);
                setTrackStats(mToggleRepost, mTrack.reposts_count, mTrack.user_repost);
                setTrackStats(mToggleLike, mTrack.likes_count, mTrack.user_like);
            }

        } else if (Sound.ACTION_SOUND_INFO_UPDATED.equals(action)) {
            Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(intent.getLongExtra(CloudPlaybackService.BroadcastExtras.id, -1));
            if (t != null) {
                setTrack(t, mQueuePosition, true, mOnScreen);
                if (mTrackDetailsView != null) {
                    mTrackDetailsView.fillTrackDetails(mTrack);
                }
            }

        } else if (Sound.ACTION_SOUND_INFO_ERROR.equals(action)) {
            if (mTrackDetailsView != null) {
                mTrackDetailsView.fillTrackDetails(mTrack);
            }

        } else if (CloudPlaybackService.BUFFERING.equals(action)) {
            onBuffering();
        } else if (CloudPlaybackService.BUFFERING_COMPLETE.equals(action)) {
            mWaveformController.onBufferingStop();
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false),
                    intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));

        } else if (CloudPlaybackService.PLAYBACK_ERROR.equals(action)) {
            mTrack.last_playback_error = ScPlayer.PlayerError.PLAYBACK_ERROR;
            onUnplayable(intent, mTrack);
        } else if (CloudPlaybackService.STREAM_DIED.equals(action)) {
            mTrack.last_playback_error = ScPlayer.PlayerError.STREAM_ERROR;
            onUnplayable(intent, mTrack);
        } else if (CloudPlaybackService.TRACK_UNAVAILABLE.equals(action)) {
            mTrack.last_playback_error = ScPlayer.PlayerError.TRACK_UNAVAILABLE;
            onUnplayable(intent, mTrack);
        } else if (CloudPlaybackService.COMMENTS_LOADED.equals(action)) {
            mWaveformController.setComments(mTrack.comments, true);
        } else if (CloudPlaybackService.SEEKING.equals(action)) {
            mWaveformController.onSeek(intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, -1));
        } else if (CloudPlaybackService.SEEK_COMPLETE.equals(action)) {
            mWaveformController.onSeekComplete();
        }
    }

    private void onUnplayable(Intent intent, Track track) {
        mWaveformController.onBufferingStop();
        mWaveformController.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false),
                intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));

        showUnplayable(track);
    }

    public void onNewComment(Comment comment) {
        if (comment.track_id == mTrack.id) {
            if (mTrack.comments != null) mWaveformController.setComments(mTrack.comments, false, true);
            mWaveformController.showNewComment(comment);
        }
    }


    public void setProgress(long pos, int loadPercent, boolean showSmoothProgress) {
        if (pos >= 0 && mDuration > 0) {
            mWaveformController.setProgress(pos);
            mWaveformController.setSecondaryProgress(loadPercent * 10);
        } else {
            mWaveformController.setProgress(0);
            mWaveformController.setSecondaryProgress(0);
        }

        // Onboard showing smooth progress if we already aren't
        if (!mWaveformController.showingSmoothProgress() && showSmoothProgress){
            mWaveformController.startSmoothProgress();
        }
    }

    public void onStop(boolean killLoading) {
        mWaveformController.onStop(killLoading);
    }

    public void onBuffering() {
        final Track track = getTrack();
        if (track != null) {
            track.last_playback_error = -1;
            hideUnplayable();
            mWaveformController.onBufferingStart();
            mWaveformController.stopSmoothProgress();
        }
    }

    public void setPlaybackStatus(boolean isPlaying, long position) {
        mWaveformController.setPlaybackStatus(isPlaying, position);
    }

    public long getTrackId() {
        return mTrack == null ? -1 : mTrack.id;
    }

    public void clear() {
        mOnScreen = false;
        onStop(true);
        showDefaultArtwork();
        mAvatar.setImageBitmap(null);
        mWaveformController.reset(true);
        mWaveformController.setOnScreen(false);
    }

    @Nullable public Track getTrack() {
        return mTrack;
    }

    public boolean onBackPressed() {
        if (mTrackFlipper != null && mTrackFlipper.getDisplayedChild() == 1) {
            onTrackDetailsFlip(mTrackFlipper, false);
            return true;
        } else if (mIsCommenting) {
            setCommentMode(false);
            return true;
        } else {
            return false;
        }
    }
}
