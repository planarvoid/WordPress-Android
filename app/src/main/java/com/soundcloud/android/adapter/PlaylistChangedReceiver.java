package com.soundcloud.android.adapter;

import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.behavior.PlayableHolder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PlaylistChangedReceiver extends BroadcastReceiver {

    private final ScBaseAdapter mAdapter;

    public PlaylistChangedReceiver(ScBaseAdapter baseAdapter) {
        mAdapter = baseAdapter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mAdapter != null) {

            long playlistId = intent.getLongExtra(Playlist.EXTRA_ID, -1);
            int newTracksCount = intent.getIntExtra(Playlist.EXTRA_TRACKS_COUNT, -1);

            for (int i = 0; i < mAdapter.getCount(); i++) {
                if (mAdapter.getItem(i) instanceof PlayableHolder) {
                    updatePlayableIfNecessary(((PlayableHolder) mAdapter.getItem(i)).getPlayable(),
                            playlistId, newTracksCount);
                }

            }
        }
    }

    private void updatePlayableIfNecessary(Playable playable, long playlistId, int newTracksCount) {
        if (playable instanceof Playlist && playable.getId() == playlistId) {
            ((Playlist) playable).setTrackCount(newTracksCount);
            mAdapter.notifyDataSetChanged();
        }
    }
}
