package com.soundcloud.android.view.play;

import static com.soundcloud.android.imageloader.ImageLoader.Options;
import static com.soundcloud.android.utils.AnimUtils.runFadeInAnimationOn;
import static com.soundcloud.android.utils.AnimUtils.runFadeOutAnimationOn;

import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.UserBrowser;
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
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewStub;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import java.util.List;

public class PlayerTrackView extends LinearLayout implements
        View.OnTouchListener,
        LoadCommentsTask.LoadCommentsListener {

    private ScPlayer mPlayer;

    private ImageView mArtwork, mAvatar;
    private ImageLoader.BindResult mCurrentArtBindResult;

    private WaveformController mWaveformController;
    private FrameLayout mUnplayableLayout;

    private TrackInfoBar mTrackInfoBar;
    private @Nullable ViewFlipper mTrackFlipper;
    private PlayerTrackDetails mTrackDetailsView;

    private boolean mDraggingLabel = false;
    private int mInitialX = -1;
    private int mLastX = -1;
    private int mTextWidth = 0;
    private int mViewWidth = 0;
    private int mTouchSlop;

    private ImageLoader.BindResult mCurrentAvatarBindResult;

    public Track mTrack;
    private int mQueuePosition;
    private long mDuration;
    private boolean mLandscape, mOnScreen;
    private boolean mIsCommenting;

    private ToggleButton mToggleLike;
    private ToggleButton mToggleComment;
    private ToggleButton mToggleRepost;
    private ImageButton mShareButton;

    private View mArtworkOverlay;

    public PlayerTrackView(ScPlayer player) {
        super(player);

        LayoutInflater inflater = (LayoutInflater) player.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_track, this);

        setOrientation(LinearLayout.VERTICAL);

        mPlayer = player;

        mTrackInfoBar = (TrackInfoBar) findViewById(R.id.track_info_bar);
        mTrackInfoBar.setEnabled(false);
        mTrackFlipper = (ViewFlipper) findViewById(R.id.vfTrackInfo);


        mTrackInfoBar.addTextShadows();
        mArtwork = (ImageView) findViewById(R.id.artwork);
        if (mArtwork != null) {
            mArtwork.setVisibility(View.INVISIBLE);
            mArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mArtwork.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onTrackDetailsFlip();
                }
            });
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
                    Intent intent = new Intent(getContext(), UserBrowser.class);
                    intent.putExtra("userId", mTrack.user_id);
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
        mToggleLike.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mTrack.user_like != isChecked) mPlayer.toggleLike(mTrack);
            }
        });

        mToggleRepost = (ToggleButton) findViewById(R.id.toggle_repost);
        mToggleRepost.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mTrack.user_repost != isChecked) mPlayer.toggleRepost(mTrack);
            }
        });

        mToggleComment = (ToggleButton) findViewById(R.id.toggle_comment);
        mToggleComment.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isChecked() != mIsCommenting) setCommentMode(isChecked, true);
            }
        });

        mShareButton = (ImageButton) findViewById(R.id.btn_share);
        mShareButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrack != null && mTrack.isPublic()) {
                    Intent shareIntent = mTrack.getShareIntent();
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                            mTrack.title + (mTrack.user != null ? " by " + mTrack.user.username : "") + " on SoundCloud");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, mTrack.permalink_url);
                    mPlayer.startActivity(shareIntent);
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
        if (mTrackDetailsView != null) mTrackDetailsView.setPlayingTrack(mTrack);
        updateAvatar(priority);

        if (mDuration != mTrack.duration) {
            mDuration = mTrack.duration;
        }

        final String commentCount = mTrack.comment_count > 0 ? String.valueOf(mTrack.comment_count) : "0";
        mToggleComment.setTextOff(commentCount);
        mToggleComment.setTextOn(commentCount);
        mToggleComment.setChecked(mIsCommenting);

        final String repostsCount = mTrack.reposts_count > 0 ? String.valueOf(mTrack.reposts_count) : "0";
        mToggleRepost.setTextOff(repostsCount);
        mToggleRepost.setTextOn(repostsCount);
        mToggleRepost.setChecked(mTrack.user_repost);


        final String likesCount = mTrack.likes_count > 0 ? String.valueOf(mTrack.likes_count) : "0";
        mToggleLike.setTextOff(likesCount);
        mToggleLike.setTextOn(likesCount);
        mToggleLike.setChecked(mTrack.user_like);

        mShareButton.setVisibility(mTrack.isPublic() ? View.VISIBLE : View.GONE);

        setAssociationStatus();

        if ((mTrack.isWaitingOnState() || mTrack.isStreamable()) && mTrack.last_playback_error == -1) {
            hideUnplayable();
        } else {
            showUnplayable();
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

            if (mTrackFlipper != null && mTrackFlipper.getDisplayedChild() == 1) {
                onTrackDetailsFlip();
            }
        }
    }

    void refreshComments() {
        if (mTrack == null) return;
        if (AndroidUtils.isTaskFinished(mTrack.load_comments_task)) {
            mTrack.load_comments_task =
                    new LoadCommentsTask(mPlayer.getApp());
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
            mArtwork.setVisibility(View.INVISIBLE);
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
                mArtwork.setVisibility(View.INVISIBLE);
            } else {
                onArtworkSet(false);
            }
        }
    }

     private void onArtworkSet(boolean animate) {
        if (mArtwork.getVisibility() == View.INVISIBLE || mArtwork.getVisibility() == View.GONE) {
            if (animate) AnimUtils.runFadeInAnimationOn(getContext(), mArtwork);
            mArtwork.setVisibility(View.VISIBLE);
        }
    }

    private void updateAvatar(boolean postAtFront) {
        if (mTrack.hasAvatar()) {
            if ((mCurrentAvatarBindResult = ImageLoader.get(mPlayer).bind(
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
                    }, postAtFront ? Options.postAtFront() : new Options())) != ImageLoader.BindResult.OK) {
            }
        } else {
            ImageLoader.get(mPlayer).unbind(mAvatar);
        }
    }

    public void onTrackDetailsFlip() {
        if (mTrackFlipper.getDisplayedChild() == 0) {
            if (mTrack != null) {
                mPlayer.track(Page.Sounds_info__main, mTrack);
            }

            mWaveformController.closeComment(false);

            if (mTrackDetailsView == null) {
                mTrackDetailsView = new PlayerTrackDetails(mPlayer);
                mTrackDetailsView.setPlayingTrack(mTrack);
                mTrackFlipper.addView(mTrackDetailsView);
            }

            if (!mTrackDetailsView.getIsTrackInfoFilled()) mTrackDetailsView.fillTrackDetails();

            mTrackFlipper.setInAnimation(AnimUtils.inFromRightAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.setOutAnimation(AnimUtils.outToLeftAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.showNext();
        } else {
            mTrackFlipper.setInAnimation(AnimUtils.inFromLeftAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.setOutAnimation(AnimUtils.outToRightAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.showPrevious();
        }
    }

    public void onDataConnected() {
        if (mWaveformController.waveformResult == ImageLoader.BindResult.ERROR) {
            mWaveformController.updateTrack(mTrack, mQueuePosition, mOnScreen);
        }
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

    private TextView textViewForContainer(View v) {
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

        if (!mLandscape){
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
                if (target.getAnimation() == animation){
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
        if (mWaveformController != null) {
            mWaveformController.onDestroy();
        }
    }

    public boolean waveformVisible(){
        return (mTrackFlipper == null || mTrackFlipper.getDisplayedChild() == 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mTrackFlipper != null && keyCode == KeyEvent.KEYCODE_BACK &&
             mTrackFlipper.getDisplayedChild() != 0) {
            onTrackDetailsFlip();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    public WaveformController getWaveformController() {
        return mWaveformController;
    }

    private void showUnplayable() {
        if (mUnplayableLayout == null) {
            mUnplayableLayout = (FrameLayout) ((ViewStub) findViewById(R.id.stub_unplayable_layout)).inflate();
        }

        if (mUnplayableLayout != null){
            final TextView unplayableText = (TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt);
            if (unplayableText != null)  { // sometimes inflation error results in text NPE
                if (mTrack == null || mTrack.isStreamable()) {
                    int errorMessage = R.string.player_stream_error;
                    if (mTrack != null) {
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
        if (action.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {

            if (intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isSupposedToBePlaying, false)) {
                hideUnplayable();
                if (mTrack != null) mTrack.last_playback_error = -1;
            } else {
                mWaveformController.setPlaybackStatus(false, intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
            }

        } else if (action.equals(Sound.ACTION_TRACK_ASSOCIATION_CHANGED)) {
            if (mTrack != null && mTrack.id == intent.getLongExtra(CloudPlaybackService.BroadcastExtras.id, -1)) {
                mTrack.user_like = intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isLike, false);
                mTrack.user_repost = intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isRepost, false);
                setAssociationStatus();
            }

        } else if (action.equals(Sound.ACTION_SOUND_INFO_UPDATED)) {
            Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(intent.getLongExtra(CloudPlaybackService.BroadcastExtras.id, -1));
            if (t != null) {
                setTrack(t, mQueuePosition, true, mOnScreen);
                if (mTrackDetailsView != null) {
                    mTrackDetailsView.onInfoLoadSuccess();
                }
            }

        } else if (action.equals(Sound.ACTION_SOUND_INFO_ERROR)) {
            if (mTrackDetailsView != null) {
                mTrackDetailsView.onInfoLoadError();
            }

        } else if (action.equals(CloudPlaybackService.BUFFERING)) {
            onBuffering();
        } else if (action.equals(CloudPlaybackService.BUFFERING_COMPLETE)) {
            mWaveformController.onBufferingStop();
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false),
                    intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));

        } else if (action.equals(CloudPlaybackService.PLAYBACK_ERROR)) {
            mTrack.last_playback_error = ScPlayer.PlayerError.PLAYBACK_ERROR;
            mWaveformController.onBufferingStop();
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false),
                    intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
            showUnplayable();

        } else if (action.equals(CloudPlaybackService.STREAM_DIED)) {
            mTrack.last_playback_error = ScPlayer.PlayerError.STREAM_ERROR;
            mWaveformController.onBufferingStop();
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false),
                    intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
            showUnplayable();

        } else if (action.equals(CloudPlaybackService.TRACK_UNAVAILABLE)) {
            mTrack.last_playback_error = ScPlayer.PlayerError.TRACK_UNAVAILABLE;
            mWaveformController.onBufferingStop();
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false),
                    intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
            showUnplayable();
            // TODO consolidate error handling, remove duplication

        } else if (action.equals(CloudPlaybackService.COMMENTS_LOADED)) {
            mWaveformController.setComments(mTrack.comments, true);
        } else if (action.equals(CloudPlaybackService.SEEKING)) {
            mWaveformController.onSeek(intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, -1));
        } else if (action.equals(CloudPlaybackService.SEEK_COMPLETE)) {
            mWaveformController.onSeekComplete();
        }
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

        // Start showing smooth progress if we already aren't
        if (!mWaveformController.showingSmoothProgress() && showSmoothProgress){
            mWaveformController.startSmoothProgress();
        }
    }

    public void onStop(boolean killLoading) {
        mWaveformController.onStop(killLoading);
    }

    public void onBuffering() {
        if (mTrack != null) {
            mTrack.last_playback_error = -1;
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

    public void destroy() {
        clear();
        mWaveformController.onDestroy();
    }

    public void clear() {
        mOnScreen = false;
        onStop(true);
        if (mArtwork != null) mArtwork.setImageBitmap(null);
        mAvatar.setImageBitmap(null);
        mWaveformController.reset(true);
        mWaveformController.setOnScreen(false);
    }

    @Nullable public Track getTrack() {
        return mTrack;
    }

    public boolean onBackPressed() {
        if (mTrackFlipper != null && mTrackFlipper.getDisplayedChild() == 1) {
            onTrackDetailsFlip();
            return true;
        } else {
            return false;
        }
    }
}
