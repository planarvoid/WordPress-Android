package com.soundcloud.android.stream;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.LikeableItem;
import com.soundcloud.android.presentation.RepostableItem;
import com.soundcloud.android.presentation.UpdatableTrackItem;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.PlayableViewItem;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

@AutoValue
public abstract class TrackStreamItem extends StreamItem implements PlayableViewItem<TrackStreamItem>, UpdatableTrackItem, LikeableItem, RepostableItem {
    public abstract TrackItem trackItem();

    public abstract boolean promoted();

    public abstract Date createdAt();

    public abstract Optional<String> avatarUrlTemplate();

    public static TrackStreamItem create(TrackItem trackItem, Date createdAt, Optional<String> avatarUrlTemplate) {
        return new AutoValue_TrackStreamItem(Kind.TRACK, trackItem, trackItem.isPromoted(), createdAt, avatarUrlTemplate);
    }

    private TrackStreamItem create(TrackItem trackItem) {
        return new AutoValue_TrackStreamItem(Kind.TRACK, trackItem, promoted(), createdAt(), avatarUrlTemplate());
    }

    @Override
    public Urn getUrn() {
        return trackItem().getUrn();
    }

    @Override
    public TrackStreamItem updatedWithTrack(Track track) {
        return create(trackItem().updatedWithTrack(track));
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
    public TrackStreamItem updateNowPlaying(CurrentPlayQueueItemEvent event) {
        final TrackItem updatedTrackItem = trackItem().updateNowPlaying(event.getCurrentPlayQueueItem().getUrnOrNotSet());
        return create(updatedTrackItem);
    }
}
