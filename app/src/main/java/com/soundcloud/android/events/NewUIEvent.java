package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;

import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.List;

@AutoValue
public abstract class NewUIEvent extends NewTrackingEvent {

    public enum Kind {
        FOLLOW("follow"),
        UNFOLLOW("unfollow"),
        LIKE("like"),
        UNLIKE("unlike"),
        REPOST("repost"),
        UNREPOST("unrepost"),
        ADD_TO_PLAYLIST("add_to_playlist"),
        CREATE_PLAYLIST("create_playlist"),
        COMMENT("comment"),
        SHARE("share"),
        SHUFFLE("shuffle"),
        PLAY_QUEUE_SHUFFLE("play_queue_shuffle"),
        SWIPE_SKIP("swipe_skip"),
        SYSTEM_SKIP("system_skip"),
        BUTTON_SKIP("button_skip"),
        NAVIGATION("navigation"),
        PLAYER_OPEN("player_open"),
        PLAYER_CLOSE("player_close"),
        VIDEO_AD_FULLSCREEN("video_ad_fullscreen"),
        VIDEO_AD_SHRINK("video_ad_shrink"),
        AD_CLICKTHROUGH("ad_click_through"),
        SKIP_AD_CLICK("skip_ad_click"),
        START_STATION("start_station"),
        PLAY_QUEUE_OPEN("play_queue_open"),
        PLAY_QUEUE_CLOSE("play_queue_close"),
        PLAY_QUEUE_TRACK_REORDER("play_queue_track_reorder"),
        PLAY_QUEUE_TRACK_REMOVE("play_queue_track_remove"),
        PLAY_QUEUE_TRACK_REMOVE_UNDO("play_queue_track_remove_undo"),
        PLAY_QUEUE_REPEAT("play_queue_repeat"),
        PLAY_NEXT("play_next"),
        RECOMMENDED_PLAYLISTS("playlist_discovery");
        public final String key;

        Kind(String key) {
            this.key = key;
        }
    }

    public enum Trigger {
        AUTO("auto"),
        MANUAL("manual");
        public final String key;

        Trigger(String key) {
            this.key = key;
        }
    }

    public enum MonetizationType {
        PROMOTED("promoted"),
        AUDIO_AD("audio_ad"),
        VIDEO_AD("video_ad"),
        MOBILE_INLAY("mobile_inlay");
        public final String key;

        MonetizationType(String key) {
            this.key = key;
        }

        private static MonetizationType fromAdData(AdData adData) {
            if (adData instanceof AudioAd) {
                return AUDIO_AD;
            } else if (adData instanceof VideoAd) {
                return VIDEO_AD;
            } else {
                return MOBILE_INLAY;
            }
        }
    }

    public enum ClickName {
        SHUFFLE_ON("shuffle::on"),
        SHUFFLE_OFF("shuffle::off");
        public final String key;

        ClickName(String key) {
            this.key = key;
        }
    }

    public abstract Kind kind();

    public abstract Optional<Trigger> trigger();

    public abstract Optional<String> creatorName();

    public abstract Optional<Urn> creatorUrn();

    public abstract Optional<String> playableTitle();

    public abstract Optional<Urn> playableUrn();

    public abstract Optional<String> playableType();

    public abstract Optional<Urn> pageUrn();

    public abstract Optional<String> originScreen();

    public abstract Optional<AttributingActivity> attributingActivity();

    public abstract Optional<Module> module();

    public abstract Optional<String> linkType();

    public abstract Optional<ClickName> clickName();

    public abstract Optional<String> contextScreen();

    public abstract Optional<String> invokerScreen();

    public abstract Optional<String> clickSource();

    public abstract Optional<Urn> clickSourceUrn();

    public abstract Optional<Urn> queryUrn();

    public abstract Optional<Integer> queryPosition();

    public abstract Optional<Boolean> isFromOverflow();

    public abstract Optional<Urn> clickObjectUrn();

    public abstract Optional<String> adUrn();

    public abstract Optional<MonetizationType> monetizationType();

    public abstract Optional<Urn> monetizableTrackUrn();

