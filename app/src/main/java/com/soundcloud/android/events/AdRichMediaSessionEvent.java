package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.discovery.recommendations.QuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.StopReasonProvider;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class AdRichMediaSessionEvent extends TrackingEvent {

    private static final String RICH_MEDIA_STREAM_EVENT = "rich_media_stream";

    public enum Action {
        AUDIO_ACTION_PLAY("play"),
        AUDIO_ACTION_PAUSE("pause"),
        AUDIO_ACTION_CHECKPOINT("checkpoint");
        private final String key;

        Action(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum Trigger {
        MANUAL("manual"), AUTO("auto");
        private final String key;

        Trigger(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public abstract String eventName();

    public abstract Action action();

    public abstract Urn adUrn();

    public abstract Optional<Urn> monetizableTrackUrn();

    public abstract PlayableAdData.MonetizationType monetizationType();

    public abstract String pageName();

    public abstract Trigger trigger();

    public abstract Optional<StopReasonProvider.StopReason> stopReason();

    public abstract long playheadPosition();

    public abstract String clickEventId();

    public abstract String protocol();

    public abstract String playerType();

    public abstract long trackLength();

    public abstract Optional<String> source();

    public abstract Optional<String> sourceVersion();

    public abstract Optional<Urn> inPlaylist();

    public abstract Optional<Integer> playlistPosition();

    public abstract Optional<Urn> reposter();

    public abstract Optional<Urn> queryUrn();

    public abstract Optional<Integer> queryPosition();

    public abstract Optional<Urn> sourceUrn();


    public static AdRichMediaSessionEvent forPlay(PlayableAdData adData, AdSessionEventArgs eventArgs) {
        return create(Action.AUDIO_ACTION_PLAY, adData, eventArgs).build();
    }

    public static AdRichMediaSessionEvent forStop(PlayableAdData adData, AdSessionEventArgs eventArgs, StopReasonProvider.StopReason stopReason) {
        return create(Action.AUDIO_ACTION_PAUSE, adData, eventArgs).stopReason(Optional.of(stopReason)).build();
    }

    public static AdRichMediaSessionEvent forCheckpoint(PlayableAdData adData, AdSessionEventArgs eventArgs) {
        return create(Action.AUDIO_ACTION_CHECKPOINT, adData, eventArgs).build();
    }

    private static Builder create(Action action, PlayableAdData adData, AdSessionEventArgs eventArgs) {
        final TrackSourceInfo trackSourceInfo = eventArgs.getTrackSourceInfo();
        return new AutoValue_AdRichMediaSessionEvent.Builder().id(defaultId())
                                                              .timestamp(defaultTimestamp())
                                                              .referringEvent(Optional.absent())
                                                              .eventName(RICH_MEDIA_STREAM_EVENT)
                                                              .action(action)
                                                              .adUrn(adData.getAdUrn())
                                                              .monetizableTrackUrn(Optional.fromNullable(adData.getMonetizableTrackUrn()))
                                                              .monetizationType(adData.getMonetizationType())
                                                              .pageName(trackSourceInfo.getOriginScreen())
                                                              .trigger(trackSourceInfo.getIsUserTriggered() ? Trigger.MANUAL : Trigger.AUTO)
                                                              .source(Optional.absent())
                                                              .sourceVersion(Optional.absent())
                                                              .inPlaylist(Optional.absent())
                                                              .playlistPosition(Optional.absent())
                                                              .reposter(Optional.absent())
                                                              .queryUrn(Optional.absent())
                                                              .queryPosition(Optional.absent())
                                                              .sourceUrn(Optional.absent())
                                                              .stopReason(Optional.absent())
                                                              .eventArgs(eventArgs)
                                                              .trackSourceInfo(trackSourceInfo);
    }

    @Override
    public AdRichMediaSessionEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_AdRichMediaSessionEvent.Builder(this).referringEvent(Optional.of(referringEvent)).build();
    }

    @AutoValue.Builder
    abstract static class Builder {
        abstract Builder id(String id);

        abstract Builder timestamp(long timestamp);

        abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        abstract Builder action(Action action);

        abstract Builder eventName(String eventName);

        abstract Builder adUrn(Urn adUrn);

        abstract Builder monetizableTrackUrn(Optional<Urn> monetizableTrackUrn);

        abstract Builder monetizationType(PlayableAdData.MonetizationType monetizationType);

        abstract Builder pageName(String pageName);

        abstract Builder trigger(Trigger trigger);

        abstract Builder stopReason(Optional<StopReasonProvider.StopReason> stopReason);

        abstract Builder playheadPosition(long playheadPosition);

        abstract Builder clickEventId(String clickEventId);

        abstract Builder protocol(String protocol);

        abstract Builder playerType(String playerType);

        abstract Builder trackLength(long trackLength);

        abstract Builder source(Optional<String> source);

        abstract Builder sourceVersion(Optional<String> sourceVersion);

        abstract Builder inPlaylist(Optional<Urn> inPlaylist);

        abstract Builder playlistPosition(Optional<Integer> playlistPosition);

        abstract Builder reposter(Optional<Urn> reposter);

        abstract Builder queryUrn(Optional<Urn> queryUrn);

        abstract Builder queryPosition(Optional<Integer> queryPosition);

        abstract Builder sourceUrn(Optional<Urn> sourceUrn);

        private Builder eventArgs(AdSessionEventArgs eventArgs) {
            playheadPosition(eventArgs.getProgress());
            clickEventId(eventArgs.getUuid());
            protocol(eventArgs.getProtocol());
            playerType(eventArgs.getPlayerType());
            trackLength(eventArgs.getDuration());
            return this;
        }

        private Builder trackSourceInfo(TrackSourceInfo sourceInfo) {
            if (sourceInfo.hasSource()) {
                source(Optional.of(sourceInfo.getSource()));
                sourceVersion(Optional.of(sourceInfo.getSourceVersion()));
            }

            if (sourceInfo.isFromPlaylist()) {
                inPlaylist(Optional.of(sourceInfo.getCollectionUrn()));
                playlistPosition(Optional.of(sourceInfo.getPlaylistPosition()));
            }

            if (sourceInfo.hasReposter()) {
                reposter(Optional.of(sourceInfo.getReposter()));
            }

            if (sourceInfo.isFromSearchQuery()) {
                SearchQuerySourceInfo searchQuerySourceInfo = sourceInfo.getSearchQuerySourceInfo();
                queryUrn(Optional.of(searchQuerySourceInfo.getQueryUrn()));
                queryPosition(Optional.of(searchQuerySourceInfo.getUpdatedResultPosition(Urn.NOT_SET)));
            }

            if (sourceInfo.isFromStation()) {
                sourceUrn(Optional.of(sourceInfo.getCollectionUrn()));

                if (!sourceInfo.getStationsSourceInfo().getQueryUrn().equals(Urn.NOT_SET)) {
                    queryUrn(Optional.of(sourceInfo.getStationsSourceInfo().getQueryUrn()));
                }
            }

            if (sourceInfo.hasQuerySourceInfo()) {
                QuerySourceInfo querySourceInfo = sourceInfo.getQuerySourceInfo();
                queryUrn(Optional.of(querySourceInfo.getQueryUrn()));
                queryPosition(Optional.of(querySourceInfo.getQueryPosition()));
            }

            return this;
        }

        abstract AdRichMediaSessionEvent build();
    }
}
