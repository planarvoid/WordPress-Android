package com.soundcloud.android.playback.views;


import static com.soundcloud.android.playback.service.PlaybackService.BroadcastExtras;
import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.collections.views.PlayableBar;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.LoadCommentsTask;
import com.soundcloud.android.playback.PlayerActivity;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.StatsView;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.List;

@SuppressWarnings("deprecation")
public class PlayerTrackView extends FrameLayout implements
        LoadCommentsTask.LoadCommentsListener, WaveformControllerLayout.WaveformListener, PlayableController.AddToPlaylistListener {

    protected Track mTrack;
    protected boolean mOnScreen;
    protected WaveformControllerLayout mWaveformController;

    private FrameLayout mUnplayableLayout;
    private int mQueuePosition;
    private boolean mIsCommenting;
    private long mDuration;

    private PublicApi mPublicApi;
    @NotNull
    protected PlayerTrackViewListener mListener;
    private PlaybackStateProvider mPlaybackStateProvider;
    private PlayableController mPlayableController;
    private Subscription mTrackSubscription = Subscriptions.empty();

    public interface PlayerTrackViewListener extends WaveformControllerLayout.WaveformListener {
        void onAddToPlaylist(Track track);
        void onCloseCommentMode();
    }

    public PlayerTrackView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        View.inflate(context,R.layout.player_track, this);

        mListener = (PlayerTrackViewListener) context;// NO!!!
        mPublicApi = new PublicApi(context.getApplicationContext());
        mPlaybackStateProvider = new PlaybackStateProvider();

        ((ProgressBar) findViewById(R.id.progress_bar)).setMax(1000);
        mWaveformController = (WaveformControllerLayout) findViewById(R.id.waveform_controller);
        mWaveformController.setListener(mListener);

        SoundAssociationOperations soundAssocOps = new SoundAssociationOperations(
                new SoundAssociationStorage(), new SoundCloudRxHttpClient(),
                SoundCloudApplication.sModelManager);

        mPlayableController = new PlayableController(context, soundAssocOps, null);

        final PlayableBar trackInfoBar = (PlayableBar) findViewById(R.id.playable_bar);
        if (trackInfoBar != null){
            trackInfoBar.addTextShadows();
            trackInfoBar.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ProfileActivity.startFromPlayable(context, getTrack());
                }
            });

            mPlayableController.setTitleView((TextView) findViewById(R.id.playable_title))
                    .setUsernameView((TextView) findViewById(R.id.playable_user))
                    .setAvatarView((ImageView) trackInfoBar.findViewById(R.id.icon), ImageSize.getListItemImageSize(context), R.drawable.avatar_badge)
                    .setStatsView((StatsView) findViewById(R.id.stats), false)
                    .setCreatedAtView((TextView) findViewById(R.id.playable_created_at))
                    .setPrivacyIndicatorView((TextView) findViewById(R.id.playable_private_indicator))
                    .setLikeButton((ToggleButton) findViewById(R.id.toggle_like))
                    .setRepostButton((ToggleButton) findViewById(R.id.toggle_repost))
                    .setAddToPlaylistButton(findViewById(R.id.btn_addToPlaylist), this)
                    .setShareButton((ImageButton) findViewById(R.id.btn_share));
            ;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mPlayableController.startListeningForChanges();
    }

    @Override
    protected void onDetachedFromWindow() {
        mPlayableController.stopListeningForChanges();
        super.onAttachedToWindow();
    }

    @Override
    public void onAddToPlaylist(Track track) {
        // just forward to player. This will be nicer after the player refactor
        mListener.onAddToPlaylist(track);
    }

    // TODO, this is currently true all the time
    public void setOnScreen(boolean onScreen){
        mOnScreen = onScreen;
        mWaveformController.setOnScreen(onScreen);
    }

    public void setPlayQueueItem(Observable<Track> trackObservable, int queuePosition){
        mQueuePosition = queuePosition;
        mTrackSubscription.unsubscribe(); // unsubscribe from old subscription which may be in flight
        mTrackSubscription = trackObservable.subscribe(new DefaultObserver<Track>() {
            @Override
            public void onNext(Track args) {
                // GET RID OF PRIORITY OR IMPLEMENT IT PROPERLY
                setTrackInternal(args, true);
            }
        });
    }

    public void setOriginScreen(OriginProvider originProvider) {
        mPlayableController.setOriginProvider(originProvider);
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
        mTrack = track;
        mWaveformController.updateTrack(mTrack, mQueuePosition, priority);

        if (mDuration != mTrack.duration) {
            mDuration = mTrack.duration;
        }

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

        mPlayableController.setPlayable(track);
        if (mQueuePosition == mPlaybackStateProvider.getPlayPosition()) {
            setProgress(mPlaybackStateProvider.getPlayProgress(), mPlaybackStateProvider.getLoadingPercent(),
                    Consts.SdkSwitches.useSmoothProgress && mPlaybackStateProvider.isPlaying());
        }
    }

    private void refreshComments() {
        if (mTrack != null){
            if (AndroidUtils.isTaskFinished(mTrack.load_comments_task)) {
                mTrack.load_comments_task = new LoadCommentsTask(mPublicApi);
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
    }

    public int getPlayPosition() {
        return mQueuePosition;
    }

    public void onDestroy() {
        clear();
        mWaveformController.onDestroy();
    }

    public WaveformControllerLayout getWaveformController() {
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
            if (intent.getBooleanExtra(BroadcastExtras.IS_SUPPOSED_TO_BE_PLAYING, false)) {
                hideUnplayable();
                mTrack.last_playback_error = -1;
            } else {
                mWaveformController.setPlaybackStatus(false, intent.getLongExtra(BroadcastExtras.POSITION, 0));
            }
        } else if (Playable.COMMENTS_UPDATED.equals(action)) {
            if (mTrack.getId() == intent.getLongExtra(BroadcastExtras.ID, -1)) {
                onCommentsChanged();
            }

        } else if (Playable.ACTION_SOUND_INFO_UPDATED.equals(action)) {
            Track t = SoundCloudApplication.sModelManager.getTrack(intent.getLongExtra(BroadcastExtras.ID, -1));
            if (t != null) {
                setTrackInternal(t, mOnScreen);
                onTrackInfoChanged();
            }

        } else if (Playable.ACTION_SOUND_INFO_ERROR.equals(action)) {
            onTrackInfoChanged();

        } else if (Broadcasts.BUFFERING.equals(action)) {
            setBufferingState(true);
        } else if (Broadcasts.BUFFERING_COMPLETE.equals(action)) {
            setBufferingState(false);
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra(BroadcastExtras.IS_PLAYING, false),
                    intent.getLongExtra(BroadcastExtras.POSITION, 0));

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
            mWaveformController.onSeek(intent.getLongExtra(BroadcastExtras.POSITION, -1));
        } else if (Broadcasts.SEEK_COMPLETE.equals(action)) {
            mWaveformController.onSeekComplete();
        }
    }

    protected void onTrackInfoChanged() {
    }

    private void onUnplayable(Intent intent) {
        mWaveformController.setBufferingState(false);
        mWaveformController.setPlaybackStatus(intent.getBooleanExtra(BroadcastExtras.IS_PLAYING, false),
                intent.getLongExtra(BroadcastExtras.POSITION, 0));

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

    public Track getTrack() {
        return mTrack;
    }

    public void clear() {
        mOnScreen = false;
        onStop(true);
        mWaveformController.reset(true);
        mWaveformController.setOnScreen(false);
    }
}
