package com.soundcloud.android.playback.service;

import static com.soundcloud.android.playback.service.PlaybackService.Actions;
import static com.soundcloud.android.playback.service.PlaybackService.Broadcasts;
import static com.soundcloud.android.playback.service.PlaybackService.PlayExtras;

import com.google.common.primitives.Longs;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.associations.AssociationManager;
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

import java.util.List;

class PlaybackReceiver extends BroadcastReceiver {

    private PlaybackService mPlaybackService;
    private AssociationManager mAssociationManager;
    private AudioManager mAudioManager;
    private final AccountOperations mAccountOperations;
    private final PlayQueueManager mPlayQueueManager;

    public PlaybackReceiver(PlaybackService playbackService, AssociationManager associationManager,
                            AudioManager audioManager, PlayQueueManager playQueueManager) {
        this(playbackService, associationManager, audioManager, new AccountOperations(playbackService), playQueueManager);
    }

    public PlaybackReceiver(PlaybackService playbackService, AssociationManager associationManager,
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
        Log.d(PlaybackService.TAG, "BroadcastReceiver#onReceive(" + action + ")");

        if (Actions.RESET_ALL.equals(action)) {
            mPlaybackService.resetAll();
            mPlayQueueManager.clearAll();

        } else if (mAccountOperations.soundCloudAccountExists()) {

            if (Actions.NEXT_ACTION.equals(action)) {
                if (mPlaybackService.next()){
                    mPlaybackService.openCurrent();
                }
            } else if (Actions.PREVIOUS_ACTION.equals(action)) {
                if (mPlaybackService.prev()){
                    mPlaybackService.openCurrent();
                }
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
                if (mPlaybackService.getPlaybackStateInternal() == PlaybackState.WAITING_FOR_PLAYLIST) {
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
                if (mPlaybackService.getPlaybackStateInternal().isSupposedToBePlaying()) {
                    mPlaybackService.saveProgressAndStop();
                } else {
                    // make sure we go to a stopped stat. No-op if there already
                    mPlaybackService.stop();
                }
            }
        } else {
            Log.e(PlaybackService.TAG, "Aborting playback service action, no soundcloud account(" + intent + ")");
        }
    }

    public void setLikeStatus(@NotNull Uri playableUri, boolean like) {
        Playable playable = (Playable) SoundCloudApplication.sModelManager.getModel(playableUri);
        mAssociationManager.setLike(playable, like);
    }

    public void setRepostStatus(@NotNull Uri playableUri, boolean repost) {
        Playable playable = (Playable) SoundCloudApplication.sModelManager.getModel(playableUri);
        mAssociationManager.setRepost(playable, repost);
    }

    private void handlePlayAction(Intent intent) {

        if (intent.hasExtra(PlayExtras.TRACK_ID_LIST)) {

            final List<Long> trackIds = Longs.asList(intent.getLongArrayExtra(PlayExtras.TRACK_ID_LIST));
            final int startPosition = intent.getIntExtra(PlayExtras.START_POSITION, 0);
            final PlaySessionSource playSessionSource = intent.getParcelableExtra(PlayExtras.PLAY_SESSION_SOURCE);
            PlayQueue playQueue = PlayQueue.fromIdList(trackIds, startPosition, playSessionSource);

            mPlayQueueManager.setNewPlayQueue(playQueue, playSessionSource);
            mPlaybackService.openCurrent();

            if (playSessionSource.originatedInExplore() && !playQueue.isEmpty()){
                mPlayQueueManager.fetchRelatedTracks(playQueue.getCurrentTrackId());
            }

        } else {
            Log.w(PlaybackService.TAG, "Received play intent without a play queue");
        }
    }
}
