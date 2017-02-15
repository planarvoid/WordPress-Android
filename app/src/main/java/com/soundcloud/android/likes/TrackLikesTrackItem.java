package com.soundcloud.android.likes;


import com.google.auto.value.AutoValue;
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

@AutoValue
abstract class TrackLikesTrackItem extends TrackLikesItem implements PlayableViewItem, ListItem, OfflineItem, UpdatableTrackItem, LikeableItem, RepostableItem {

    public static TrackLikesTrackItem create(Track track) {
        return create(TrackItem.from(track));
    }

    public static TrackLikesTrackItem create(TrackItem trackItem) {
        return new AutoValue_TrackLikesTrackItem(Kind.TrackItem, trackItem);
    }

    public abstract TrackItem getTrackItem();

    @Override
    public Urn getUrn() {
        return getTrackItem().getUrn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return getTrackItem().getImageUrlTemplate();
    }

    @Override
    public TrackLikesTrackItem updatedWithTrackItem(Track track) {
        return create(TrackItem.from(track));
    }

    @Override
    public TrackLikesTrackItem updatedWithOfflineState(OfflineState offlineState) {
        return create(getTrackItem().updatedWithOfflineState(offlineState));
    }

    public TrackLikesTrackItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        return create(getTrackItem().updatedWithLike(likeStatus));
    }

    public TrackLikesTrackItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        return create(getTrackItem().updatedWithRepost(repostStatus));
    }

    @Override
    public boolean updateNowPlaying(CurrentPlayQueueItemEvent event) {
        return getTrackItem().updateNowPlaying(event.getCurrentPlayQueueItem().getUrnOrNotSet());
    }
}
