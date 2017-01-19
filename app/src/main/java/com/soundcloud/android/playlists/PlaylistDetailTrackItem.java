package com.soundcloud.android.playlists;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.OfflineItem;
import com.soundcloud.android.presentation.RepostableItem;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.presentation.UpdatableTrackItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.PlayableViewItem;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

class PlaylistDetailTrackItem extends PlaylistDetailItem implements TypedListItem, PlayableViewItem, OfflineItem, UpdatableTrackItem, LikeableItem, RepostableItem {

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
    public PlaylistDetailTrackItem updatedWithTrackItem(Track track) {
        return new PlaylistDetailTrackItem(TrackItem.from(track));
    }

    @Override
    public PlaylistDetailTrackItem updatedWithOfflineState(OfflineState offlineState) {
        return new PlaylistDetailTrackItem(trackItem.updatedWithOfflineState(offlineState));
    }

    @Override
    public PlaylistDetailTrackItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        return new PlaylistDetailTrackItem(trackItem.updatedWithLike(likeStatus));
    }

    @Override
    public PlaylistDetailTrackItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        return new PlaylistDetailTrackItem(trackItem.updatedWithRepost(repostStatus));
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
