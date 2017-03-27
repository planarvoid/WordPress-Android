package com.soundcloud.android.events;

import static com.soundcloud.android.events.UIEvent.ClickName.FOLLOW_ADD;
import static com.soundcloud.android.events.UIEvent.ClickName.FOLLOW_REMOVE;
import static com.soundcloud.android.events.UIEvent.Kind.FOLLOW;
import static com.soundcloud.android.events.UIEvent.Kind.UNFOLLOW;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.List;

@AutoValue
public abstract class UIEvent extends TrackingEvent {

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
        VIDEO_AD_MUTE("video_ad_mute"),
        VIDEO_AD_UNMUTE("video_ad_unmute"),
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
        RECOMMENDED_PLAYLISTS("playlist_discovery"),
        MORE_PLAYLISTS_BY_USER("more_playlists_by_user");
        private final String key;

        Kind(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public String getKind() {
        return kind().toString();
    }

    public enum Trigger {
        AUTO("auto"),
        MANUAL("manual");
        private final String key;

        Trigger(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum ShareLinkType {
        FIREBASE("firebase"),
        SOUNDCLOUD("soundcloud");
        private final String key;

        ShareLinkType(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum ClickName {
        SHARE_REQUEST("share::request"),
        SHARE_CANCEL("share::cancel"),
        SHARE_PROMPT("share::prompt"),
        REPOST("repost::add"),
        UNREPOST("repost::remove"),
        LIKE("like::add"),
        UNLIKE("like::remove"),
        SHUFFLE("shuffle:on"),
        SWIPE_SKIP("swipe_skip"),
        SYSTEM_SKIP("system_skip"),
        BUTTON_SKIP("button_skip"),
        VIDEO_AD_FULLSCREEN("ad::full_screen"),
        VIDEO_AD_SHRINK("ad::exit_full_screen"),
        VIDEO_AD_MUTE("ad::mute"),
        VIDEO_AD_UNMUTE("ad::unmute"),
        SKIP_AD_CLICK("ad::skip"),
        FOLLOW_ADD("follow::add"),
        FOLLOW_REMOVE("follow::remove"),
        PLAYER_OPEN("player::max"),
        PLAYER_CLOSE("player::min"),
        PLAY_QUEUE_OPEN("play_queue::max"),
        PLAY_QUEUE_CLOSE("play_queue::min"),
        PLAY_QUEUE_TRACK_REORDER("track_in_play_queue::reorder"),
        PLAY_QUEUE_TRACK_REMOVE("track_in_play_queue::remove"),
        PLAY_QUEUE_TRACK_REMOVE_UNDO("track_in_play_queue::remove_undo"),
        PLAY_QUEUE_REPEAT_ON("repeat::on"),
        PLAY_QUEUE_REPEAT_OFF("repeat::off"),
        PLAY_NEXT("play_next"),
        RECOMMENDED_PLAYLIST("item_navigation"),
        MORE_PLAYLISTS_BY_USER("item_navigation"),
        SHUFFLE_ON("shuffle::on"),
        SHUFFLE_OFF("shuffle::off");
        private final String key;

        ClickName(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum ClickCategory {
        PLAYBACK("playback"),
        PLAYER("player_interaction"),
        ENGAGEMENT("engagement");
        private final String key;

        ClickCategory(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum Action {
        SHARE("share"),
        REPOST_ADD("repost::add"),
        REPOST_REMOVE("repost::remove"),
        LIKE_ADD("like::add"),
        LIKE_REMOVE("like::remove"),
        FOLLOW_ADD("follow::add"),
        FOLLOW_REMOVE("follow::remove"),
        NAVIGATION("item_navigation");
        private final String key;

        Action(String key) {
            this.key = key;
        }

        public String key() {
            return key;
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

    public abstract Optional<ClickCategory> clickCategory();

    public abstract Optional<String> contextScreen();

    public abstract Optional<String> invokerScreen();

    public abstract Optional<String> clickSource();

    public abstract Optional<Urn> clickSourceUrn();

    public abstract Optional<Urn> queryUrn();

    public abstract Optional<Integer> queryPosition();

    public abstract Optional<Boolean> isFromOverflow();

    public abstract Optional<Urn> clickObjectUrn();

    public abstract Optional<String> adUrn();

    public abstract Optional<AdData.MonetizationType> monetizationType();

    public abstract Optional<Urn> monetizableTrackUrn();

    public abstract Optional<Urn> promoterUrn();

    public abstract Optional<List<String>> adTrackingUrls();

    public abstract Optional<String> clickthroughsKind();

    public abstract Optional<String> clickthroughsUrl();

    public abstract Optional<Uri> adArtworkUrl();

    public abstract Optional<String> playQueueRepeatMode();

    public abstract Optional<ShareLinkType> shareLinkType();

    public abstract Optional<Action> action();

    public static UIEvent fromPlayerOpen(boolean manual) {
        return event(Kind.PLAYER_OPEN, ClickName.PLAYER_OPEN).trigger(Optional.of(manual ? Trigger.MANUAL : Trigger.AUTO)).build();
    }

    public static UIEvent fromPlayerClose(boolean manual) {
        return event(Kind.PLAYER_CLOSE, ClickName.PLAYER_CLOSE).trigger(Optional.of(manual ? Trigger.MANUAL : Trigger.AUTO)).build();
    }

    public static UIEvent fromToggleFollow(boolean isFollow, EntityMetadata userMetadata, EventContextMetadata eventContextMetadata) {
        final Kind kind = isFollow ? FOLLOW : UNFOLLOW;
        final ClickName clickName = isFollow ? FOLLOW_ADD : FOLLOW_REMOVE;
        final Action action = isFollow ? Action.FOLLOW_ADD : Action.FOLLOW_REMOVE;
        return event(kind, clickName).clickCategory(Optional.of(ClickCategory.ENGAGEMENT)).entityMetadata(userMetadata).eventContextMetadata(eventContextMetadata).action(Optional.of(action)).build();
    }

    public static UIEvent fromToggleLike(boolean isLike, Urn resourceUrn, EventContextMetadata contextMetadata, @Nullable PromotedSourceInfo promotedSourceInfo, EntityMetadata playable) {
        final Kind kind = isLike ? Kind.LIKE : Kind.UNLIKE;
        final ClickName clickName = isLike ? ClickName.LIKE : ClickName.UNLIKE;
        final Action action = isLike ? Action.LIKE_ADD : Action.LIKE_REMOVE;
        final Builder builder = event(kind, clickName).clickCategory(Optional.of(ClickCategory.ENGAGEMENT))
                                                      .clickObjectUrn(Optional.of(resourceUrn))
                                                      .eventContextMetadata(contextMetadata)
                                                      .action(Optional.of(action))
                                                      .entityMetadata(playable);
        if (promotedSourceInfo != null) {
            builder.promotedSourceInfo(promotedSourceInfo);
        }
        return builder.build();
    }

    public static UIEvent fromToggleRepost(boolean isRepost,
                                           Urn resourceUrn,
                                           EventContextMetadata contextMetadata,
                                           @Nullable PromotedSourceInfo promotedSourceInfo,
                                           EntityMetadata entityMetadata) {
        final Kind kind = isRepost ? Kind.REPOST : Kind.UNREPOST;
        final ClickName clickName = isRepost ? ClickName.REPOST : ClickName.UNREPOST;
        final Action action = isRepost ? Action.REPOST_ADD : Action.REPOST_REMOVE;
        final Builder builder = event(kind, clickName).clickCategory(Optional.of(ClickCategory.ENGAGEMENT))
                                                      .clickObjectUrn(Optional.of(resourceUrn))
                                                      .eventContextMetadata(contextMetadata)
                                                      .action(Optional.of(action))
                                                      .entityMetadata(entityMetadata);
        if (promotedSourceInfo != null) {
            builder.promotedSourceInfo(promotedSourceInfo);
        }
        return builder.build();
    }

    public static UIEvent fromAddToPlaylist(EventContextMetadata eventContextMetadata) {
        return event(Kind.ADD_TO_PLAYLIST).eventContextMetadata(eventContextMetadata).build();
    }

    public static UIEvent fromComment(EventContextMetadata eventContextMetadata, EntityMetadata playable) {
        return event(Kind.COMMENT).eventContextMetadata(eventContextMetadata).entityMetadata(playable).build();
    }

    public static UIEvent fromShuffle(EventContextMetadata contextMetadata) {
        return event(Kind.SHUFFLE, ClickName.SHUFFLE).clickCategory(Optional.of(ClickCategory.PLAYBACK)).eventContextMetadata(contextMetadata).build();
    }

    public static UIEvent fromSystemSkip() {
        return event(Kind.SYSTEM_SKIP, ClickName.SYSTEM_SKIP).clickCategory(Optional.of(ClickCategory.PLAYER)).build();
    }

    public static UIEvent fromButtonSkip() {
        return event(Kind.BUTTON_SKIP, ClickName.BUTTON_SKIP).clickCategory(Optional.of(ClickCategory.PLAYER)).build();
    }

    public static UIEvent fromSwipeSkip() {
        return event(Kind.SWIPE_SKIP, ClickName.SWIPE_SKIP).clickCategory(Optional.of(ClickCategory.PLAYER)).build();
    }

    public static UIEvent fromVideoAdFullscreen(VideoAd videoAd, @Nullable TrackSourceInfo trackSourceInfo) {
        return event(Kind.VIDEO_AD_FULLSCREEN, ClickName.VIDEO_AD_FULLSCREEN).playableAdAttributes(videoAd, trackSourceInfo).adTrackingUrls(Optional.of(videoAd.getFullScreenUrls())).build();
    }

    public static UIEvent fromVideoAdShrink(VideoAd videoAd, @Nullable TrackSourceInfo trackSourceInfo) {
        return event(Kind.VIDEO_AD_SHRINK, ClickName.VIDEO_AD_SHRINK).playableAdAttributes(videoAd, trackSourceInfo).adTrackingUrls(Optional.of(videoAd.getExitFullScreenUrls())).build();
    }

    public static UIEvent fromVideoMute(VideoAd videoAd, TrackSourceInfo sourceInfo) {
        return event(Kind.VIDEO_AD_MUTE, ClickName.VIDEO_AD_MUTE).playableAdAttributes(videoAd, sourceInfo).adTrackingUrls(Optional.of(videoAd.getMuteUrls())).build();
    }

    public static UIEvent fromVideoUnmute(VideoAd videoAd, TrackSourceInfo sourceInfo) {
        return event(Kind.VIDEO_AD_UNMUTE, ClickName.VIDEO_AD_UNMUTE).playableAdAttributes(videoAd, sourceInfo).adTrackingUrls(Optional.of(videoAd.getUnmuteUrls())).build();
    }

    public static UIEvent fromSkipAdClick(PlayableAdData adData, @Nullable TrackSourceInfo trackSourceInfo) {
        return event(Kind.SKIP_AD_CLICK, ClickName.SKIP_AD_CLICK).playableAdAttributes(adData, trackSourceInfo).adTrackingUrls(Optional.of(adData.getSkipUrls())).build();
    }

    public static UIEvent fromPlayableClickThrough(PlayableAdData adData, TrackSourceInfo trackSourceInfo) {
        final String clickthroughKind = "clickthrough::" + adData.getMonetizationType().key();
        final Builder builder = event(Kind.AD_CLICKTHROUGH).playableAdAttributes(adData, trackSourceInfo)
                                                           .adTrackingUrls(Optional.of(adData.getClickUrls()))
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

    public static UIEvent fromAppInstallAdClickThrough(AppInstallAd adData) {
        return event(Kind.AD_CLICKTHROUGH).basicAdAttributes(adData)
                                          .adTrackingUrls(Optional.of(adData.getClickUrls()))
                                          .clickthroughsUrl(Optional.of(adData.getClickThroughUrl()))
                                          .clickthroughsKind(Optional.of("clickthrough::app_install"))
                                          .build();
    }

    public static UIEvent fromStartStation() {
        return event(Kind.START_STATION).build();
    }

    public static UIEvent fromPlayQueueOpen() {
        return event(Kind.PLAY_QUEUE_OPEN, ClickName.PLAY_QUEUE_OPEN).build();
    }

    public static UIEvent fromPlayQueueClose() {
        return event(Kind.PLAY_QUEUE_CLOSE, ClickName.PLAY_QUEUE_CLOSE).build();
    }

    public static UIEvent fromPlayQueueReorder(Screen screen) {
        return event(Kind.PLAY_QUEUE_TRACK_REORDER, ClickName.PLAY_QUEUE_TRACK_REORDER).originScreen(Optional.of(screen.get())).build();
    }

    public static UIEvent fromPlayQueueRemove(Screen screen) {
        return event(Kind.PLAY_QUEUE_TRACK_REMOVE, ClickName.PLAY_QUEUE_TRACK_REMOVE).originScreen(Optional.of(screen.get())).build();
    }

    public static UIEvent fromPlayQueueRemoveUndo(Screen screen) {
        return event(Kind.PLAY_QUEUE_TRACK_REMOVE_UNDO, ClickName.PLAY_QUEUE_TRACK_REMOVE_UNDO).originScreen(Optional.of(screen.get())).build();
    }

    public static UIEvent fromCreatePlaylist(EntityMetadata metadata) {
        return event(Kind.CREATE_PLAYLIST).entityMetadata(metadata).build();
    }

    public static UIEvent fromNavigation(Urn itemUrn, EventContextMetadata contextMetadata) {
        return event(Kind.NAVIGATION).action(Optional.of(Action.NAVIGATION)).clickObjectUrn(Optional.of(itemUrn)).eventContextMetadata(contextMetadata).build();
    }

    public static UIEvent fromPlayQueueShuffle(boolean isShuffled) {
        return event(Kind.PLAY_QUEUE_SHUFFLE).clickName(Optional.of(isShuffled ? ClickName.SHUFFLE_ON : ClickName.SHUFFLE_OFF)).originScreen(Optional.of(Screen.PLAY_QUEUE.get())).build();
    }

    public static UIEvent fromPlayQueueRepeat(Screen screen, PlayQueueManager.RepeatMode repeatMode) {
        final Builder builder = event(Kind.PLAY_QUEUE_REPEAT).originScreen(Optional.of(screen.get()));
        if (Strings.isNotBlank(repeatMode.get())) {
            builder.playQueueRepeatMode(Optional.of(repeatMode.get()));
            builder.clickName(Optional.of(ClickName.PLAY_QUEUE_REPEAT_ON));
        } else {
            builder.clickName(Optional.of(ClickName.PLAY_QUEUE_REPEAT_OFF));
        }
        return builder.build();
    }

    public static UIEvent fromPlayNext(Urn urn, String lastScreen, EventContextMetadata eventContextMetadata) {
        return event(Kind.PLAY_NEXT, ClickName.PLAY_NEXT).clickCategory(Optional.of(ClickCategory.ENGAGEMENT))
                                                         .eventContextMetadata(eventContextMetadata)
                                                         .clickObjectUrn(Optional.of(urn))
                                                         .originScreen(Optional.of(lastScreen))
                                                         .build();
    }

    public static UIEvent fromRecommendedPlaylists(Urn itemUrn,
                                                   EventContextMetadata contextMetadata) {
        return event(Kind.RECOMMENDED_PLAYLISTS, ClickName.RECOMMENDED_PLAYLIST).clickObjectUrn(Optional.of(itemUrn)).eventContextMetadata(contextMetadata).build();
    }

    public static UIEvent fromMorePlaylistsByUser(Urn itemUrn,
                                                  EventContextMetadata contextMetadata) {
        return event(Kind.MORE_PLAYLISTS_BY_USER, ClickName.MORE_PLAYLISTS_BY_USER).clickObjectUrn(Optional.of(itemUrn)).eventContextMetadata(contextMetadata).build();
    }

    public static UIEvent fromShareRequest(Urn resourceUrn, EventContextMetadata contextMetadata, @Nullable PromotedSourceInfo promotedSourceInfo, EntityMetadata playable) {
        return shareEvent(ClickName.SHARE_REQUEST, resourceUrn, contextMetadata, promotedSourceInfo, playable).build();
    }

    public static UIEvent fromSharePromptWithFirebaseLink(Urn resourceUrn, EventContextMetadata contextMetadata, @Nullable PromotedSourceInfo promotedSourceInfo, EntityMetadata playable) {
        return shareEvent(ClickName.SHARE_PROMPT, resourceUrn, contextMetadata, promotedSourceInfo, playable)
                .shareLinkType(Optional.of(ShareLinkType.FIREBASE))
                .build();
    }

    public static UIEvent fromSharePromptWithSoundCloudLink(Urn resourceUrn, EventContextMetadata contextMetadata, @Nullable PromotedSourceInfo promotedSourceInfo, EntityMetadata playable) {
        return shareEvent(ClickName.SHARE_PROMPT, resourceUrn, contextMetadata, promotedSourceInfo, playable)
                .shareLinkType(Optional.of(ShareLinkType.SOUNDCLOUD))
                .build();
    }

    public static UIEvent fromShareCancel(Urn resourceUrn, EventContextMetadata contextMetadata, @Nullable PromotedSourceInfo promotedSourceInfo, EntityMetadata playable) {
        return shareEvent(ClickName.SHARE_CANCEL, resourceUrn, contextMetadata, promotedSourceInfo, playable).build();
    }

    private static UIEvent.Builder shareEvent(ClickName clickName,
                                              Urn resourceUrn,
                                              EventContextMetadata contextMetadata,
                                              @Nullable PromotedSourceInfo promotedSourceInfo,
                                              EntityMetadata playable) {
        final Builder builder = event(Kind.SHARE, clickName).clickObjectUrn(Optional.of(resourceUrn))
                                                            .clickCategory(Optional.of(ClickCategory.ENGAGEMENT))
                                                            .eventContextMetadata(contextMetadata)
                                                            .entityMetadata(playable)
                                                            .action(Optional.of(Action.SHARE));
        if (promotedSourceInfo != null) {
            builder.promotedSourceInfo(promotedSourceInfo);
        }
        return builder;
    }

    private static Builder event(Kind kind, ClickName clickName) {
        return event(kind).clickName(Optional.of(clickName));
    }

    private static Builder event(Kind kind) {
        return new AutoValue_UIEvent.Builder().id(defaultId())
                                              .timestamp(defaultTimestamp())
                                              .referringEvent(Optional.absent())
                                              .kind(kind)
                                              .adTrackingUrls(Optional.absent())
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
                                              .clickCategory(Optional.absent())
                                              .clickObjectUrn(Optional.absent())
                                              .clickSource(Optional.absent())
                                              .clickSourceUrn(Optional.absent())
                                              .queryUrn(Optional.absent())
                                              .queryPosition(Optional.absent())
                                              .isFromOverflow(Optional.absent())
                                              .adUrn(Optional.absent())
                                              .monetizationType(Optional.absent())
                                              .monetizableTrackUrn(Optional.absent())
                                              .promoterUrn(Optional.absent())
                                              .playQueueRepeatMode(Optional.absent())
                                              .shareLinkType(Optional.absent())
                                              .action(Optional.absent());
    }

    @Override
    public UIEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_UIEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
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

        abstract Builder clickName(Optional<ClickName> clickName);

        abstract Builder clickCategory(Optional<ClickCategory> clickCategory);

        abstract Builder clickObjectUrn(Optional<Urn> clickObjectUrn);

        abstract Builder clickSource(Optional<String> clickSource);

        Builder clickSource(String clickSource) {
            return clickSource(Optional.of(clickSource));
        }

        abstract Builder clickSourceUrn(Optional<Urn> clickSourceUrn);

        abstract Builder queryUrn(Optional<Urn> queryUrn);

        abstract Builder queryPosition(Optional<Integer> queryPosition);

        abstract Builder isFromOverflow(Optional<Boolean> isFromOverflow);

        abstract Builder adUrn(Optional<String> adUrn);

        abstract Builder monetizationType(Optional<AdData.MonetizationType> monetizationType);

        abstract Builder monetizableTrackUrn(Optional<Urn> monetizableTrackUrn);

        abstract Builder promoterUrn(Optional<Urn> promoterUrn);

        abstract Builder adTrackingUrls(Optional<List<String>> trackingUrls);

        abstract Builder clickthroughsKind(Optional<String> clickthroughsKind);

        abstract Builder clickthroughsUrl(Optional<String> clickthroughsUrl);

        abstract Builder adArtworkUrl(Optional<Uri> adArtworkUrl);

        abstract Builder playQueueRepeatMode(Optional<String> playQueueRepeatMode);

        abstract Builder shareLinkType(Optional<ShareLinkType> shareLinkType);

        abstract Builder action(Optional<Action> action);

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
            if (eventContextMetadata.clickSource().isPresent()) {
                clickSource(eventContextMetadata.clickSource());
            }
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
                }
                if (sourceInfo.hasQuerySourceInfo()) {
                    queryPosition(Optional.of(sourceInfo.getQuerySourceInfo().getQueryPosition()));
                }
            }
            return this;
        }

        Builder promotedSourceInfo(PromotedSourceInfo promotedSourceInfo) {
            adUrn(Optional.of(promotedSourceInfo.getAdUrn()));
            monetizationType(Optional.of(AdData.MonetizationType.PROMOTED));
            promoterUrn(promotedSourceInfo.getPromoterUrn());
            return this;
        }

        Builder basicAdAttributes(AdData adData) {
            adUrn(Optional.of(adData.getAdUrn().toString()));
            monetizationType(Optional.of(adData.getMonetizationType()));
            return this;
        }

        Builder playableAdAttributes(PlayableAdData adData, TrackSourceInfo trackSourceInfo) {
            basicAdAttributes(adData);
            monetizableTrackUrn(Optional.fromNullable(adData.getMonetizableTrackUrn()));
            if (trackSourceInfo != null) {
                originScreen(Optional.fromNullable(trackSourceInfo.getOriginScreen()));
            }
            return this;
        }

        abstract UIEvent build();
    }
}