    public abstract Optional<Urn> promoterUrn();

    public abstract Optional<List<String>> sizeChanges();

    public abstract Optional<List<String>> skips();

    public abstract Optional<List<String>> clickthroughs();

    public abstract Optional<String> clickthroughsKind();

    public abstract Optional<String> clickthroughsUrl();

    public abstract Optional<Uri> adArtworkUrl();

    public abstract Optional<String> playQueueRepeatMode();

    public static NewUIEvent fromPlayerOpen(boolean manual) {
        return event(Kind.PLAYER_OPEN).trigger(Optional.of(manual ? Trigger.MANUAL : Trigger.AUTO)).build();
    }

    public static NewUIEvent fromPlayerClose(boolean manual) {
        return event(Kind.PLAYER_CLOSE).trigger(Optional.of(manual ? Trigger.MANUAL : Trigger.AUTO)).build();
    }

    public static NewUIEvent fromToggleFollow(boolean isFollow, EntityMetadata userMetadata, EventContextMetadata eventContextMetadata) {
        Kind kind = isFollow ? Kind.FOLLOW : Kind.UNFOLLOW;
        return event(kind).entityMetadata(userMetadata).eventContextMetadata(eventContextMetadata).build();
    }

    public static NewUIEvent fromToggleLike(boolean isLike, Urn resourceUrn, EventContextMetadata contextMetadata, @Nullable PromotedSourceInfo promotedSourceInfo, EntityMetadata playable) {
        final Builder builder = event(isLike ? Kind.LIKE : Kind.UNLIKE).clickObjectUrn(Optional.of(resourceUrn)).eventContextMetadata(contextMetadata).entityMetadata(playable);
        if (promotedSourceInfo != null) {
            builder.promotedSourceInfo(promotedSourceInfo);
        }
        return builder.build();
    }

    public static NewUIEvent fromToggleRepost(boolean isRepost,
                                              Urn resourceUrn,
                                              EventContextMetadata contextMetadata,
                                              @Nullable PromotedSourceInfo promotedSourceInfo,
                                              EntityMetadata entityMetadata) {
        final Builder builder = event(isRepost ? Kind.REPOST : Kind.UNREPOST).clickObjectUrn(Optional.of(resourceUrn)).eventContextMetadata(contextMetadata).entityMetadata(entityMetadata);
        if (promotedSourceInfo != null) {
            builder.promotedSourceInfo(promotedSourceInfo);
        }
        return builder.build();
    }

    public static NewUIEvent fromAddToPlaylist(EventContextMetadata eventContextMetadata) {
        return event(Kind.ADD_TO_PLAYLIST).eventContextMetadata(eventContextMetadata).build();
    }

    public static NewUIEvent fromComment(EventContextMetadata eventContextMetadata, EntityMetadata playable) {
        return event(Kind.COMMENT).eventContextMetadata(eventContextMetadata).entityMetadata(playable).build();
    }

    public static NewUIEvent fromShare(Urn resourceUrn, EventContextMetadata contextMetadata, @Nullable PromotedSourceInfo promotedSourceInfo, EntityMetadata playable) {
        final Builder builder = event(Kind.SHARE).clickObjectUrn(Optional.of(resourceUrn)).eventContextMetadata(contextMetadata).entityMetadata(playable);
        if (promotedSourceInfo != null) {
            builder.promotedSourceInfo(promotedSourceInfo);
        }
        return builder.build();
    }

    public static NewUIEvent fromShuffle(EventContextMetadata contextMetadata) {
        return event(Kind.SHUFFLE).eventContextMetadata(contextMetadata).build();
    }

    public static NewUIEvent fromSystemSkip() {
        return event(Kind.SYSTEM_SKIP).build();
    }

    public static NewUIEvent fromButtonSkip() {
        return event(Kind.BUTTON_SKIP).build();
    }

    public static NewUIEvent fromSwipeSkip() {
        return event(Kind.SWIPE_SKIP).build();
    }

