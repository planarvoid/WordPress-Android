package com.soundcloud.android.service.playback;

import static com.soundcloud.android.service.playback.CloudPlaybackService.Actions;
import static com.soundcloud.android.service.playback.CloudPlaybackService.Broadcasts;
import static com.soundcloud.android.service.playback.CloudPlaybackService.PlayExtras;
import static com.soundcloud.android.service.playback.State.WAITING_FOR_PLAYLIST;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;

class PlaybackReceiver extends BroadcastReceiver {

    private CloudPlaybackService mPlaybackService;
    private AssociationManager mAssociationManager;
    private AudioManager mAudioManager;
    private final AccountOperations mAccountOperations;
    private final PlayQueueManager mPlayQueueManager;

    public PlaybackReceiver(CloudPlaybackService playbackService, AssociationManager associationManager,
                            AudioManager audioManager, PlayQueueManager playQueueManager) {
        this(playbackService, associationManager, audioManager, new AccountOperations(playbackService), playQueueManager);
    }

    public PlaybackReceiver(CloudPlaybackService playbackService, AssociationManager associationManager,
                            AudioManager audioManager, AccountOperations accountOperations, PlayQueueManager playQueueManager) {
        this.mPlaybackService = playbackService;
        this.mAssociationManager = associationManager;
        this.mAudioManager = audioManager;
        this.mAccountOperations = accountOperations;
        mPlayQueueManager = playQueueManager;
    }


    @Override

    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Log.d(CloudPlaybackService.TAG, "BroadcastReceiver#onReceive(" + action + ")");

        if (Actions.RESET_ALL.equals(action)) {
            mPlaybackService.resetAll();
            mPlayQueueManager.clearAll();

        } else if (mAccountOperations.soundCloudAccountExists()) {

            if (Actions.NEXT_ACTION.equals(action)) {
                mPlaybackService.next();
            } else if (Actions.PREVIOUS_ACTION.equals(action)) {
                mPlaybackService.prev();
            } else if (Actions.TOGGLEPLAYBACK_ACTION.equals(action)) {
                mPlaybackService.togglePlayback();
            } else if (Actions.PAUSE_ACTION.equals(action)) {
                mPlaybackService.pause();
            } else if (Broadcasts.UPDATE_WIDGET_ACTION.equals(action)) {
                // Someone asked us to executeRefreshTask a set of specific widgets,
                // probably because they were just added.
                final int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                final PlayerAppWidgetProvider appWidgetProvider = mPlaybackService.getAppWidgetProvider();
                appWidgetProvider.performUpdate(context, appWidgetIds, new Intent(Broadcasts.PLAYSTATE_CHANGED));

            } else if (Actions.ADD_LIKE_ACTION.equals(action)) {
                setLikeStatus(intent.getData(), true);
            } else if (Actions.REMOVE_LIKE_ACTION.equals(action)) {
                setLikeStatus(intent.getData(), false);
            } else if (Actions.ADD_REPOST_ACTION.equals(action)) {
                setRepostStatus(intent.getData(), true);
            } else if (Actions.REMOVE_REPOST_ACTION.equals(action)) {
                setRepostStatus(intent.getData(), false);
            } else if (Actions.PLAY_ACTION.equals(action)) {
                handlePlayAction(intent);
            } else if (Actions.RETRY_RELATED_TRACKS.equals(action)) {
                mPlayQueueManager.retryRelatedTracksFetch();
            } else if (Broadcasts.PLAYQUEUE_CHANGED.equals(action)) {
                if (mPlaybackService.getState() == WAITING_FOR_PLAYLIST) {
                    mPlaybackService.openCurrent();
                }
            } else if (Actions.LOAD_TRACK_INFO.equals(action)) {
                final Track t = Track.nullableTrackfromIntent(intent);
                if (t != null) {
                    t.refreshInfoAsync(mPlaybackService.getOldCloudApi(), mPlaybackService.getInfoListener());
                } else {
                    mPlaybackService.getInfoListener().onError(intent.getLongExtra(Track.EXTRA_ID, -1L));
                }
            } else if (Actions.STOP_ACTION.equals(action)) {
                if (mPlaybackService.getState().isSupposedToBePlaying()) {
                    mPlaybackService.saveProgressAndStop();
                } else {
                    // make sure we go to a stopped stat. No-op if there already
                    mPlaybackService.stop();
                }
            }
        } else {
            Log.e(CloudPlaybackService.TAG, "Aborting playback service action, no soundcloud account(" + intent + ")");
        }
    }

    public void setLikeStatus(@NotNull Uri playableUri, boolean like) {
        Playable playable = (Playable) SoundCloudApplication.MODEL_MANAGER.getModel(playableUri);
        mAssociationManager.setLike(playable, like);
    }

    public void setRepostStatus(@NotNull Uri playableUri, boolean repost) {
        Playable playable = (Playable) SoundCloudApplication.MODEL_MANAGER.getModel(playableUri);
        mAssociationManager.setRepost(playable, repost);
    }

    private void handlePlayAction(Intent intent) {
        if (intent.hasExtra(PlayQueue.EXTRA)) {
            PlayQueue playQueue = intent.getParcelableExtra(PlayQueue.EXTRA);

            mPlayQueueManager.setNewPlayQueue(playQueue);
            mPlaybackService.openCurrent();

            if (intent.getBooleanExtra(PlayExtras.fetchRelated, false)){
                mPlayQueueManager.fetchRelatedTracks(playQueue.getCurrentTrackId());
            }
        } else {
            Log.w(CloudPlaybackService.TAG, "Received play intent without a play queue");
        }
    }
}
