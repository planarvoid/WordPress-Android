package com.soundcloud.android.service.playback;

import static com.soundcloud.android.service.playback.CloudPlaybackService.Actions;
import static com.soundcloud.android.service.playback.CloudPlaybackService.Broadcasts;
import static com.soundcloud.android.service.playback.CloudPlaybackService.PlayExtras;
import static com.soundcloud.android.service.playback.State.EMPTY_PLAYLIST;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.NotNull;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

class PlaybackReceiver extends BroadcastReceiver {

    private CloudPlaybackService mPlaybackService;
    private AssociationManager mAssociationManager;
    private PlayQueueManager mPlayQueueManager;
    private AudioManager mAudioManager;

    public PlaybackReceiver(CloudPlaybackService playbackService, AssociationManager associationManager,
                            PlayQueueManager playQueueManager, AudioManager audioManager) {
        this.mPlaybackService = playbackService;
        this.mAssociationManager = associationManager;
        this.mPlayQueueManager = playQueueManager;
        this.mAudioManager = audioManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (Log.isLoggable(CloudPlaybackService.TAG, Log.DEBUG)) {
            Log.d(CloudPlaybackService.TAG, "BroadcastReceiver#onReceive("+action+")");
        }
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
            appWidgetProvider.performUpdate(context, appWidgetIds,new Intent(Broadcasts.PLAYSTATE_CHANGED));

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
        } else if (Actions.RESET_ALL.equals(action)) {
            mPlaybackService.resetAll();
            mPlayQueueManager.clear();

        } else if (Actions.STOP_ACTION.equals(action)) {
            if (mPlaybackService.getState().isSupposedToBePlaying()) {
                mPlaybackService.saveProgressAndStop();
            }

        } else if (Broadcasts.PLAYQUEUE_CHANGED.equals(action)) {
            if (mPlaybackService.getState() == EMPTY_PLAYLIST) {
                mPlaybackService.openCurrent();
            }
        } else if (Actions.LOAD_TRACK_INFO.equals(action)) {
            final Track t = Track.nullableTrackfromIntent(intent);
            if (t != null){
                t.refreshInfoAsync(mPlaybackService.getOldCloudApi(), mPlaybackService.getInfoListener());
            } else {
                mPlaybackService.getInfoListener().onError(intent.getLongExtra(Track.EXTRA_ID, -1L));
            }

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
        if (Log.isLoggable(CloudPlaybackService.TAG, Log.DEBUG)) {
            Log.d(CloudPlaybackService.TAG, "handlePlayAction(" + intent + ")");
        }

        if (intent.getBooleanExtra(PlayExtras.unmute, false)) {
            configureVolume();
        }

        final boolean startPlayback = intent.getBooleanExtra(PlayExtras.startPlayback, true);
        final int position = intent.getIntExtra(PlayExtras.playPosition, 0);
        if (intent.getData() != null) {
            playViaUri(intent, startPlayback, position);

        } else if (intent.getBooleanExtra(PlayExtras.playFromXferList, false)) {
            playViaTransferList(startPlayback, position);

        } else if (intent.hasExtra(PlayExtras.track) || intent.hasExtra(PlayExtras.trackId)) {
            playSingleTrack(intent, startPlayback);

        } else if (!mPlayQueueManager.isEmpty() || mPlaybackService.configureLastPlaylist()) {
            // random play intent, play whatever we had last
            if (startPlayback) mPlaybackService.play();
        }

    }

    private void configureVolume() {
        final int volume = (int) Math.round(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)* 0.75d);
        if (Log.isLoggable(CloudPlaybackService.TAG, Log.DEBUG)){
            Log.d(CloudPlaybackService.TAG, "setting volume to " + volume);
        }
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    private void playViaUri(Intent intent, boolean startPlayback, int position) {
        mPlayQueueManager.loadUri(intent.getData(), position, mPlaybackService.playlistXfer, position);
        if (startPlayback) mPlaybackService.openCurrent();
    }

    private void playViaTransferList(boolean startPlayback, int position) {
        mPlayQueueManager.setPlayQueue(mPlaybackService.playlistXfer, position);
        mPlaybackService.playlistXfer = null;
        if (startPlayback) mPlaybackService.openCurrent();
    }

    private void playSingleTrack(Intent intent, boolean startPlayback) {

        // go to the cache to ensure 1 copy of each track app wide
        final Track cachedTrack = SoundCloudApplication.MODEL_MANAGER.cache(Track.fromIntent(intent), ScResource.CacheUpdateMode.NONE);
        mPlayQueueManager.loadTrack(cachedTrack, true);

        if (intent.getBooleanExtra(PlayExtras.fetchRelated, false)){
            mPlayQueueManager.fetchRelatedTracks(cachedTrack);
        }

        if (startPlayback) {
            mPlaybackService.openCurrent();
        }

    }
}
