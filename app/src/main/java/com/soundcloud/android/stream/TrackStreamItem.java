package com.soundcloud.android.stream;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.RepostableItem;
import com.soundcloud.android.presentation.UpdatableTrackItem;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.PlayableViewItem;

import java.util.Date;

@AutoValue
abstract class TrackStreamItem extends StreamItem implements PlayableViewItem, UpdatableTrackItem, LikeableItem, RepostableItem {
    public abstract TrackItem trackItem();

    public abstract boolean promoted();

    public abstract Date createdAt();

    static TrackStreamItem create(TrackItem trackItem, Date createdAt) {
        return new AutoValue_TrackStreamItem(Kind.TRACK, trackItem, false, createdAt);
    }

    private TrackStreamItem create(TrackItem trackItem) {
        return new AutoValue_TrackStreamItem(Kind.TRACK, trackItem, promoted(), createdAt());
    }

    static TrackStreamItem createForPromoted(PromotedTrackItem trackItem, Date createdAt) {
        return new AutoValue_TrackStreamItem(Kind.TRACK, trackItem, true, createdAt);
    }

    @Override
    public Urn getUrn() {
        return trackItem().getUrn();
    }

    @Override
    public TrackStreamItem updatedWithTrackItem(Track track) {
        return create(TrackItem.from(track));
    }

    @Override
    public TrackStreamItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        return create(trackItem().updatedWithLike(likeStatus));
    }

    @Override
    public TrackStreamItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        return create(trackItem().updatedWithRepost(repostStatus));
    }

    @Override
    public boolean updateNowPlaying(CurrentPlayQueueItemEvent event) {
        return trackItem().updateNowPlaying(event.getCurrentPlayQueueItem().getUrnOrNotSet());
    }
}