    public static NewUIEvent fromVideoAdFullscreen(VideoAd videoAd, @Nullable TrackSourceInfo trackSourceInfo) {
        return event(Kind.VIDEO_AD_FULLSCREEN).playerAdAttributes(videoAd, trackSourceInfo).sizeChanges(Optional.of(videoAd.getFullScreenUrls())).build();
    }

    public static NewUIEvent fromVideoAdShrink(VideoAd videoAd, @Nullable TrackSourceInfo trackSourceInfo) {
        return event(Kind.VIDEO_AD_SHRINK).playerAdAttributes(videoAd, trackSourceInfo).sizeChanges(Optional.of(videoAd.getExitFullScreenUrls())).build();
    }

    public static NewUIEvent fromSkipAdClick(PlayerAdData adData, @Nullable TrackSourceInfo trackSourceInfo) {
        return event(Kind.SKIP_AD_CLICK).playerAdAttributes(adData, trackSourceInfo).skips(Optional.of(adData.getSkipUrls())).build();
    }

    public static NewUIEvent fromPlayerAdClickThrough(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
        final String clickthroughKind = "clickthrough::" + MonetizationType.fromAdData(adData);
        final Builder builder = event(Kind.AD_CLICKTHROUGH).playerAdAttributes(adData, trackSourceInfo)
                                                           .clickthroughs(Optional.of(adData.getClickUrls()))
                                                           .clickthroughsKind(Optional.of(clickthroughKind));
        if (adData instanceof AudioAd) {
            final AudioAd audioAd = (AudioAd) adData;
            builder.clickthroughsUrl(audioAd.getClickThroughUrl());
            builder.adArtworkUrl(audioAd.getCompanionImageUrl());
        } else {
            builder.clickthroughsUrl(Optional.of(((VideoAd) adData).getClickThroughUrl()));
        }

        return builder.build();
    }

    public static NewUIEvent fromAppInstallAdClickThrough(AppInstallAd adData) {
        return event(Kind.AD_CLICKTHROUGH).basicAdAttributes(adData)
                                          .clickthroughs(Optional.of(adData.getClickUrls()))
                                          .clickthroughsUrl(Optional.of(adData.getClickThroughUrl()))
                                          .clickthroughsKind(Optional.of("clickthrough::app_install"))
                                          .build();
    }

    public static NewUIEvent fromStartStation() {
        return event(Kind.START_STATION).build();
    }

    public static NewUIEvent fromPlayQueueOpen() {
        return event(Kind.PLAY_QUEUE_OPEN).build();
    }

    public static NewUIEvent fromPlayQueueClose() {
        return event(Kind.PLAY_QUEUE_CLOSE).build();
    }

    public static NewUIEvent fromPlayQueueReorder(Screen screen) {
        return event(Kind.PLAY_QUEUE_TRACK_REORDER).originScreen(Optional.of(screen.get())).build();
    }

    public static NewUIEvent fromPlayQueueRemove(Screen screen) {
        return event(Kind.PLAY_QUEUE_TRACK_REMOVE).originScreen(Optional.of(screen.get())).build();
    }

    public static NewUIEvent fromPlayQueueRemoveUndo(Screen screen) {
        return event(Kind.PLAY_QUEUE_TRACK_REMOVE_UNDO).originScreen(Optional.of(screen.get())).build();
    }


    public static NewUIEvent fromCreatePlaylist(EntityMetadata metadata) {
        return event(Kind.CREATE_PLAYLIST).entityMetadata(metadata).build();
    }

    public static NewUIEvent fromNavigation(Urn itemUrn,
                                            EventContextMetadata contextMetadata) {
        return event(Kind.NAVIGATION).clickObjectUrn(Optional.of(itemUrn)).eventContextMetadata(contextMetadata).build();
    }

    public static NewUIEvent fromPlayQueueShuffle(boolean isShuffled) {
        return event(Kind.PLAY_QUEUE_SHUFFLE).clickName(Optional.of(isShuffled ? ClickName.SHUFFLE_ON : ClickName.SHUFFLE_OFF)).originScreen(Optional.of(Screen.PLAY_QUEUE.get())).build();
    }

