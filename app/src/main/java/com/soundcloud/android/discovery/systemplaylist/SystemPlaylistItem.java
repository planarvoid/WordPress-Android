package com.soundcloud.android.discovery.systemplaylist;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

abstract class SystemPlaylistItem {

    enum Kind {
        HEADER,
        TRACK
    }

    abstract Kind kind();

    abstract Urn systemPlaylistUrn();

    abstract Optional<Urn> queryUrn();

    abstract Optional<String> trackingFeatureName();

    boolean isHeader() {
        return kind().equals(Kind.HEADER);
    }

    boolean isTrack() {
        return kind().equals(Kind.TRACK);
    }

    @AutoValue
    abstract static class Track extends SystemPlaylistItem {
        abstract TrackItem track();

        abstract boolean isNewForYou();

        static Track create(Urn urn, TrackItem track, Optional<Urn> queryUrn, Optional<String> trackingFeatureName) {
            return new AutoValue_SystemPlaylistItem_Track(Kind.TRACK, urn, queryUrn, trackingFeatureName, track, false);
        }

        static Track createNewForYouTrack(Urn urn, TrackItem track, Optional<Urn> queryUrn, Optional<String> trackingFeatureName) {
            return new AutoValue_SystemPlaylistItem_Track(Kind.TRACK, urn, queryUrn, trackingFeatureName, track, true);
        }

        Track withTrackItem(TrackItem trackItem) {
            return new AutoValue_SystemPlaylistItem_Track(kind(), systemPlaylistUrn(), queryUrn(), trackingFeatureName(), trackItem, isNewForYou());
        }
    }

    @AutoValue
    abstract static class Header extends SystemPlaylistItem {
        abstract Optional<String> title();

        abstract Optional<String> description();

        abstract String metadata();

        abstract Optional<String> updatedAt();

        abstract Optional<ImageResource> image();

        abstract boolean shouldShowPlayButton();

        static Header create(Urn urn,
                             Optional<String> title,
                             Optional<String> description,
                             String metadata,
                             Optional<String> updatedAt,
                             Optional<ImageResource> image,
                             Optional<Urn> queryUrn,
                             Optional<String> trackingFeatureName,
                             boolean shouldShowPlayButton) {
            return new AutoValue_SystemPlaylistItem_Header(Kind.HEADER, urn, queryUrn, trackingFeatureName, title, description, metadata, updatedAt, image, shouldShowPlayButton);
        }
    }
}
