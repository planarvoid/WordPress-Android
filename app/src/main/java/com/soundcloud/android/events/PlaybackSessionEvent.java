package com.soundcloud.android.events;

import static com.soundcloud.android.events.PlaybackSessionEvent.Kind.EVENT_KIND_CHECKPOINT;
import static com.soundcloud.android.events.PlaybackSessionEvent.Kind.EVENT_KIND_PLAY;
import static com.soundcloud.android.events.PlaybackSessionEvent.Kind.EVENT_KIND_PLAY_START;
import static com.soundcloud.android.events.PlaybackSessionEvent.Kind.EVENT_KIND_STOP;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Durations;
import com.soundcloud.android.playback.StopReasonProvider.StopReason;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.NonNull;

import java.util.List;

@AutoValue
public abstract class PlaybackSessionEvent extends TrackingEvent {
    public static final String EVENT_NAME = "audio";

    public enum Kind {
        EVENT_KIND_PLAY_START("play_start"),
        EVENT_KIND_PLAY("play"),
        EVENT_KIND_STOP("pause"),
        EVENT_KIND_CHECKPOINT("checkpoint");
        private final String key;

        Kind(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private static final String MONETIZATION_PROMOTED = "promoted";

    public abstract Kind kind();

    public abstract Urn trackUrn();

    public abstract Urn creatorUrn();

    public abstract String creatorName();

    public abstract String playableTitle();

    public abstract Urn playableUrn();

    public abstract String playableType();

    public abstract long duration();

    public abstract long progress();

    public abstract boolean isOfflineTrack();

    public abstract boolean marketablePlay();

    public abstract String clientEventId();

    public abstract Optional<String> playId();

    public abstract String monetizationModel();

    public abstract String protocol();

    public abstract Optional<String> policy();

    public abstract String playerType();

    public abstract Optional<StopReason> stopReason();

    public abstract Optional<Long> listenTime();

    public abstract TrackSourceInfo trackSourceInfo();

    public abstract Optional<String> adUrn();

    public abstract Optional<String> monetizationType();

    public abstract Optional<Urn> promoterUrn();

    public abstract Optional<Boolean> shouldReportAdStart();

    public abstract Optional<List<String>> promotedPlayUrls();

    public static PlaybackSessionEvent forPlayStart(PlaybackSessionEventArgs args) {
        return PlaybackSessionEvent.create(EVENT_KIND_PLAY_START, args).build();
    }

    public static PlaybackSessionEvent forPlay(PlaybackSessionEventArgs args) {
        return PlaybackSessionEvent.create(EVENT_KIND_PLAY, args).playId(Optional.of(args.getPlayId())).build();
    }

    @NonNull
    public static PlaybackSessionEvent forStop(PlaybackSessionEvent lastPlayEvent, StopReason stopReason, PlaybackSessionEventArgs args) {
        final Builder builder = create(EVENT_KIND_STOP, args);
        builder.playId(Optional.of(args.getPlayId()));
        builder.listenTime(Optional.of(builder.build().timestamp() - lastPlayEvent.getTimestamp()));
        builder.stopReason(Optional.of(stopReason));
        return builder.build();
    }

    public static PlaybackSessionEvent forCheckpoint(PlaybackSessionEventArgs args) {
        return PlaybackSessionEvent.create(EVENT_KIND_CHECKPOINT, args).playId(Optional.of(args.getPlayId())).build();
    }

    // Regular track
    private static PlaybackSessionEvent.Builder create(Kind eventKind, PlaybackSessionEventArgs args) {
        final EntityMetadata entityMetadata = EntityMetadata.from(args.getTrackData());
        return new AutoValue_PlaybackSessionEvent.Builder().id(defaultId())
                                                           .timestamp(defaultTimestamp())
                                                           .referringEvent(Optional.absent())
                                                           .playId(Optional.absent())
                                                           .kind(eventKind)
                                                           .isOfflineTrack(args.isOfflineTrack())
                                                           .marketablePlay(args.isMarketablePlay())
                                                           .clientEventId(args.getClientEventId())
                                                           .trackUrn(args.getTrackData().getUrn())
                                                           .creatorUrn(args.getTrackData().creatorUrn())
                                                           .creatorName(args.getTrackData().creatorName())
                                                           .entityMetadata(entityMetadata)
                                                           .monetizationModel(args.getTrackData().monetizationModel())
                                                           .protocol(args.getProtocol())
                                                           .policy(Optional.fromNullable(args.getTrackData().policy()))
                                                           .playerType(args.getPlayerType())
                                                           .trackSourceInfo(args.getTrackSourceInfo())
                                                           .progress(args.getProgress())
                                                           .duration(Durations.getTrackPlayDuration(args.getTrackData()))
                                                           .listenTime(Optional.absent())
                                                           .stopReason(Optional.absent())
                                                           .adUrn(Optional.absent())
                                                           .monetizationType(Optional.absent())
                                                           .promoterUrn(Optional.absent())
                                                           .shouldReportAdStart(Optional.absent())
                                                           .promotedPlayUrls(Optional.absent());
    }

    // Promoted track
    public static PlaybackSessionEvent copyWithPromotedTrack(PlaybackSessionEvent playbackSessionEvent, PromotedSourceInfo promotedSource) {
        final AutoValue_PlaybackSessionEvent.Builder builder = new AutoValue_PlaybackSessionEvent.Builder(playbackSessionEvent);
        builder.adUrn(Optional.of(promotedSource.getAdUrn()));
        builder.monetizationType(Optional.of(MONETIZATION_PROMOTED));
        builder.promoterUrn(promotedSource.getPromoterUrn());
        builder.shouldReportAdStart(Optional.of(!promotedSource.isPlaybackStarted()));
        builder.promotedPlayUrls(Optional.of(promotedSource.getTrackingUrls()));
        return builder.build();
    }

    public boolean isPlayStartEvent() {
        return EVENT_KIND_PLAY_START.equals(kind());
    }

    public boolean isPlayEvent() {
        return EVENT_KIND_PLAY.equals(kind());
    }

    public boolean isPlayOrPlayStartEvent() {
        return isPlayStartEvent() || isPlayEvent();
    }

    public boolean isCheckpointEvent() {
        return EVENT_KIND_CHECKPOINT.equals(kind());
    }

    public boolean isStopEvent() {
        return EVENT_KIND_STOP.equals(kind());
    }

    public boolean isPromotedTrack() {
        return isMonetizationType(MONETIZATION_PROMOTED);
    }

    @Override
    public PlaybackSessionEvent putReferringEvent(ReferringEvent referringEvent) {
        return null;
    }

    private boolean isMonetizationType(String type) {
        return monetizationType().isPresent() && monetizationType().get().equals(type);
    }

    public boolean isPlayAdShouldReportAdStart() {
        return kind().equals(EVENT_KIND_PLAY) && shouldReportAdStart().isPresent() && shouldReportAdStart().get();
    }


    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder timestamp(long timestamp);

        public abstract Builder referringEvent(Optional<ReferringEvent> referringEvent);

        public abstract Builder kind(Kind kind);

        public abstract Builder trackUrn(Urn trackUrn);

        public abstract Builder creatorUrn(Urn creatorUrn);

        public abstract Builder creatorName(String creatorName);

        public abstract Builder playableTitle(String playableTitle);

        public abstract Builder playableUrn(Urn playableUrn);

        public abstract Builder playableType(String playableType);

        public abstract Builder duration(long duration);

        public abstract Builder progress(long progress);

        public abstract Builder isOfflineTrack(boolean isOfflineTrack);

        public abstract Builder marketablePlay(boolean marketablePlay);

        public abstract Builder clientEventId(String clientEventId);

        public abstract Builder playId(Optional<String> playId);

        public abstract Builder monetizationModel(String monetizationModel);

        public abstract Builder protocol(String protocol);

        public abstract Builder policy(Optional<String> policy);

        public abstract Builder playerType(String playerType);

        public abstract Builder stopReason(Optional<StopReason> stopReason);

        public abstract Builder listenTime(Optional<Long> listenTime);

        public abstract Builder trackSourceInfo(TrackSourceInfo trackSourceInfo);

        public abstract Builder adUrn(Optional<String> adUrn);

        public abstract Builder monetizationType(Optional<String> monetizationType);

        public abstract Builder promoterUrn(Optional<Urn> promoterUrn);

        public abstract Builder shouldReportAdStart(Optional<Boolean> shouldReportAdStart);

        public abstract Builder promotedPlayUrls(Optional<List<String>> promotedPlayUrls);

        Builder entityMetadata(EntityMetadata entityMetadata) {
            creatorName(entityMetadata.creatorName);
            creatorUrn(entityMetadata.creatorUrn);
            playableTitle(entityMetadata.playableTitle);
            playableUrn(entityMetadata.playableUrn);
            playableType(entityMetadata.getPlayableType());
            return this;
        }

        public abstract PlaybackSessionEvent build();
    }
}
