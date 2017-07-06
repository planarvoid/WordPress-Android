package com.soundcloud.android.playlists;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.api.model.Timestamped;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.OfflineItem;
import com.soundcloud.android.presentation.RepostableItem;
import com.soundcloud.android.presentation.UpdatableTrackItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.PlayableViewItem;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

@AutoValue
public abstract class PlaylistDetailTrackItem extends PlaylistDetailItem implements PlayableViewItem<PlaylistDetailTrackItem>,
        OfflineItem, UpdatableTrackItem, LikeableItem, RepostableItem, ListItem, Timestamped {

    PlaylistDetailTrackItem() {
        super(PlaylistDetailItem.Kind.TrackItem);
    }

    static Builder builder() {
        return new AutoValue_PlaylistDetailTrackItem.Builder()
                .promotedSourceInfo(Optional.absent())
                .inEditMode(false);
    }

    abstract Urn playlistUrn();

    abstract Urn playlistOwnerUrn();

    abstract Optional<PromotedSourceInfo> promotedSourceInfo();

    abstract TrackItem trackItem();

    abstract boolean inEditMode();

    abstract Builder toBuilder();

    private Builder toBuilder(TrackItem from) {
        return toBuilder().trackItem(from);
    }

    @Override
    public Date getCreatedAt() {
        return trackItem().getCreatedAt();
    }

    @Override
    public PlaylistDetailTrackItem updatedWithTrack(Track track) {
        return toBuilder(trackItem().updatedWithTrack(track)).build();
    }

    @Override
    public PlaylistDetailTrackItem updatedWithOfflineState(OfflineState offlineState) {
        return toBuilder(trackItem().updatedWithOfflineState(offlineState)).build();
    }

    @Override
    public PlaylistDetailTrackItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        return toBuilder(trackItem().updatedWithLike(likeStatus)).build();
    }

    @Override
    public PlaylistDetailTrackItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        return toBuilder(trackItem().updatedWithRepost(repostStatus)).build();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return trackItem().getImageUrlTemplate();
    }

    @Override
    public Urn getUrn() {
        return trackItem().getUrn();
    }

    @Override
    public PlaylistDetailTrackItem updateNowPlaying(CurrentPlayQueueItemEvent event) {
        final TrackItem updatedTrackItem = trackItem().updateNowPlaying(event.getCurrentPlayQueueItem().getUrnOrNotSet());
        return toBuilder().trackItem(updatedTrackItem).build();
    }

    @AutoValue.Builder
    abstract static class Builder {

        abstract Builder playlistUrn(Urn playlistUrn);

        abstract Builder playlistOwnerUrn(Urn playlistOwnerUrn);

        abstract Builder promotedSourceInfo(Optional<PromotedSourceInfo> promotedSourceInfo);

        abstract Builder trackItem(TrackItem item);

        abstract Builder inEditMode(boolean inEditMode);

        abstract PlaylistDetailTrackItem build();
    }
}
