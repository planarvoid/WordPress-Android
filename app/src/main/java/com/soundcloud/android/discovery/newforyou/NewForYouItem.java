package com.soundcloud.android.discovery.newforyou;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.tracks.TrackItem;

import java.util.List;

abstract class NewForYouItem {

    enum Kind {
        HEADER,
        TRACK
    }

    abstract Kind kind();
    abstract NewForYou newForYou();

    boolean isHeader() {
        return kind().equals(Kind.HEADER);
    }

    boolean isTrack() {
        return kind().equals(Kind.TRACK);
    }

    @AutoValue
    abstract static class NewForYouTrackItem extends NewForYouItem {
        abstract TrackItem track();

        static NewForYouTrackItem create(NewForYou newForYou, TrackItem track) {
            return new AutoValue_NewForYouItem_NewForYouTrackItem(Kind.TRACK, newForYou, track);
        }
    }

    @AutoValue
    abstract static class NewForYouHeaderItem extends NewForYouItem {
        abstract String duration();
        abstract String updatedAt();
        abstract List<ImageResource> imageResources();

        static NewForYouHeaderItem create(NewForYou newForYou, String duration, String updatedAt, List<ImageResource> imageResources) {
            return new AutoValue_NewForYouItem_NewForYouHeaderItem(Kind.HEADER, newForYou, duration, updatedAt, imageResources);
        }
    }
}