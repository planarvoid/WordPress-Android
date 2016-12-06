package com.soundcloud.android.playlists;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.PlayableViewItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

class PlaylistDetailTrackItem extends PlaylistDetailItem implements TypedListItem, PlayableViewItem {

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
    public ListItem update(PropertySet sourceSet) {
        return trackItem.update(sourceSet);
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
