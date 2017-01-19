package com.soundcloud.android.likes;


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

class TrackLikesTrackItem extends TrackLikesItem implements PlayableViewItem, ListItem, OfflineItem, UpdatableTrackItem, LikeableItem, RepostableItem {

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
    public TrackLikesTrackItem updatedWithTrackItem(Track track) {
        return new TrackLikesTrackItem(TrackItem.from(track));
    }

    @Override
    public TrackLikesTrackItem updatedWithOfflineState(OfflineState offlineState) {
        return new TrackLikesTrackItem(trackItem.updatedWithOfflineState(offlineState));
    }

    public TrackLikesTrackItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        return new TrackLikesTrackItem(trackItem.updatedWithLike(likeStatus));
    }

    public TrackLikesTrackItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        return new TrackLikesTrackItem(trackItem.updatedWithRepost(repostStatus));
    }

    @Override
    public boolean updateNowPlaying(CurrentPlayQueueItemEvent event) {
        return trackItem.updateNowPlaying(event.getCurrentPlayQueueItem().getUrnOrNotSet());
    }
}
