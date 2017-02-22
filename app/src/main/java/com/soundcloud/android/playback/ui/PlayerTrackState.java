package com.soundcloud.android.playback.ui;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Durations;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.tracks.TieredTrack;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

public class PlayerTrackState extends PlayerItem implements TieredTrack, ImageResource {

    static final PlayerTrackState EMPTY = new PlayerTrackState(TrackItem.EMPTY, false, false, ViewVisibilityProvider.EMPTY);

    private final boolean isCurrentTrack;
    private final boolean isForeground;
    private final ViewVisibilityProvider viewVisibilityProvider;

    private Optional<StationRecord> station = Optional.absent();

    PlayerTrackState(TrackItem source,
                     boolean isCurrentTrack,
                     boolean isForeground,
                     ViewVisibilityProvider viewVisibilityProvider) {
        super(source);
        this.isCurrentTrack = isCurrentTrack;
        this.isForeground = isForeground;
        this.viewVisibilityProvider = viewVisibilityProvider;
    }

    public TrackItem getSource() {
        return source;
    }

    public boolean isCurrentTrack() {
        return isCurrentTrack;
    }

    public ViewVisibilityProvider getViewVisibilityProvider() {
        return viewVisibilityProvider;
    }

    public boolean isForeground() {
        return isForeground;
    }

    public void setStation(StationRecord station) {
        this.station = Optional.of(station);
    }

    public Optional<StationRecord> getStation() {
        return station;
    }

    public Urn getUrn() {
        return source.getUrn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.getImageUrlTemplate();
    }

    public String getTitle() {
        return source.title();
    }

    public String getUserName() {
        return source.creatorName();
    }

    public Urn getUserUrn() {
        return source.creatorUrn();
    }

    @Override
    public boolean isBlocked() {
        return source.isBlocked();
    }

    @Override
    public boolean isSnipped() {
        return source.isSnipped();
    }

    @Override
    public boolean isSubMidTier() {
        return source.isSubMidTier();
    }

    @Override
    public boolean isSubHighTier() {
        return source.isSubHighTier();
    }

    long getPlayableDuration() {
        return Durations.getTrackPlayDuration(source);
    }

    long getFullDuration() {
        return source.fullDuration();
    }

    @Nullable
    String getWaveformUrl() {
        return source.waveformUrl();
    }

    boolean isUserLike() {
        return source.isLikedByCurrentUser();
    }

    public boolean isUserRepost() {
        return source.isUserRepost();
    }

    int getLikeCount() {
        return source.likesCount();
    }

    public String getPermalinkUrl() {
        return source.permalinkUrl();
    }

    public boolean isPrivate() {
        return source.isPrivate();
    }

    public boolean isCommentable() {
        return source.commentable();
    }

    public boolean isEmpty() {
        return source.equals(TrackItem.EMPTY);
    }

}
