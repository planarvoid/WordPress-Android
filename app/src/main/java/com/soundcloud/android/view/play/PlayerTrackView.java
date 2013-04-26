package com.soundcloud.android.view.play;

import static com.soundcloud.android.imageloader.ImageLoader.Options;
import static com.soundcloud.android.utils.AnimUtils.runFadeInAnimationOn;
import static com.soundcloud.android.utils.AnimUtils.runFadeOutAnimationOn;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.dialog.MyPlaylistsDialogFragment;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.PlayableActionButtonsController;
import com.soundcloud.android.view.adapter.PlayableBar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import java.lang.ref.SoftReference;
import java.util.List;

@SuppressWarnings("deprecation")
public class PlayerTrackView extends LinearLayout implements LoadCommentsTask.LoadCommentsListener {

    private ScPlayer mPlayer;

    private @Nullable ImageView mArtwork;
    private ImageView mAvatar;
    private @Nullable FrameLayout mArtworkHolder;
    private ImageLoader.BindResult mCurrentArtBindResult;

    private WaveformController mWaveformController;
    private FrameLayout mUnplayableLayout;

    private PlayableBar mTrackInfoBar;
    private @Nullable ViewFlipper mTrackFlipper;            // can be null in landscape mode
    private @Nullable PlayerTrackDetails mTrackDetailsView; // ditto

    private ImageLoader.BindResult mCurrentAvatarBindResult;

    private @Nullable Track mTrack;
    private int mQueuePosition;
    private long mDuration;
    private final boolean mLandscape;
    private boolean mOnScreen;
    private boolean mIsCommenting;

    private ToggleButton mToggleInfo;

    private PlayableActionButtonsController mActionButtons;

    private SoftReference<Drawable> mArtworkBgDrawable;

    private View mArtworkOverlay;

