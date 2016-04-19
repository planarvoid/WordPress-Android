package com.soundcloud.android.playback.ui;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Durations;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.tracks.TieredTrack;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

public class PlayerTrackState extends PlayerItem implements TieredTrack, PropertySetSource, ImageResource {

    static final PlayerTrackState EMPTY = new PlayerTrackState(PropertySet.from(
            TrackProperty.URN.bind(Urn.NOT_SET),
            TrackProperty.TITLE.bind(Strings.EMPTY),
            TrackProperty.CREATOR_NAME.bind(Strings.EMPTY),
            TrackProperty.CREATOR_URN.bind(Urn.NOT_SET),
            TrackProperty.SNIPPET_DURATION.bind(0L),
            TrackProperty.FULL_DURATION.bind(0L),
            TrackProperty.WAVEFORM_URL.bind(Strings.EMPTY),
            TrackProperty.IS_USER_LIKE.bind(false),
            TrackProperty.IS_USER_REPOST.bind(false),
            TrackProperty.LIKES_COUNT.bind(0),
            TrackProperty.PERMALINK_URL.bind(Strings.EMPTY),
            TrackProperty.IS_PRIVATE.bind(false)
    ), false, false, ViewVisibilityProvider.EMPTY);

    private final boolean isCurrentTrack;
    private final boolean isForeground;
    private final ViewVisibilityProvider viewVisibilityProvider;

    private Optional<StationRecord> station = Optional.absent();

    PlayerTrackState(PropertySet source,
                     boolean isCurrentTrack,
                     boolean isForeground,
                     ViewVisibilityProvider viewVisibilityProvider) {
        super(source);
        this.isCurrentTrack = isCurrentTrack;
        this.isForeground = isForeground;
        this.viewVisibilityProvider = viewVisibilityProvider;
    }

    public PropertySet getSource() {
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
        return source.get(TrackProperty.URN);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.get(TrackProperty.IMAGE_URL_TEMPLATE);
    }

    public String getTitle() {
        return source.get(PlayableProperty.TITLE);
    }

    public String getUserName() {
        return source.getOrElse(PlayableProperty.CREATOR_NAME, Strings.EMPTY);
    }

    public Urn getUserUrn() {
        return source.getOrElse(PlayableProperty.CREATOR_URN, Urn.NOT_SET);
    }

    @Override
    public boolean isBlocked() {
        return source.getOrElse(TrackProperty.BLOCKED, false);
    }

    @Override
    public boolean isSnipped() {
        return source.getOrElse(TrackProperty.SNIPPED, false);
    }

    @Override
    public boolean isSubMidTier() {
        return source.getOrElse(TrackProperty.SUB_MID_TIER, false);
    }

    @Override
    public boolean isSubHighTier() {
        return source.getOrElse(TrackProperty.SUB_HIGH_TIER, false);
    }

    long getPlayableDuration() {
        return Durations.getTrackPlayDuration(source);
    }

    long getFullDuration() {
        return source.get(TrackProperty.FULL_DURATION);
    }

    @Nullable
    String getWaveformUrl() {
        return source.getOrElseNull(TrackProperty.WAVEFORM_URL);
    }

    boolean isUserLike() {
        return source.get(PlayableProperty.IS_USER_LIKE);
    }

    public boolean isUserRepost() {
        return source.getOrElse(PlayableProperty.IS_USER_REPOST, false);
    }

    int getLikeCount() {
        return source.get(PlayableProperty.LIKES_COUNT);
    }

    public String getPermalinkUrl() {
        return source.get(PlayableProperty.PERMALINK_URL);
    }

    public boolean isPrivate() {
        return source.get(PlayableProperty.IS_PRIVATE);
    }

    public boolean isCommentable() {
        return source.getOrElse(TrackProperty.IS_COMMENTABLE, false);
    }

    @Override
    public PropertySet toPropertySet() {
        return source;
    }
}