    public static NewUIEvent fromPlayQueueRepeat(Screen screen, PlayQueueManager.RepeatMode repeatMode) {
        return event(Kind.PLAY_QUEUE_REPEAT).originScreen(Optional.of(screen.get())).playQueueRepeatMode(Optional.of(repeatMode.get())).build();
    }

    public static NewUIEvent fromPlayNext(Urn urn, String lastScreen, EventContextMetadata eventContextMetadata) {
        return event(Kind.PLAY_NEXT).eventContextMetadata(eventContextMetadata).clickObjectUrn(Optional.of(urn)).originScreen(Optional.of(lastScreen)).build();
    }

    public static NewUIEvent fromRecommendedPlaylists(Urn itemUrn,
                                                      EventContextMetadata contextMetadata) {
        return event(Kind.RECOMMENDED_PLAYLISTS).clickObjectUrn(Optional.of(itemUrn)).eventContextMetadata(contextMetadata).build();
    }

    private static Builder event(Kind kind) {
        return new AutoValue_NewUIEvent.Builder().id(defaultId())
                                                 .timestamp(defaultTimestamp())
                                                 .referringEvent(Optional.absent())
                                                 .kind(kind)
                                                 .sizeChanges(Optional.absent())
                                                 .skips(Optional.absent())
                                                 .clickthroughs(Optional.absent())
                                                 .clickthroughsKind(Optional.absent())
                                                 .clickthroughsUrl(Optional.absent())
                                                 .adArtworkUrl(Optional.absent())
                                                 .trigger(Optional.absent())
                                                 .creatorName(Optional.absent())
                                                 .creatorUrn(Optional.absent())
                                                 .playableTitle(Optional.absent())
                                                 .playableUrn(Optional.absent())
                                                 .playableType(Optional.absent())
                                                 .pageUrn(Optional.absent())
                                                 .originScreen(Optional.absent())
                                                 .attributingActivity(Optional.absent())
                                                 .module(Optional.absent())
                                                 .linkType(Optional.absent())
                                                 .contextScreen(Optional.absent())
                                                 .invokerScreen(Optional.absent())
                                                 .clickName(Optional.absent())
                                                 .clickObjectUrn(Optional.absent())
                                                 .clickSource(Optional.absent())
                                                 .clickSourceUrn(Optional.absent())
                                                 .queryUrn(Optional.absent())
                                                 .queryPosition(Optional.absent())
                                                 .isFromOverflow(Optional.absent())
                                                 .adUrn(Optional.absent())
                                                 .monetizationType(Optional.absent())
                                                 .promoterUrn(Optional.absent())
                                                 .playQueueRepeatMode(Optional.absent());
    }


    @Override
    public NewUIEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_NewUIEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder id(String id);

        abstract Builder timestamp(long timestamp);

        abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        abstract Builder kind(Kind kind);

        abstract Builder trigger(Optional<Trigger> trigger);

        abstract Builder creatorName(Optional<String> creatorName);

        abstract Builder creatorUrn(Optional<Urn> creatorUrn);

        abstract Builder playableTitle(Optional<String> playableTitle);

        abstract Builder playableUrn(Optional<Urn> playableUrn);

        abstract Builder playableType(Optional<String> playableType);

        abstract Builder pageUrn(Optional<Urn> pageUrn);

        abstract Builder originScreen(Optional<String> originScreen);

        abstract Builder attributingActivity(Optional<AttributingActivity> attributingActivity);

        abstract Builder module(Optional<Module> module);

        abstract Builder linkType(Optional<String> linkType);

        abstract Builder contextScreen(Optional<String> contextScreen);

        abstract Builder invokerScreen(Optional<String> invokerScreen);

        abstract Builder clickName(Optional<ClickName> clickname);

        abstract Builder clickObjectUrn(Optional<Urn> clickObjectUrn);

        abstract Builder clickSource(Optional<String> clickSource);

        abstract Builder clickSourceUrn(Optional<Urn> clickSourceUrn);

        abstract Builder queryUrn(Optional<Urn> queryUrn);

        abstract Builder queryPosition(Optional<Integer> queryPosition);