    public PlayerTrackView(ScPlayer player) {
        super(player);
        View.inflate(player, R.layout.player_track, this);
        setOrientation(LinearLayout.VERTICAL);

        mPlayer = player;

        mTrackInfoBar = (PlayableBar) findViewById(R.id.playable_bar);
        mTrackFlipper = (ViewFlipper) findViewById(R.id.vfTrackInfo);

        findViewById(R.id.btn_addToSet).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrack != null && mPlayer.isForeground()){
                    MyPlaylistsDialogFragment.from(mTrack).show(
                            mPlayer.getSupportFragmentManager(), "playlist_dialog");
                }
            }
        });

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
        mTrackInfoBar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UserBrowser.startFromPlayable(getContext(),mTrack);
            }
        });

        mArtworkOverlay   = findViewById(R.id.artwork_overlay);

        final OnClickListener closeCommentListener = new OnClickListener(){
            @Override
            public void onClick(View v) {
                mPlayer.closeCommentMode();
            }
        };


        if (mArtworkOverlay != null) mArtworkOverlay.setOnClickListener(closeCommentListener);

        findViewById(R.id.playable_private_indicator).setVisibility(View.GONE);

        mToggleInfo = (ToggleButton) findViewById(R.id.toggle_info);
        if (mToggleInfo != null) {
            mToggleInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mTrackFlipper != null) {
                        onTrackDetailsFlip(mTrackFlipper, mToggleInfo.isChecked());
                    }
                }
            });
        }

        mActionButtons = new PlayableActionButtonsController(this);

        ((ProgressBar) findViewById(R.id.progress_bar)).setMax(1000);
        mWaveformController = (WaveformController) findViewById(R.id.waveform_controller);
        mWaveformController.setPlayerTrackView(this);
    }

    public void setOnScreen(boolean onScreen){
        mOnScreen = onScreen;
        mWaveformController.setOnScreen(onScreen);
    }

    public void setTrack(@NotNull Track track, int queuePosition, boolean forceUpdate, boolean priority) {
        final boolean changed = mTrack != track;
        if (!(forceUpdate || changed)) return;

        mQueuePosition = queuePosition;
        mTrack = track;

        mWaveformController.updateTrack(mTrack, queuePosition, priority);

        mTrackInfoBar.display(mTrack);
        if (mTrackDetailsView != null) mTrackDetailsView.fillTrackDetails(mTrack);
        updateAvatar(priority);

        if (mDuration != mTrack.duration) {
            mDuration = mTrack.duration;
        }

        mActionButtons.update(track);

        if ((mTrack.isWaitingOnState() || mTrack.isStreamable()) && mTrack.last_playback_error == -1) {
            hideUnplayable();
        } else {
            showUnplayable();
            mWaveformController.setBufferingState(false);
        }

        if (changed) {
            if (!mLandscape) updateArtwork(priority);
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

            setCommentMode(mPlayer.getCommentPosition() == queuePosition, false);

        }
    }

    private void refreshComments() {
        if (mTrack != null){
            if (AndroidUtils.isTaskFinished(mTrack.load_comments_task)) {
                mTrack.load_comments_task = new LoadCommentsTask(mPlayer.getApp());
            }
            mTrack.load_comments_task.addListener(this);
            if (AndroidUtils.isTaskPending(mTrack.load_comments_task)) {
                mTrack.load_comments_task.execute(mTrack.id);
            }
        }
    }

    public void onCommentsLoaded(long track_id, List<Comment> comments){
        if (mTrack != null && mTrack.id == track_id){
            mTrack.comments = comments;
            mWaveformController.setComments(mTrack.comments, true);
        }
    }

    private void updateArtwork(boolean postAtFront) {
        // this will cause OOMs
        if (mTrack == null || ActivityManager.isUserAMonkey()) return;

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
        if (mArtwork != null && mArtworkHolder != null) {
            mArtwork.setVisibility(View.GONE);
            mArtwork.setImageDrawable(null);
            if (mArtworkBgDrawable == null || mArtworkBgDrawable.get() == null){
                try {
                    mArtworkBgDrawable = new SoftReference<Drawable>(getResources().getDrawable(R.drawable.artwork_player));
                } catch (OutOfMemoryError ignored){}
            }

            final Drawable bg = mArtworkBgDrawable == null ? null : mArtworkBgDrawable.get();
            if (bg == null) {
                mArtwork.setBackgroundColor(0xFFFFFFFF);
            } else {
                mArtworkHolder.setBackgroundDrawable(bg);
            }
        }
    }

    private void onArtworkSet(boolean animate) {
        if (mArtwork != null && mArtworkHolder != null) {
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
    }

    private void updateAvatar(boolean postAtFront) {
        if (mTrack != null && mTrack.hasAvatar()) {
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
        if (mTrack != null && showDetails && trackFlipper.getDisplayedChild() == 0) {
            if (mIsCommenting) mPlayer.closeCommentMode();

            mPlayer.track(Page.Sounds_info__main, mTrack);
            mWaveformController.closeComment(false);
            if (mTrackDetailsView == null) {
                mTrackDetailsView = new PlayerTrackDetails(mPlayer);
                trackFlipper.addView(mTrackDetailsView);
            }

            // according to this logic, we will only load the info if we haven't yet or there was an error
            // there is currently no manual or stale refresh logic
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

            trackFlipper.setInAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.fade_in));
            trackFlipper.setOutAnimation(null);
            trackFlipper.showNext();
        } else if (!showDetails && trackFlipper.getDisplayedChild() == 1){
            trackFlipper.setInAnimation(null);
            trackFlipper.setOutAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.fade_out));
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

    public void setCommentMode(boolean  isCommenting) {
        setCommentMode(isCommenting, true);
    }

    public void setCommentMode(boolean isCommenting, boolean animated) {
        if (mIsCommenting != isCommenting){
            mIsCommenting = isCommenting;
            getWaveformController().setCommentMode(isCommenting);

            if (mTrackFlipper != null && mIsCommenting) {
                onTrackDetailsFlip(mTrackFlipper, false);
            }

            if (!mLandscape) {
                mArtworkOverlay.clearAnimation();
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

    private void showUnplayable() {
        if (mUnplayableLayout == null) {
            mUnplayableLayout = (FrameLayout) ((ViewStub) findViewById(R.id.stub_unplayable_layout)).inflate();
        }

        if (mUnplayableLayout != null) {
            final TextView unplayableText = (TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt);
            if (unplayableText != null)  { // sometimes inflation error results in text NPE
                if (mTrack == null || mTrack.isStreamable()) {
                    int errorMessage;
                    switch (mTrack == null ? -1 : mTrack.last_playback_error) {
                        case ScPlayer.PlayerError.PLAYBACK_ERROR:
                            errorMessage = R.string.player_error;
                            break;
                        case ScPlayer.PlayerError.TRACK_UNAVAILABLE:
                            errorMessage = R.string.player_track_unavailable;
                            break;
                        default:
                            errorMessage = R.string.player_stream_error;
                            break;
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
        if (mTrack == null) return;

        String action = intent.getAction();
        if (CloudPlaybackService.PLAYSTATE_CHANGED.equals(action)) {
            if (intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isSupposedToBePlaying, false)) {
                hideUnplayable();
                mTrack.last_playback_error = -1;
            } else {
                mWaveformController.setPlaybackStatus(false, intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
            }

        } else if (Playable.ACTION_PLAYABLE_ASSOCIATION_CHANGED.equals(action)) {
            if (mTrack.id == intent.getLongExtra(CloudPlaybackService.BroadcastExtras.id, -1)) {
                mTrack.user_like = intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isLike, false);
                mTrack.user_repost = intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isRepost, false);
                mActionButtons.update(mTrack);
            }

        } else if (Playable.COMMENTS_UPDATED.equals(action)) {
            if (mTrack.id == intent.getLongExtra(CloudPlaybackService.BroadcastExtras.id, -1)) {
                onCommentsChanged();
            }

        } else if (Playable.ACTION_SOUND_INFO_UPDATED.equals(action)) {
            Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(intent.getLongExtra(CloudPlaybackService.BroadcastExtras.id, -1));
            if (t != null) {
                setTrack(t, mQueuePosition, true, mOnScreen);
                if (mTrackDetailsView != null) {
                    mTrackDetailsView.fillTrackDetails(mTrack);
                }
            }

        } else if (Playable.ACTION_SOUND_INFO_ERROR.equals(action)) {
            if (mTrackDetailsView != null) {
                mTrackDetailsView.fillTrackDetails(mTrack);
            }

        } else if (CloudPlaybackService.BUFFERING.equals(action)) {
            setBufferingState(true);
        } else if (CloudPlaybackService.BUFFERING_COMPLETE.equals(action)) {
            setBufferingState(false);
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false),
                    intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));

        } else if (CloudPlaybackService.PLAYBACK_ERROR.equals(action)) {
            mTrack.last_playback_error = ScPlayer.PlayerError.PLAYBACK_ERROR;
            onUnplayable(intent);
        } else if (CloudPlaybackService.STREAM_DIED.equals(action)) {
            mTrack.last_playback_error = ScPlayer.PlayerError.STREAM_ERROR;
            onUnplayable(intent);
        } else if (CloudPlaybackService.TRACK_UNAVAILABLE.equals(action)) {
            mTrack.last_playback_error = ScPlayer.PlayerError.TRACK_UNAVAILABLE;
            onUnplayable(intent);
        } else if (CloudPlaybackService.COMMENTS_LOADED.equals(action)) {
            mWaveformController.setComments(mTrack.comments, true);
        } else if (CloudPlaybackService.SEEKING.equals(action)) {
            mWaveformController.onSeek(intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, -1));
        } else if (CloudPlaybackService.SEEK_COMPLETE.equals(action)) {
            mWaveformController.onSeekComplete();
        }
    }

    private void onUnplayable(Intent intent) {
        mWaveformController.setBufferingState(false);
        mWaveformController.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false),
                intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));

        showUnplayable();
    }

    public void onNewComment(Comment comment) {
        if (mTrack != null && comment.track_id == mTrack.id) {
            onCommentsChanged();
            mWaveformController.showNewComment(comment);
        }
    }

    private void onCommentsChanged() {
        if (mTrack != null && mTrack.comments != null) mWaveformController.setComments(mTrack.comments, false, true);
    }

    public void setProgress(long pos, int loadPercent, boolean showSmoothProgress) {
        if (pos >= 0 && mDuration > 0) {
            mWaveformController.setProgress(pos);
            mWaveformController.setSecondaryProgress(loadPercent * 10);
        } else {
            mWaveformController.setProgress(0);
            mWaveformController.setSecondaryProgress(0);
        }
        mWaveformController.setSmoothProgress(showSmoothProgress);
    }

    public void onStop(boolean killLoading) {
        mWaveformController.onStop(killLoading);
    }

    public void setBufferingState(boolean isBuffering) {
        mWaveformController.setBufferingState(isBuffering);

        if (isBuffering){
            hideUnplayable();

            // TODO: this needs to happen in the service, this should be UI only here
            if (mTrack != null) mTrack.last_playback_error = -1;
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
