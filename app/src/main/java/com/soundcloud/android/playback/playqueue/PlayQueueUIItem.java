package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.tracks.TieredTracks;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

class PlayQueueUIItem {

    private final PlayQueueItem playQueueItem;
    private final TrackItem trackItem;
    private final int uniqueId;
    private final int statusLabelId;
    private final ImageResource imageResource;
    private boolean isInRepeatMode;
    private boolean isPlaying;
    private boolean isDraggable;

    public PlayQueueUIItem(PlayQueueItem playQueueItem,
                           TrackItem trackItem,
                           int uniqueId,
                           int statusLabelId,
                           ImageResource imageResource) {
        this.playQueueItem = playQueueItem;
        this.trackItem = trackItem;
        this.uniqueId = uniqueId;
        this.statusLabelId = statusLabelId;
        this.imageResource = imageResource;
        this.isInRepeatMode = false;
        this.isPlaying = false;
        this.isDraggable = false;
    }

    static PlayQueueUIItem from(PlayQueueItem playQueueItem, TrackItem trackItem) {
        return new PlayQueueUIItem(
                playQueueItem,
                trackItem,
                System.identityHashCode(playQueueItem),
                createStatusLabelId(trackItem),
                getImageResource(trackItem)
        );
    }

    public TrackItem getTrackItem() {
        return trackItem;
    }

    public PlayQueueItem getPlayQueueItem() {
        return playQueueItem;
    }

    public long getUniqueId() {
        return uniqueId;
    }

    public Urn getUrn() {
        return playQueueItem.getUrn();
    }

    public String getTitle() {
        return trackItem.getTitle();
    }

    public String getCreator() {
        return trackItem.getCreatorName();
    }

    public int getStatusLabelId() {
        return statusLabelId;
    }

    public boolean isBlocked() {
        return trackItem.isBlocked();
    }

    public ImageResource getImageResource() {
        return imageResource;
    }

    public boolean isInRepeatMode() {
        return isInRepeatMode;
    }

    public void setInRepeatMode(boolean isInRepeatMode) {
        this.isInRepeatMode = isInRepeatMode;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public boolean isDraggable() {
        return isDraggable;
    }

    public void setDraggable(boolean draggable) {
        isDraggable = draggable;
    }

    private static ImageResource getImageResource(TrackItem trackItem) {
        Urn urn = trackItem.getUrn();
        PropertySet propertySet = trackItem.getSource();
        Optional<String> templateUrl = propertySet.getOrElse(EntityProperty.IMAGE_URL_TEMPLATE,
                                                             Optional.<String>absent());
        return SimpleImageResource.create(urn, templateUrl);
    }

    private static int createStatusLabelId(TrackItem trackItem) {
        if (trackItem.isBlocked()) {
            return R.layout.not_available;
        } else if (TieredTracks.isHighTierPreview(trackItem)) {
            return R.layout.preview;
        } else if (TieredTracks.isFullHighTierTrack(trackItem)) {
            return R.layout.go_label;
        } else if (trackItem.isPrivate()) {
            return R.layout.private_label;
        } else {
            return Consts.NOT_SET;
        }
    }
}
