package com.soundcloud.android.playlists;

import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

public final class PlaylistChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {

    private final ScBaseAdapter adapter;

    public PlaylistChangedSubscriber(ScBaseAdapter baseAdapter) {
        adapter = baseAdapter;
    }

    @Override
    public void onNext(EntityStateChangedEvent args) {
        if (adapter != null) {

            long playlistId = args.getFirstUrn().getNumericId();
            int newTracksCount = args.getNextChangeSet().get(PlaylistProperty.TRACK_COUNT);

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
