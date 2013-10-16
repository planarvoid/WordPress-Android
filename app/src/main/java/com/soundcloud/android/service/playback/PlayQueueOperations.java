package com.soundcloud.android.service.playback;

import com.soundcloud.android.Consts;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.Log;
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

    public PlayQueueOperations(Context mContext, PlayQueueStorage mPlayQueueStorage, SharedPreferences mSharedPreferences) {
        this.mContext = mContext;
        this.mPlayQueueStorage = mPlayQueueStorage;
        this.mSharedPreferences = mSharedPreferences;
    }

    public void loadFromNewQueue(PlayQueueState playQueueState, PlayQueue playQueue) {
        playQueue.setFromNewQueueState(playQueueState);
        broadcastPlayQueueChanged(playQueue);
    }

    public void savePlayQueueMetadata(PlayQueue playQueue, long seekPos) {
        final long currentTrackId = playQueue.getCurrentTrackId();
        if (currentTrackId != -1) {
            final String playQueueState = playQueue.getPlayQueueState(seekPos, currentTrackId).toString();
            Log.d(CloudPlaybackService.TAG, "Saving playqueue state: " + playQueueState);
            SharedPreferencesUtils.apply(mSharedPreferences.edit().putString(Consts.PrefKeys.SC_PLAYQUEUE_URI, playQueueState));
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
                mPlayQueueStorage.getTrackIds().subscribe(new Action1<List<Long>>() {
                    @Override
                    public void call(List<Long> trackIds) {
                        loadFromNewQueue(new PlayQueueState(trackIds, playQueueUri.getPos(), playQueueUri.getPlaySourceInfo()), playQueue);
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
