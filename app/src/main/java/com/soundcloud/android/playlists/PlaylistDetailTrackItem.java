package com.soundcloud.android.playlists;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.OfflineItem;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.presentation.UpdatableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.PlayableViewItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

class PlaylistDetailTrackItem extends PlaylistDetailItem implements TypedListItem, PlayableViewItem, OfflineItem, UpdatableItem {

    private final TrackItem trackItem;

    PlaylistDetailTrackItem(TrackItem trackItem) {
        super(PlaylistDetailItem.Kind.TrackItem);
        this.trackItem = trackItem;
    }

    @Override
    public Date getCreatedAt() {
        return trackItem.getCreatedAt();
    }

    @Override
    public ListItem updated(PropertySet sourceSet) {
        return new PlaylistDetailTrackItem(trackItem.updated(sourceSet));
    }

    @Override
    public PlaylistDetailTrackItem updatedWithOfflineState(OfflineState offlineState) {
        return new PlaylistDetailTrackItem(trackItem.updatedWithOfflineState(offlineState));
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return trackItem.getImageUrlTemplate();
    }

    @Override
    public Urn getUrn() {
        return trackItem.getUrn();
    }

    @Override
    public TypedListItem.Kind getKind() {
        return TypedListItem.Kind.PLAYABLE;
    }

    public TrackItem getTrackItem() {
        return trackItem;
    }

    @Override
    public boolean updateNowPlaying(CurrentPlayQueueItemEvent event) {
        return trackItem.updateNowPlaying(event.getCurrentPlayQueueItem().getUrnOrNotSet());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlaylistDetailTrackItem
                && MoreObjects.equal(trackItem, ((PlaylistDetailTrackItem) o).trackItem);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(trackItem);
    }
}
