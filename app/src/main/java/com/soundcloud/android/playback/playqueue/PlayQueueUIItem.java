package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;

class PlayQueueUIItem {

    private final long id;
    private final Urn urn;
    private final String title;
    private final String creator;
    private final boolean blocked;
    private final int statusLableId;
    private final ImageResource imageResource;
    private final TrackItem trackItem;

    private boolean isInRepeatMode;
    private boolean isPlaying;
    private boolean isDraggable;

    public PlayQueueUIItem(long id,
                           Urn urn,
                           String title,
                           String creator,
                           boolean blocked,
                           int statusLableId,
                           ImageResource imageResource,
                           TrackItem trackItem,
                           boolean isInRepeatMode) {

        this.id = id;
        this.urn = urn;
        this.title = title;
        this.creator = creator;
        this.blocked = blocked;
        this.statusLableId = statusLableId;
        this.imageResource = imageResource;
        this.trackItem = trackItem;
        this.isInRepeatMode = isInRepeatMode;
    }

    public long getId() {
        return id;
    }

    public Urn getUrn() {
        return urn;
    }

    public String getTitle() {
        return title;
    }

    public String getCreator() {
        return creator;
    }

    public int getStatusLableId() {
        return statusLableId;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public ImageResource getImageResource() {
        return imageResource;
    }

    public TrackItem getTrackItem() {
        return trackItem;
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
}
