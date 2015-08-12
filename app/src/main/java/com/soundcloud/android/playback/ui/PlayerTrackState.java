package com.soundcloud.android.playback.ui;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.Nullable;

class PlayerTrackState extends PlayerItem implements PropertySetSource {

    static final PlayerTrackState EMPTY = new PlayerTrackState(PropertySet.from(
            TrackProperty.URN.bind(Urn.NOT_SET),
            TrackProperty.TITLE.bind(ScTextUtils.EMPTY_STRING),
            TrackProperty.CREATOR_NAME.bind(ScTextUtils.EMPTY_STRING),
            TrackProperty.CREATOR_URN.bind(Urn.NOT_SET),
            TrackProperty.DURATION.bind(0L),
            TrackProperty.WAVEFORM_URL.bind(ScTextUtils.EMPTY_STRING),
            TrackProperty.IS_LIKED.bind(false),
            TrackProperty.IS_REPOSTED.bind(false),
            TrackProperty.LIKES_COUNT.bind(0),
            TrackProperty.PERMALINK_URL.bind(ScTextUtils.EMPTY_STRING),
            TrackProperty.IS_PRIVATE.bind(false)
    ), false, false, null);

    private final boolean isCurrentTrack;
    private final boolean isForeground;
    private final ViewVisibilityProvider viewVisibilityProvider;

    private PropertySet relatedTrack;

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

    public void setRelatedTrack(PropertySet relatedTrack) {
        this.relatedTrack = relatedTrack;
    }

    public boolean hasRelatedTrack() {
        return relatedTrack != null;
    }

    public String getRelatedTrackTitle() {
        return relatedTrack.get(TrackProperty.TITLE);
    }

    public Urn getRelatedTrackUrn() {
        return relatedTrack.get(TrackProperty.URN);
    }

    Urn getUrn() {
        return source.get(TrackProperty.URN);
    }

    String getTitle() {
        return source.get(PlayableProperty.TITLE);
    }

    String getUserName() {
        return source.get(PlayableProperty.CREATOR_NAME);
    }

    Urn getUserUrn() {
        return source.get(PlayableProperty.CREATOR_URN);
    }

    long getDuration() {
        return source.get(PlayableProperty.DURATION);
    }

    @Nullable
    String getWaveformUrl() {
        return source.getOrElseNull(TrackProperty.WAVEFORM_URL);
    }

    boolean isUserLike() {
        return source.get(PlayableProperty.IS_LIKED);
    }

    public boolean isUserRepost() {
        return source.get(PlayableProperty.IS_REPOSTED);
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

    @Override
    public PropertySet toPropertySet() {
        return source;
    }
}
