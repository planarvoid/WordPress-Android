package com.soundcloud.android.playlists;

import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PlaylistChangedReceiver extends BroadcastReceiver {

    private final ScBaseAdapter adapter;

    public PlaylistChangedReceiver(ScBaseAdapter baseAdapter) {
        adapter = baseAdapter;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (adapter != null) {

            long playlistId = intent.getLongExtra(PublicApiPlaylist.EXTRA_ID, -1);
            int newTracksCount = intent.getIntExtra(PublicApiPlaylist.EXTRA_TRACKS_COUNT, -1);

            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i) instanceof PlayableHolder) {
                    updatePlayableIfNecessary(((PlayableHolder) adapter.getItem(i)).getPlayable(),
                            playlistId, newTracksCount);
                }

            }
        }
    }

    private void updatePlayableIfNecessary(Playable playable, long playlistId, int newTracksCount) {
        if (playable instanceof PublicApiPlaylist && playable.getId() == playlistId) {
            ((PublicApiPlaylist) playable).setTrackCount(newTracksCount);
            adapter.notifyDataSetChanged();
        }
    }
}
