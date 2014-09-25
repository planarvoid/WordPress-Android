package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.util.SparseArray;

import java.util.List;

@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class PlaybackSessionEvent {

    public static final int STOP_REASON_PAUSE = 0;
    public static final int STOP_REASON_BUFFERING = 1;
    public static final int STOP_REASON_SKIP = 2;
    public static final int STOP_REASON_TRACK_FINISHED = 3;
    public static final int STOP_REASON_END_OF_QUEUE = 4;
    public static final int STOP_REASON_NEW_QUEUE = 5;
    public static final int STOP_REASON_ERROR = 6;

    private static final int EXTRA_AD_URN = 0;
    private static final int EXTRA_MONETIZED_URN = 1;
    private static final int EXTRA_TRACK_POLICY = 2;
    private static final int EXTRA_AD_ARTWORK = 3;
    private static final int EXTRA_PLAYBACK_PROTOCOL = 4;
    private static final int EXTRA_AUDIO_AD_IMPRESSION_URLS = 5;
    private static final int EXTRA_AUDIO_AD_COMPANION_IMPRESSION_URLS = 6;
    private static final int EXTRA_AUDIO_AD_FINISH_URLS = 7;

    private static final int EVENT_KIND_PLAY = 0;
    private static final int EVENT_KIND_STOP = 1;

    private final int kind, duration;
    private final Urn trackUrn;
    private final Urn userUrn;
    private final String protocol;

    private final TrackSourceInfo trackSourceInfo;
    private final long timeStamp, progress;
    private long listenTime;
    private int stopReason;

    // extra meta data that might not always be present goes here
    private final SparseArray<Object> extraAttributes = new SparseArray<Object>();

    public static PlaybackSessionEvent forPlay(@NotNull PropertySet trackData, @NotNull Urn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo, long progress, long timestamp) {
        return new PlaybackSessionEvent(EVENT_KIND_PLAY, trackData, userUrn, protocol, trackSourceInfo, progress, timestamp);
    }

    public static PlaybackSessionEvent forPlay(@NotNull PropertySet trackData, @NotNull Urn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo, long progress) {
        return forPlay(trackData, userUrn, protocol, trackSourceInfo, progress, System.currentTimeMillis());
    }

    public static PlaybackSessionEvent forStop(@NotNull PropertySet trackData, @NotNull Urn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo, PlaybackSessionEvent lastPlayEvent,
                                               int stopReason, long progress, long timestamp) {
        final PlaybackSessionEvent playbackSessionEvent =
                new PlaybackSessionEvent(EVENT_KIND_STOP, trackData, userUrn, protocol, trackSourceInfo, progress, timestamp);
        playbackSessionEvent.setListenTime(playbackSessionEvent.timeStamp - lastPlayEvent.getTimeStamp());
        playbackSessionEvent.setStopReason(stopReason);
        return playbackSessionEvent;
    }

    public static PlaybackSessionEvent forStop(@NotNull PropertySet trackData, @NotNull Urn userUrn,
                                               String protocol, TrackSourceInfo trackSourceInfo, PlaybackSessionEvent lastPlayEvent,
                                               int stopReason, long progress) {
        return forStop(trackData, userUrn, protocol, trackSourceInfo, lastPlayEvent, stopReason, progress, System.currentTimeMillis());
    }

    // Use this constructor for an ordinary audio playback event
    private PlaybackSessionEvent(int eventKind, PropertySet track, Urn userUrn,
                                 String protocol, TrackSourceInfo trackSourceInfo, long progress, long timestamp) {
        this.trackUrn = track.get(TrackProperty.URN);
        this.duration = track.get(PlayableProperty.DURATION);
        this.kind = eventKind;
        this.userUrn = userUrn;
        this.protocol = protocol;
        this.trackSourceInfo = trackSourceInfo;
        this.progress = progress;
        this.timeStamp = timestamp;
        if (track.contains(TrackProperty.POLICY)) {
            this.extraAttributes.put(EXTRA_TRACK_POLICY, track.get(TrackProperty.POLICY));
        }
    }

    // Use this constructor for an audio ad playback event
    public PlaybackSessionEvent withAudioAd(PropertySet audioAd) {
        this.extraAttributes.put(EXTRA_AD_URN, audioAd.get(AdProperty.AD_URN));
        this.extraAttributes.put(EXTRA_MONETIZED_URN, audioAd.get(AdProperty.MONETIZABLE_TRACK_URN).toString());
        this.extraAttributes.put(EXTRA_AD_ARTWORK, audioAd.get(AdProperty.ARTWORK).toString());
        this.extraAttributes.put(EXTRA_AUDIO_AD_IMPRESSION_URLS, audioAd.get(AdProperty.AUDIO_AD_IMPRESSION_URLS));
        this.extraAttributes.put(EXTRA_AUDIO_AD_COMPANION_IMPRESSION_URLS, audioAd.get(AdProperty.AUDIO_AD_COMPANION_DISPLAY_IMPRESSION_URLS));
        this.extraAttributes.put(EXTRA_AUDIO_AD_FINISH_URLS, audioAd.get(AdProperty.AUDIO_AD_FINISH_URLS));
        this.extraAttributes.put(EXTRA_PLAYBACK_PROTOCOL, protocol);
        return this;
    }

    public int getKind() {
        return kind;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    @Nullable
    public String getTrackPolicy() {
        return (String) extraAttributes.get(EXTRA_TRACK_POLICY);
    }

    public boolean isPlayEvent() {
        return kind == EVENT_KIND_PLAY;
    }

    public boolean isStopEvent() {
        return !isPlayEvent();
    }

    public Urn getUserUrn() {
        return userUrn;
    }

    public TrackSourceInfo getTrackSourceInfo() {
        return trackSourceInfo;
    }

    public boolean isPlayingOwnPlaylist() {
        return trackSourceInfo.getPlaylistOwnerId() == userUrn.getNumericId();
    }

    public int getDuration() {
        return duration;
    }

    public long getProgress() {
        return progress;
    }

    public String getProtocol() {
        return protocol;
    }

    public List<String> getAudioAdImpressionUrls() {
        return (List<String>) extraAttributes.get(EXTRA_AUDIO_AD_IMPRESSION_URLS);
    }

    public List<String> getAudioAdFinishUrls() {
        return (List<String>) extraAttributes.get(EXTRA_AUDIO_AD_FINISH_URLS);
    }

    public List<String> getAudioAdCompanionImpressionUrls() {
        return (List<String>) extraAttributes.get(EXTRA_AUDIO_AD_COMPANION_IMPRESSION_URLS);
    }

    private void setListenTime(long listenTime) {
        this.listenTime = listenTime;
    }

    public int getStopReason() {
        return stopReason;
    }

    private void setStopReason(int stopReason) {
        this.stopReason = stopReason;
    }

    public String getAudioAdUrn() {
        return (String) extraAttributes.get(EXTRA_AD_URN);
    }

    public String getAudioAdMonetizedUrn() {
        return (String) extraAttributes.get(EXTRA_MONETIZED_URN);
    }

    public String getAudioAdArtworkUrl() {
        return (String) extraAttributes.get(EXTRA_AD_ARTWORK);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(PlaybackSessionEvent.class)
                .add("TrackUrn", trackUrn.toString())
                .add("Duration", duration)
                .add("Event", kind)
                .add("UserUrn", userUrn)
                .add("TrackSourceInfo", trackSourceInfo)
                .add("TimeStamp", timeStamp).toString();
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public long getListenTime() {
        return listenTime;
    }

    public boolean isAd() {
        return extraAttributes.indexOfKey(EXTRA_AD_URN) != -1;
    }

    public boolean isFirstPlay() {
        return isPlayEvent() && progress == 0L;
    }

    public boolean hasTrackFinished() {
        return isStopEvent() && getStopReason() == PlaybackSessionEvent.STOP_REASON_TRACK_FINISHED;
    }
}
