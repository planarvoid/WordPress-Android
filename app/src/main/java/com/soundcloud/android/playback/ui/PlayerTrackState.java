package com.soundcloud.android.playback.ui;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Durations;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

public class PlayerTrackState extends PlayerItem implements ImageResource {

    static final PlayerTrackState EMPTY = new PlayerTrackState(false, false, ViewVisibilityProvider.EMPTY);

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

    public PlayerTrackState(boolean isCurrentTrack, boolean isForeground, ViewVisibilityProvider viewVisibilityProvider) {
        this.isCurrentTrack = isCurrentTrack;
        this.isForeground = isForeground;
        this.viewVisibilityProvider = viewVisibilityProvider;
    }

    public Optional<TrackItem> getSource() {
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
        return super.getTrackUrn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.isPresent() ? source.get().getImageUrlTemplate() : Optional.absent();
    }

    public String getTitle() {
        return source.transform(TrackItem::title).or(Strings.EMPTY);
    }

    public String getUserName() {
        return source.transform(TrackItem::creatorName).or(Strings.EMPTY);
    }

    public Urn getUserUrn() {
        return source.transform(TrackItem::creatorUrn).or(Urn.NOT_SET);
    }

    public boolean isBlocked() {
        return source.transform(TrackItem::isBlocked).or(false);
    }

    public boolean isSnipped() {
        return source.transform(TrackItem::isSnipped).or(false);
    }

    public boolean isSubMidTier() {
        return source.transform(TrackItem::isSubMidTier).or(false);
    }

    public boolean isSubHighTier() {
        return source.transform(TrackItem::isSubHighTier).or(false);
    }

    long getPlayableDuration() {
        return source.isPresent() ? Durations.getTrackPlayDuration(source.get()) : 0L;
    }

    long getFullDuration() {
        return source.transform(TrackItem::fullDuration).or(0L);
    }

    @Nullable
    String getWaveformUrl() {
        return source.transform(TrackItem::waveformUrl).or(Strings.EMPTY);
    }

    boolean isUserLike() {
        return source.transform(TrackItem::isUserLike).or(false);
    }

    public boolean isUserRepost() {
        return source.transform(TrackItem::isUserRepost).or(false);
    }

    int getLikeCount() {
        return source.transform(TrackItem::likesCount).or(0);
    }

    public String getPermalinkUrl() {
        return source.transform(TrackItem::permalinkUrl).or(Strings.EMPTY);
    }

    public boolean isPrivate() {
        return source.transform(TrackItem::isPrivate).or(false);
    }

    public boolean isCommentable() {
        return source.transform(TrackItem::isCommentable).or(false);
    }

    public boolean isEmpty() {
        return !source.isPresent();
    }

}
