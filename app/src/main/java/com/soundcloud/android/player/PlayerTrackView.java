package com.soundcloud.android.player;


import static com.soundcloud.android.service.playback.CloudPlaybackService.BroadcastExtras;
import static com.soundcloud.android.service.playback.CloudPlaybackService.Broadcasts;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.PlayerActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.api.OldCloudAPI;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.State;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.PlayableActionButtonsController;
import com.soundcloud.android.view.adapter.PlayableBar;
import com.soundcloud.android.view.play.PlayerTrackDetails;
import com.soundcloud.android.view.play.WaveformController;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import java.util.List;

@SuppressWarnings("deprecation")
public class PlayerTrackView extends LinearLayout implements LoadCommentsTask.LoadCommentsListener, WaveformController.WaveformListener {

    private ImageView mAvatar;
    private WaveformController mWaveformController;
    private FrameLayout mUnplayableLayout;

    private PlayableBar mTrackInfoBar;
    private @Nullable ViewFlipper mTrackFlipper;            // can be null in landscape mode
    private @Nullable
    PlayerTrackDetails mTrackDetailsView; // ditto

    @NotNull
    protected Track mTrack;
    private int mQueuePosition;
    private long mDuration;
    protected boolean mOnScreen;
    private boolean mIsCommenting;

    private ToggleButton mToggleInfo;
    private PlayableActionButtonsController mActionButtons;
    private AndroidCloudAPI oldCloudApi;
    @NotNull
    protected PlayerTrackViewListener mListener;

    public interface PlayerTrackViewListener extends WaveformController.WaveformListener {
        void onAddToPlaylist(Track track);
        void onCloseCommentMode();
    }

