package com.soundcloud.android.playback.views;


import static com.soundcloud.android.playback.service.CloudPlaybackService.BroadcastExtras;
import static com.soundcloud.android.playback.service.CloudPlaybackService.Broadcasts;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.collections.views.PlayableBar;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.LoadCommentsTask;
import com.soundcloud.android.playback.PlayerActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import rx.Observable;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("deprecation")
public class PlayerTrackView extends FrameLayout implements
        LoadCommentsTask.LoadCommentsListener, WaveformControllerLayout.WaveformListener {

    @NotNull
    protected Track mTrack;
    protected boolean mOnScreen;
    protected WaveformControllerLayout mWaveformController;

    private FrameLayout mUnplayableLayout;
    private int mQueuePosition;
    private boolean mIsCommenting;
    private long mDuration;

    private PublicCloudAPI oldCloudApi;
    @NotNull
    protected PlayerTrackViewListener mListener;
    @Nullable
    private PlayableBar mTrackInfoBar;
    @Nullable
    private PlayableActionButtonsController mActionButtons;

    public interface PlayerTrackViewListener extends WaveformControllerLayout.WaveformListener {
        void onAddToPlaylist(Track track);
        void onCloseCommentMode();
    }

    public PlayerTrackView(Context context, AttributeSet attrs) {
        super(context, attrs);

        View.inflate(context,R.layout.player_track, this);

        mListener = (PlayerTrackViewListener) context;// NO!!!
        oldCloudApi = new PublicApi(context.getApplicationContext());

        ((ProgressBar) findViewById(R.id.progress_bar)).setMax(1000);
        mWaveformController = (WaveformControllerLayout) findViewById(R.id.waveform_controller);
        mWaveformController.setListener(mListener);

        mTrackInfoBar = (PlayableBar) findViewById(R.id.playable_bar);
        if (mTrackInfoBar != null){
            findViewById(R.id.playable_private_indicator).setVisibility(View.GONE);
            mTrackInfoBar.addTextShadows();
            mTrackInfoBar.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ProfileActivity.startFromPlayable(getContext(), mTrack);
                }
            });
        }

        // cheap way of finding engagements
        if (findViewById(R.id.toggle_like) != null){
            mActionButtons = new PlayableActionButtonsController(this);
            findViewById(R.id.btn_addToSet).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onAddToPlaylist(mTrack);
                }
            });
        }
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

        if (mTrackInfoBar != null) mTrackInfoBar.setTrack(track);
        if (mActionButtons != null) mActionButtons.setTrack(track);
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
                if (mActionButtons != null) mActionButtons.setTrack(mTrack);
            }

        } else if (Playable.COMMENTS_UPDATED.equals(action)) {
            if (mTrack.getId() == intent.getLongExtra(BroadcastExtras.id, -1)) {
                onCommentsChanged();
            }

        } else if (Playable.ACTION_SOUND_INFO_UPDATED.equals(action)) {
            Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(intent.getLongExtra(BroadcastExtras.id, -1));
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

    protected void onTrackInfoChanged() {
    }

    protected void onAssociationsChanged() {
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
        mWaveformController.reset(true);
        mWaveformController.setOnScreen(false);
    }
}