        abstract Builder isFromOverflow(Optional<Boolean> isFromOverflow);

        abstract Builder adUrn(Optional<String> adUrn);

        abstract Builder monetizationType(Optional<MonetizationType> monetizationType);

        abstract Builder monetizableTrackUrn(Optional<Urn> monetizableTrackUrn);

        abstract Builder promoterUrn(Optional<Urn> promoterUrn);

        abstract Builder sizeChanges(Optional<List<String>> sizeChanges);

        abstract Builder skips(Optional<List<String>> skips);

        abstract Builder clickthroughs(Optional<List<String>> clickthroughs);

        abstract Builder clickthroughsKind(Optional<String> clickthroughsKind);

        abstract Builder clickthroughsUrl(Optional<String> clickthroughsUrl);

        abstract Builder adArtworkUrl(Optional<Uri> adArtworkUrl);

        abstract Builder playQueueRepeatMode(Optional<String> playQueueRepeatMode);

        Builder entityMetadata(EntityMetadata entityMetadata) {
            creatorName(Optional.of(entityMetadata.creatorName));
            creatorUrn(Optional.of(entityMetadata.creatorUrn));
            playableTitle(Optional.of(entityMetadata.playableTitle));
            playableUrn(Optional.of(entityMetadata.playableUrn));
            playableType(Optional.of(entityMetadata.getPlayableType()));
            return this;
        }

        Builder eventContextMetadata(EventContextMetadata eventContextMetadata) {
            pageUrn(Optional.of(eventContextMetadata.pageUrn()));
            originScreen(Optional.fromNullable(eventContextMetadata.pageName()));
            contextScreen(Optional.fromNullable(eventContextMetadata.contextScreen()));
            invokerScreen(Optional.fromNullable(eventContextMetadata.invokerScreen()));
            attributingActivity(Optional.fromNullable(eventContextMetadata.attributingActivity()));
            linkType(Optional.fromNullable(eventContextMetadata.linkType()).transform(LinkType::getName));
            module(Optional.fromNullable(eventContextMetadata.module()));
            isFromOverflow(Optional.of(eventContextMetadata.isFromOverflow()));
            trackSourceInfo(eventContextMetadata.trackSourceInfo());
            return this;
        }

        Builder trackSourceInfo(TrackSourceInfo sourceInfo) {
            if (sourceInfo != null) {
                if (sourceInfo.hasSource()) {
                    clickSource(Optional.fromNullable(sourceInfo.getSource()));
                }
                if (sourceInfo.hasCollectionUrn()) {
                    clickSourceUrn(Optional.fromNullable(sourceInfo.getCollectionUrn()));
                }
                if (sourceInfo.hasStationsSourceInfo()) {
                    queryUrn(Optional.of(sourceInfo.getStationsSourceInfo().getQueryUrn()));
                } else if (sourceInfo.hasQuerySourceInfo()) {
                    queryUrn(Optional.of(sourceInfo.getQuerySourceInfo().getQueryUrn()));
                    queryPosition(Optional.of(sourceInfo.getQuerySourceInfo().getQueryPosition()));
                }
            }
            return this;
        }

        Builder promotedSourceInfo(PromotedSourceInfo promotedSourceInfo) {
            adUrn(Optional.of(promotedSourceInfo.getAdUrn()));
            monetizationType(Optional.of(MonetizationType.PROMOTED));
            promoterUrn(promotedSourceInfo.getPromoterUrn());
            return this;
        }

        Builder basicAdAttributes(AdData adData) {
            adUrn(Optional.of(adData.getAdUrn().toString()));
            monetizationType(Optional.of(MonetizationType.fromAdData(adData)));
            return this;
        }

        Builder playerAdAttributes(PlayerAdData adData, TrackSourceInfo trackSourceInfo) {
            basicAdAttributes(adData);
            monetizableTrackUrn(Optional.of(adData.getMonetizableTrackUrn()));
            originScreen(Optional.fromNullable(trackSourceInfo.getOriginScreen()));
            return this;
        }

        abstract NewUIEvent build();
    }
}
