package com.soundcloud.android.playback.views;


import static com.soundcloud.android.playback.service.PlaybackService.BroadcastExtras;
import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.associations.EngagementsController;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.LoadCommentsTask;
import com.soundcloud.android.playback.PlayerActivity;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

@SuppressWarnings("deprecation")
public class LegacyPlayerTrackView extends FrameLayout implements PlayerTrackView,
        LoadCommentsTask.LoadCommentsListener, WaveformControllerLayout.WaveformListener, EngagementsController.AddToPlaylistListener {

    protected Track track;
    protected boolean onScreen;
    protected WaveformControllerLayout waveformController;

    private FrameLayout mUnplayableLayout;
    private int mQueuePosition;
    private boolean mIsCommenting;
    private long mDuration;

    private PublicApi mPublicApi;
    @NotNull
    protected PlayerTrackViewListener mListener;

    private PlayablePresenter mPlayablePresenter;
    private EngagementsController mEngagementsController;

    private TextView mDebugTextView;

    public interface PlayerTrackViewListener extends WaveformControllerLayout.WaveformListener {
        void onAddToPlaylist(Track track);
        void onCloseCommentMode();
    }
    public LegacyPlayerTrackView(final Context context, AttributeSet attrs) {
        super(context, attrs);

        View.inflate(context,R.layout.player_track, this);

        mListener = (PlayerTrackViewListener) context;// NO!!!
        mPublicApi = new PublicApi(context.getApplicationContext());

        ((ProgressBar) findViewById(R.id.progress_bar)).setMax(1000);
        waveformController = (WaveformControllerLayout) findViewById(R.id.waveform_controller);
        waveformController.setListener(mListener);

        SoundCloudApplication application = (SoundCloudApplication) context.getApplicationContext();
        SoundAssociationOperations soundAssocOps = new SoundAssociationOperations(
                application.getEventBus(), new SoundAssociationStorage(), new SoundCloudRxHttpClient(),
                SoundCloudApplication.sModelManager);

        mPlayablePresenter = new PlayablePresenter(context);
        mEngagementsController = new EngagementsController(
                application.getEventBus(), soundAssocOps, application.getAccountOperations());
        mEngagementsController.bindView(this);
        mEngagementsController.setAddToPlaylistListener(this);

        mDebugTextView = (TextView) findViewById(R.id.debug_txt);

        final View trackInfoBar = findViewById(R.id.playable_bar);
        if (trackInfoBar != null){
            trackInfoBar.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ProfileActivity.startFromPlayable(context, track);
                }
            });

            mPlayablePresenter.setPlayableRowView(this)
                    .setAvatarView((ImageView) trackInfoBar.findViewById(R.id.icon), ImageSize.getListItemImageSize(context))
                    .addTextShadowForGrayBg();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mEngagementsController.startListeningForChanges();
    }

    @Override
    protected void onDetachedFromWindow() {
        mEngagementsController.stopListeningForChanges();
        super.onDetachedFromWindow();
    }

    @Override
    public void onAddToPlaylist(Track track) {
        // just forward to player. This will be nicer after the player refactor
        mListener.onAddToPlaylist(track);
    }

    // TODO, this is currently true all the time
    public void setOnScreen(boolean onScreen){
        this.onScreen = onScreen;
        waveformController.setOnScreen(onScreen);
    }

    public void setTrackState(Track track, int queuePosition, PlaybackStateProvider playbackStateProvider){
        this.track = track;
        mQueuePosition = queuePosition;
        waveformController.updateTrack(this.track, mQueuePosition, true);

        if (mDuration != this.track.duration) {
            mDuration = this.track.duration;
        }

        if ((this.track.isWaitingOnState() || this.track.isStreamable()) && this.track.last_playback_error == -1) {
            hideUnplayable();
        } else {
            showUnplayable();
            waveformController.setBufferingState(false);
        }

        if (this.track.comments != null) {
            waveformController.setComments(this.track.comments, true);
        } else {
            refreshComments();
        }

        mPlayablePresenter.setPlayable(track);
        mEngagementsController.setPlayable(track);

        if (playbackStateProvider.isPlayingTrack(track)) {
            setProgress(playbackStateProvider.getPlayProgress(), playbackStateProvider.getLoadingPercent());
        }
    }

    public void setOriginScreen(OriginProvider originProvider) {
        mEngagementsController.setOriginProvider(originProvider);
    }

    @Override
    public long sendSeek(float seekPosition) {
        return mListener.sendSeek(seekPosition);
    }

    @Override
    public long setSeekMarker(int queuePosition, float seekPosition) {
        return mListener.setSeekMarker(queuePosition, seekPosition);
    }

    private void refreshComments() {
        if (track != null){
            if (AndroidUtils.isTaskFinished(track.load_comments_task)) {
                track.load_comments_task = new LoadCommentsTask(mPublicApi);
            }
            track.load_comments_task.addListener(this);
            if (AndroidUtils.isTaskPending(track.load_comments_task)) {
                track.load_comments_task.execute(track.getId());
            }
        }
    }

    public void onCommentsLoaded(long track_id, List<Comment> comments){
        if (track != null && track.getId() == track_id){
            track.comments = comments;
            waveformController.setComments(track.comments, true);
        }
    }

    public void onBeingScrolled(){
        waveformController.setSuppressComments(true);
    }

    public void onScrollComplete(){
        waveformController.setSuppressComments(false);
    }

    public void onDataConnected() {
        waveformController.onDataConnected();
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
        waveformController.onDestroy();
    }

    public WaveformControllerLayout getWaveformController() {
        return waveformController;
    }

    private void showUnplayable() {
        if (mUnplayableLayout == null) {
            mUnplayableLayout = (FrameLayout) ((ViewStub) findViewById(R.id.stub_unplayable_layout)).inflate();
        }

        if (mUnplayableLayout != null) {
            final TextView unplayableText = (TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt);
            if (unplayableText != null)  { // sometimes inflation error results in text NPE
                if (track == null || track.isStreamable()) {
                    int errorMessage;
                    switch (track == null ? -1 : track.last_playback_error) {
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
        waveformController.setVisibility(View.GONE);

    }

    private void hideUnplayable() {
        waveformController.setVisibility(View.VISIBLE);
        if (mUnplayableLayout != null) mUnplayableLayout.setVisibility(View.GONE);
    }

    public void handleIdBasedIntent(Intent intent) {
        if (track != null && track.getId() == intent.getLongExtra("id", -1)) handleStatusIntent(intent);
    }

    public void handleStatusIntent(Intent intent) {
        if (track == null) return;

        String action = intent.getAction();
        if (Broadcasts.PLAYSTATE_CHANGED.equals(action)) {

            final Playa.StateTransition stateTransition = Playa.StateTransition.fromIntent(intent);

            if (stateTransition.playSessionIsActive()) {
                hideUnplayable();
                track.last_playback_error = -1;
            } else {
                waveformController.setPlaybackStatus(false, intent.getLongExtra(BroadcastExtras.POSITION, 0));

                if (stateTransition.wasError()){
                    // I realize how horrible it is to store error state on the model.
                    // This is not new code and will go away with Player UI refactor
                    track.last_playback_error = PlayerActivity.PlayerError.PLAYBACK_ERROR;
                    onUnplayable(intent);
                }
            }
            setBufferingState(stateTransition.isBuffering());

            final String debugExtra = stateTransition.getDebugExtra();
            if (ScTextUtils.isNotBlank(debugExtra)){
                mDebugTextView.setText(debugExtra);
                mDebugTextView.setVisibility(View.VISIBLE);
            } else {
                mDebugTextView.setVisibility(View.GONE);
            }


        } else if (Playable.COMMENTS_UPDATED.equals(action)) {
            if (track.getId() == intent.getLongExtra(BroadcastExtras.ID, -1)) {
                onCommentsChanged();
            }

        } else if (Broadcasts.COMMENTS_LOADED.equals(action)) {
            waveformController.setComments(track.comments, true);
        }
    }

    private void onUnplayable(Intent intent) {
        waveformController.setBufferingState(false);
        waveformController.setPlaybackStatus(Playa.StateTransition.fromIntent(intent).getNewState().isPlayerPlaying(),
                intent.getLongExtra(BroadcastExtras.POSITION, 0));

        showUnplayable();
    }

    public void onNewComment(Comment comment) {
        if (track != null && comment.track_id == track.getId()) {
            onCommentsChanged();
            waveformController.showNewComment(comment);
        }
    }

    private void onCommentsChanged() {
        if (track != null && track.comments != null) waveformController.setComments(track.comments, false, true);
    }

    public void setProgress(long pos, int loadPercent) {
        waveformController.setProgress(pos, loadPercent);
    }

    public void onStop() {
        waveformController.onStop(true);
    }

    public void setBufferingState(boolean isBuffering) {
        waveformController.setBufferingState(isBuffering);

        if (isBuffering){
            hideUnplayable();

            // TODO: this needs to happen in the service, this should be UI only here
            if (track != null) track.last_playback_error = -1;
        }
    }

    public long getTrackId() {
        return track == null ? -1 : track.getId();
    }

    public void clear() {
        onScreen = false;
        onStop();
        mDebugTextView.setVisibility(View.GONE);
        waveformController.reset(true);
        waveformController.setOnScreen(false);
    }
}
