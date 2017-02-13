package com.soundcloud.android.stream;


import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.suggestedcreators.SuggestedCreator;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.List;

public abstract class StreamItem {

    public enum Kind {
        TRACK,
        PLAYLIST,
        FACEBOOK_LISTENER_INVITES,
        STATIONS_ONBOARDING,
        FACEBOOK_CREATORS,
        STREAM_UPSELL,
        SUGGESTED_CREATORS,
        APP_INSTALL,
        VIDEO_AD,
        STREAM_HIGHLIGHTS
    }

    static StreamItem forUpsell() {
        return new AutoValue_StreamItem_Default(Kind.STREAM_UPSELL);
    }

    public static StreamItem forSuggestedCreators(final List<SuggestedCreator> suggestedCreators) {
        return SuggestedCreators.create(suggestedCreators);
    }

    public static StreamItem forFacebookCreatorInvites(Urn trackUrn, String trackUrl) {
        return FacebookCreatorInvites.create(trackUrn, trackUrl);
    }

    public static StreamItem forFacebookListenerInvites() {
        return FacebookListenerInvites.create(Optional.absent());
    }

    public static FacebookListenerInvites forFacebookListenerInvites(final List<String> friendPictureUrls) {
        return FacebookListenerInvites.create(Optional.of(friendPictureUrls));
    }

    public static StreamItem forStationOnboarding() {
        return new AutoValue_StreamItem_Default(Kind.STATIONS_ONBOARDING);
    }

    public static StreamItem forAppInstall(AppInstallAd ad) {
        return AppInstall.create(ad);
    }

    public static StreamItem forVideoAd(VideoAd ad) {
        return Video.create(ad);
    }

    Optional<ListItem> getListItem() {
        if (kind() == Kind.TRACK) {
            return Optional.of(((TrackStreamItem) this).trackItem());
        } else if (kind() == Kind.PLAYLIST) {
            return Optional.of(((PlaylistStreamItem) this).playlistItem());
        }
        return Optional.absent();
    }

    public Optional<AdData> getAdData() {
        if (kind() == Kind.APP_INSTALL) {
            return Optional.of(((StreamItem.AppInstall) this).appInstall());
        } else if (kind() == Kind.VIDEO_AD) {
            return Optional.of(((StreamItem.Video) this).video());
        } else {
            return Optional.absent();
        }
    }

    public abstract Kind kind();

    public boolean isPromoted() {
        return (this.kind() == Kind.TRACK && ((TrackStreamItem) this).promoted())
                || (this.kind() == Kind.PLAYLIST && ((PlaylistStreamItem) this).promoted());
    }

    public boolean isAd() {
        return kind() == Kind.APP_INSTALL || kind() == Kind.VIDEO_AD;
    }

    public boolean isUpsell() {
        return kind() == Kind.STREAM_UPSELL;
    }

    @AutoValue
    abstract static class Default extends StreamItem {
    }

    @AutoValue
    public abstract static class FacebookCreatorInvites extends StreamItem {
        public abstract Urn trackUrn();

        abstract String trackUrl();

        private static FacebookCreatorInvites create(Urn trackUrn, String trackUrl) {
            return new AutoValue_StreamItem_FacebookCreatorInvites(Kind.FACEBOOK_CREATORS, trackUrn, trackUrl);
        }
    }

    @AutoValue
    public abstract static class FacebookListenerInvites extends StreamItem {
        public abstract Optional<List<String>> friendPictureUrls();

        public boolean hasPictures() {
            return friendPictureUrls().isPresent() && !friendPictureUrls().get().isEmpty();
        }

        private static FacebookListenerInvites create(Optional<List<String>> friendPictureUrls) {
            return new AutoValue_StreamItem_FacebookListenerInvites(Kind.FACEBOOK_LISTENER_INVITES, friendPictureUrls);
        }
    }

    @AutoValue
    public abstract static class SuggestedCreators extends StreamItem {
        public abstract List<SuggestedCreatorItem> suggestedCreators();

        public static SuggestedCreators create(List<SuggestedCreator> suggestedCreators) {
            final List<SuggestedCreatorItem> suggestedCreatorItems = new ArrayList<>(suggestedCreators.size());
            for (SuggestedCreator suggestedCreator : suggestedCreators) {
                suggestedCreatorItems.add(SuggestedCreatorItem.fromSuggestedCreator(suggestedCreator));
            }
            return new AutoValue_StreamItem_SuggestedCreators(Kind.SUGGESTED_CREATORS, suggestedCreatorItems);
        }
    }

    @AutoValue
    public abstract static class StreamHighlights extends StreamItem {
        public abstract List<TrackItem> suggestedTrackItems();

        public static StreamHighlights create(List<ApiTrack> suggestedTracks) {
            final List<TrackItem> suggestedTrackItems = new ArrayList<>(suggestedTracks.size());
            for (ApiTrack apiTrack : suggestedTracks) {
                suggestedTrackItems.add(TrackItem.from(apiTrack));
            }
            return new AutoValue_StreamItem_StreamHighlights(Kind.STREAM_HIGHLIGHTS, suggestedTrackItems);
        }
    }

    @AutoValue
    public abstract static class AppInstall extends StreamItem {
        public abstract AppInstallAd appInstall();

        static AppInstall create(AppInstallAd ad) {
            return new AutoValue_StreamItem_AppInstall(Kind.APP_INSTALL, ad);
        }
    }

    @AutoValue
    public abstract static class Video extends StreamItem {
        public abstract VideoAd video();

        static Video create(VideoAd ad) {
            return new AutoValue_StreamItem_Video(Kind.VIDEO_AD, ad);
        }
    }
}