    public PlayerTrackView(Context context, AttributeSet attrs) {
        super(context, attrs);

        View.inflate(context,R.layout.player_track, this);

        mListener = (PlayerActivity) context;// NO!!!
        oldCloudApi = new OldCloudAPI(context.getApplicationContext());
        mTrackInfoBar = (PlayableBar) findViewById(R.id.playable_bar);
        mTrackFlipper = (ViewFlipper) findViewById(R.id.vfTrackInfo);

        findViewById(R.id.btn_addToSet).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onAddToPlaylist(mTrack);
            }
        });

        mTrackInfoBar.addTextShadows();

        mAvatar = (ImageView) findViewById(R.id.icon);
        mAvatar.setBackgroundDrawable(getResources().getDrawable(R.drawable.avatar_badge));
        mTrackInfoBar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UserBrowser.startFromPlayable(getContext(), mTrack);
            }
        });

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
        mWaveformController.setListener(mListener);
    }

    // TODO, this is currently true all the time
    public void setOnScreen(boolean onScreen){
        mOnScreen = onScreen;
        mWaveformController.setOnScreen(onScreen);
    }

    public void setPlayQueueItem(Observable<Track> trackObservable, int queuePosition){
        mQueuePosition = queuePosition;
        trackObservable.subscribe(new DefaultObserver<Track>() {
            @Override
            public void onNext(Track args) {
                // GET RID OF PRIORITY OR IMPLEMENT IT PROPERLY
                setTrackInternal(args, true);
            }
        });
    }

    @Override
    public long sendSeek(float seekPosition) {
        return mListener.sendSeek(seekPosition);
    }

    @Override
    public long setSeekMarker(int queuePosition, float seekPosition) {
        return mListener.setSeekMarker(queuePosition, seekPosition);
    }

    protected void setTrackInternal(@NotNull Track track, boolean priority) {
        final boolean changed = !track.equals(mTrack);

        mTrack = track;
        mWaveformController.updateTrack(mTrack, mQueuePosition, priority);
        mTrackInfoBar.display(mTrack);
        updateAvatar(priority);

        if (mTrackDetailsView != null) {
            mTrackDetailsView.fillTrackDetails(mTrack);
        }

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

        if (mTrack.comments != null) {
            mWaveformController.setComments(mTrack.comments, true);
        } else {
            refreshComments();
        }

        if (mTrackFlipper != null && changed) {
            onTrackDetailsFlip(mTrackFlipper, false);
        }

        if (mQueuePosition == CloudPlaybackService.getPlayPosition()){
            setProgress(CloudPlaybackService.getCurrentProgress(), CloudPlaybackService.getLoadingPercent(),
                    Consts.SdkSwitches.useSmoothProgress && CloudPlaybackService.getPlaybackState() == State.PLAYING);
        }


    }

    private void refreshComments() {
        if (mTrack != null){
            if (AndroidUtils.isTaskFinished(mTrack.load_comments_task)) {
                mTrack.load_comments_task = new LoadCommentsTask(oldCloudApi);
            }
            mTrack.load_comments_task.addListener(this);
            if (AndroidUtils.isTaskPending(mTrack.load_comments_task)) {
                mTrack.load_comments_task.execute(mTrack.getId());
            }
        }
    }

    public void onCommentsLoaded(long track_id, List<Comment> comments){
        if (mTrack != null && mTrack.getId() == track_id){
            mTrack.comments = comments;
            mWaveformController.setComments(mTrack.comments, true);
        }
    }

    public void onBeingScrolled(){
        mWaveformController.setSuppressComments(true);
    }

    public void onScrollComplete(){
        mWaveformController.setSuppressComments(false);
    }

    private void updateAvatar(boolean postAtFront) {
        if (mTrack != null && mTrack.getUser() != null) {
            final User user = mTrack.getUser();
            ImageLoader.getInstance().displayImage(user.getListAvatarUri(getContext()), mAvatar);
        } else {
            ImageLoader.getInstance().cancelDisplayTask(mAvatar);
        }
    }

    public void onTrackDetailsFlip(@NotNull ViewFlipper trackFlipper, boolean showDetails) {
        if (mTrack != null && showDetails && trackFlipper.getDisplayedChild() == 0) {
            mListener.onCloseCommentMode();

            SoundCloudApplication.fromContext(getContext()).track(Page.Sounds_info__main, mTrack);
            mWaveformController.closeComment(false);
            if (mTrackDetailsView == null) {
                mTrackDetailsView = new PlayerTrackDetails(getContext());
                trackFlipper.addView(mTrackDetailsView);
            }

            // according to this logic, we will only load the info if we haven't yet or there was an error
            // there is currently no manual or stale refresh logic
            if (mTrack.shouldLoadInfo()) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final Context context = getContext();
                        if (context != null){
                            context.startService(new Intent(CloudPlaybackService.Actions.LOAD_TRACK_INFO).putExtra(Track.EXTRA, mTrack));
                        }
                    }
                }, 400); //flipper animation time is 250, so this should be enough to allow the animation to end

                mTrackDetailsView.fillTrackDetails(mTrack, true);
            } else {
                mTrackDetailsView.fillTrackDetails(mTrack);
            }

            trackFlipper.setInAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.fade_in));
            trackFlipper.setOutAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.hold));
            trackFlipper.showNext();
        } else if (!showDetails && trackFlipper.getDisplayedChild() == 1){
            trackFlipper.setInAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.hold));
            trackFlipper.setOutAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.fade_out));
            trackFlipper.showPrevious();
        }
        if (mToggleInfo != null) mToggleInfo.setChecked(showDetails);
    }

    public void onDataConnected() {
        mWaveformController.onDataConnected();
    }

    public void setCommentMode(boolean  isCommenting) {
        setCommentMode(isCommenting, true);
    }

    public void setCommentMode(boolean isCommenting, boolean animated) {
        if (mIsCommenting != isCommenting){
            onCommentModeChanged(isCommenting, animated);
        }
    }

    protected void onCommentModeChanged(boolean isCommenting, boolean animated) {
        mIsCommenting = isCommenting;
        getWaveformController().setCommentMode(isCommenting);

        if (mTrackFlipper != null && mIsCommenting) {
            onTrackDetailsFlip(mTrackFlipper, false);
        }
    }

    public int getPlayPosition() {
        return mQueuePosition;
    }

    public void onDestroy() {
        clear();
        mWaveformController.onDestroy();
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
                        case PlayerActivity.PlayerError.PLAYBACK_ERROR:
                            errorMessage = R.string.player_error;
                            break;
                        case PlayerActivity.PlayerError.TRACK_UNAVAILABLE:
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
            mUnplayableLayout.setVisibility(View.VISIBLE);
        }
        mWaveformController.setVisibility(View.GONE);

    }

    private void hideUnplayable() {
        mWaveformController.setVisibility(View.VISIBLE);
        if (mUnplayableLayout != null) mUnplayableLayout.setVisibility(View.GONE);
    }

    public void handleIdBasedIntent(Intent intent) {
        if (mTrack != null && mTrack.getId() == intent.getLongExtra("id", -1)) handleStatusIntent(intent);
    }

    public void handleStatusIntent(Intent intent) {
        if (mTrack == null) return;

        String action = intent.getAction();
        if (Broadcasts.PLAYSTATE_CHANGED.equals(action)) {
            if (intent.getBooleanExtra(BroadcastExtras.isSupposedToBePlaying, false)) {
                hideUnplayable();
                mTrack.last_playback_error = -1;
            } else {
                mWaveformController.setPlaybackStatus(false, intent.getLongExtra(BroadcastExtras.position, 0));
            }

        } else if (Playable.ACTION_PLAYABLE_ASSOCIATION_CHANGED.equals(action)) {
            if (mTrack.getId() == intent.getLongExtra(BroadcastExtras.id, -1)) {
                mTrack.user_like = intent.getBooleanExtra(BroadcastExtras.isLike, false);
                mTrack.user_repost = intent.getBooleanExtra(BroadcastExtras.isRepost, false);
                mActionButtons.update(mTrack);
            }

        } else if (Playable.COMMENTS_UPDATED.equals(action)) {
            if (mTrack.getId() == intent.getLongExtra(BroadcastExtras.id, -1)) {
                onCommentsChanged();
            }

        } else if (Playable.ACTION_SOUND_INFO_UPDATED.equals(action)) {
            Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(intent.getLongExtra(BroadcastExtras.id, -1));
            if (t != null) {
                setTrackInternal(t, mOnScreen);
                if (mTrackDetailsView != null) {
                    mTrackDetailsView.fillTrackDetails(mTrack);
                }
            }

        } else if (Playable.ACTION_SOUND_INFO_ERROR.equals(action)) {
            if (mTrackDetailsView != null) {
                mTrackDetailsView.fillTrackDetails(mTrack);
            }

        } else if (Broadcasts.BUFFERING.equals(action)) {
            setBufferingState(true);
        } else if (Broadcasts.BUFFERING_COMPLETE.equals(action)) {
            setBufferingState(false);
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra(BroadcastExtras.isPlaying, false),
                    intent.getLongExtra(BroadcastExtras.position, 0));

        } else if (Broadcasts.PLAYBACK_ERROR.equals(action)) {
            mTrack.last_playback_error = PlayerActivity.PlayerError.PLAYBACK_ERROR;
            onUnplayable(intent);
        } else if (Broadcasts.STREAM_DIED.equals(action)) {
            mTrack.last_playback_error = PlayerActivity.PlayerError.STREAM_ERROR;
            onUnplayable(intent);
        } else if (Broadcasts.TRACK_UNAVAILABLE.equals(action)) {
            mTrack.last_playback_error = PlayerActivity.PlayerError.TRACK_UNAVAILABLE;
            onUnplayable(intent);
        } else if (Broadcasts.COMMENTS_LOADED.equals(action)) {
            mWaveformController.setComments(mTrack.comments, true);
        } else if (Broadcasts.SEEKING.equals(action)) {
            mWaveformController.onSeek(intent.getLongExtra(BroadcastExtras.position, -1));
        } else if (Broadcasts.SEEK_COMPLETE.equals(action)) {
            mWaveformController.onSeekComplete();
        }
    }

    private void onUnplayable(Intent intent) {
        mWaveformController.setBufferingState(false);
        mWaveformController.setPlaybackStatus(intent.getBooleanExtra(BroadcastExtras.isPlaying, false),
                intent.getLongExtra(BroadcastExtras.position, 0));

        showUnplayable();
    }

    public void onNewComment(Comment comment) {
        if (mTrack != null && comment.track_id == mTrack.getId()) {
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
        return mTrack == null ? -1 : mTrack.getId();
    }

    public void clear() {
        mOnScreen = false;
        onStop(true);
        mAvatar.setImageBitmap(null);
        mWaveformController.reset(true);
        mWaveformController.setOnScreen(false);
    }
}
