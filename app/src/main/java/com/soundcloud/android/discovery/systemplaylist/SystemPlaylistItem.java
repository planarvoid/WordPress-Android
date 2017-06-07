package com.soundcloud.android.discovery.systemplaylist;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

abstract class SystemPlaylistItem {

    enum Kind {
        HEADER,
        TRACK
    }

    abstract Kind kind();
    abstract Optional<Urn> queryUrn();

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

        static Track create(TrackItem track, Optional<Urn> queryUrn) {
            return new AutoValue_SystemPlaylistItem_Track(Kind.TRACK, queryUrn, track, false);
        }

        static Track createNewForYouTrack(TrackItem track, Optional<Urn> queryUrn) {
            return new AutoValue_SystemPlaylistItem_Track(Kind.TRACK, queryUrn, track, true);
        }

        Track withTrackItem(TrackItem trackItem) {
            return new AutoValue_SystemPlaylistItem_Track(kind(), queryUrn(), trackItem, isNewForYou());
        }
    }

    @AutoValue
    abstract static class Header extends SystemPlaylistItem {
        abstract Optional<String> title();
        abstract Optional<String> description();
        abstract String metadata();
        abstract Optional<String> updatedAt();
        abstract Optional<ImageResource> image();

        static Header create(Optional<String> title, Optional<String> description, String metadata, Optional<String> updatedAt, Optional<ImageResource> image, Optional<Urn> queryUrn) {
            return new AutoValue_SystemPlaylistItem_Header(Kind.HEADER, queryUrn, title, description, metadata, updatedAt, image);
        }
    }
}