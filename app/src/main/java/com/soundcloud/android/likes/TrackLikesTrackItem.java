package com.soundcloud.android.likes;


import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.OfflineItem;
import com.soundcloud.android.presentation.UpdatableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.PlayableViewItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

class TrackLikesTrackItem extends TrackLikesItem implements PlayableViewItem, ListItem, OfflineItem, UpdatableItem, LikeableItem {

    private final TrackItem trackItem;

    TrackLikesTrackItem(TrackItem trackItem) {
        super(Kind.TrackItem);
        this.trackItem = trackItem;
    }

    public TrackItem getTrackItem() {
        return trackItem;
    }

    @Override
    public Urn getUrn() {
        return trackItem.getUrn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return trackItem.getImageUrlTemplate();
    }

    @Override
    public ListItem updated(PropertySet sourceSet) {
        return new TrackLikesTrackItem(trackItem.updated(sourceSet));
    }

    @Override
    public TrackLikesTrackItem updatedWithOfflineState(OfflineState offlineState) {
        return new TrackLikesTrackItem(trackItem.updatedWithOfflineState(offlineState));
    }

    public TrackLikesTrackItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        return new TrackLikesTrackItem(trackItem.updatedWithLike(likeStatus));
    }

    @Override
    public boolean updateNowPlaying(CurrentPlayQueueItemEvent event) {
        return trackItem.updateNowPlaying(event.getCurrentPlayQueueItem().getUrnOrNotSet());
    }
}
