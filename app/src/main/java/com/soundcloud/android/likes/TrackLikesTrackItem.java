package com.soundcloud.android.likes;


import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.adapters.PlayableViewItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

class TrackLikesTrackItem extends TrackLikesItem implements PlayableViewItem, ListItem {

    private final TrackItem trackItem;

    TrackLikesTrackItem(TrackItem trackItem) {
        super(Kind.TrackItem);
        this.trackItem = trackItem;
    }

    public TrackItem getTrackItem() {
        return trackItem;
    }

    @Override
    public Urn getPlayableUrn() {
        return trackItem.getUrn();
    }

    @Override
    public boolean isPlaying() {
        return trackItem.isPlaying();
    }

    @Override
    public void setIsPlaying(boolean isPlaying) {
        trackItem.setIsPlaying(isPlaying);
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
    public ListItem update(PropertySet sourceSet) {
        return trackItem.update(sourceSet);
    }
}
