package com.soundcloud.android.collection.playhistory;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.RepostableItem;
import com.soundcloud.android.presentation.UpdatableTrackItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;

@AutoValue
abstract class PlayHistoryItemTrack extends PlayHistoryItem implements UpdatableTrackItem, LikeableItem, RepostableItem {

    static PlayHistoryItemTrack create(TrackItem trackItem) {
        return new AutoValue_PlayHistoryItemTrack(Kind.PlayHistoryTrack, trackItem);
    }

    public abstract TrackItem trackItem();

    @Override
    public Urn getUrn() {
        return trackItem().getUrn();
    }

    @Override
    public PlayHistoryItemTrack updatedWithTrackItem(Track track) {
        return create(TrackItem.from(track));
    }

    @Override
    public PlayHistoryItemTrack updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        return create(trackItem().updatedWithLike(likeStatus));
    }

    @Override
    public PlayHistoryItemTrack updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        return create(trackItem().updatedWithRepost(repostStatus));
    }
}
