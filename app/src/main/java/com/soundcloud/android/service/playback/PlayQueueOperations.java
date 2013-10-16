package com.soundcloud.android.service.playback;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;
import com.soundcloud.android.utils.SharedPreferencesUtils;
import rx.util.functions.Action1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.List;

public class PlayQueueOperations {

    private Context mContext;
    private PlayQueueStorage mPlayQueueStorage;
    private SharedPreferences mSharedPreferences;
    private TrackStorage mTrackStorage;

    public PlayQueueOperations(Context mContext, PlayQueueStorage mPlayQueueStorage, SharedPreferences mSharedPreferences, TrackStorage mTrackStorage) {
        this.mContext = mContext;
        this.mPlayQueueStorage = mPlayQueueStorage;
        this.mSharedPreferences = mSharedPreferences;
        this.mTrackStorage = mTrackStorage;
    }

    public void loadTrack(Track track, PlaySourceInfo trackingInfo, PlayQueue playQueue) {
        playQueue.setFromTrack(track, trackingInfo);
        SoundCloudApplication.MODEL_MANAGER.cache(track, ScResource.CacheUpdateMode.NONE);
        broadcastPlayQueueChanged(playQueue);
    }

    public void loadTracksFromIds(List<Long> trackIds, int playPosition, PlaySourceInfo trackingInfo, PlayQueue playQueue) {
        playQueue.setFromTrackIds(trackIds, playPosition, trackingInfo);
        broadcastPlayQueueChanged(playQueue);
    }

    public void savePlayQueueMetadata(PlayQueue playQueue, long seekPos) {
        final long currentTrackId = playQueue.getCurrentTrackId();
        if (currentTrackId != -1) {
            SharedPreferencesUtils.apply(mSharedPreferences.edit()
                    .putString(Consts.PrefKeys.SC_PLAYQUEUE_URI, playQueue.getPlayQueueState(seekPos, currentTrackId).toString()));
        }
    }

    public void savePlayQueue(PlayQueue playQueue, long seekPos) {
        savePlayQueueMetadata(playQueue, seekPos);
        mPlayQueueStorage.storeAsync(playQueue).subscribe(DefaultObserver.NOOP_OBSERVER);
    }

    /**
     * @return last stored seek pos of the current track in queue, or -1 if there is no reload
     */
    public long reloadPlayQueue(final PlayQueue playQueue) {

        final String lastUri = mSharedPreferences.getString(Consts.PrefKeys.SC_PLAYQUEUE_URI, null);
        if (!TextUtils.isEmpty(lastUri)) {
            final PlayQueueUri playQueueUri = new PlayQueueUri(lastUri);
            final long seekPos = playQueueUri.getSeekPos();
            final long trackId = playQueueUri.getTrackId();
            if (trackId > 0) {
                mTrackStorage.getTrackIdsForUriAsync(Content.PLAY_QUEUE.uri).subscribe(new Action1<List<Long>>() {
                    @Override
                    public void call(List<Long> trackIds) {
                        loadTracksFromIds(trackIds, playQueueUri.getPos(), playQueueUri.getPlaySourceInfo(), playQueue);
                    }
                });
            }
            return seekPos;
        } else {
            if (TextUtils.isEmpty(lastUri)) {
                // this is so the player can finish() instead of display waiting to the user
                broadcastPlayQueueChanged(playQueue);
            }
            return -1; // seekpos
        }
    }

    private void broadcastPlayQueueChanged(PlayQueue playQueue) {
        Intent intent = new Intent(CloudPlaybackService.Broadcasts.PLAYQUEUE_CHANGED)
                .putExtra(CloudPlaybackService.BroadcastExtras.queuePosition, playQueue.getPosition());
        mContext.sendBroadcast(intent);
    }


}
