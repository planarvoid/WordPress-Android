package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.objects.MoreObjects;

import android.util.SparseArray;

public class PlaybackStateTransition {
    public static final int EXTRA_PLAYBACK_PROTOCOL = 0;
    public static final int EXTRA_PLAYER_TYPE = 1;
    public static final int EXTRA_CONNECTION_TYPE = 2;
    public static final int EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE = 3;
    public static final int EXTRA_URI = 4;

    private final PlaybackState newState;
    private final PlayStateReason reason;
    private final PlaybackProgress progress;
    private final Urn itemUrn;
    private final String format;
    private final int bitrate;

    // used to pass various additional meta data with the event, often for tracking/analytics
    private final SparseArray<String> extraAttributes = new SparseArray<>(2);

    public static final PlaybackStateTransition DEFAULT = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                                      PlayStateReason.NONE,
                                                                                      Urn.NOT_SET, 0, 0);

    public PlaybackStateTransition(PlaybackState newState,
                                   PlayStateReason reason,
                                   Urn itemUrn,
                                   long currentProgress,
                                   long duration) {
        this(newState, reason, itemUrn, currentProgress, duration, new CurrentDateProvider());
    }

    public PlaybackStateTransition(PlaybackState newState,
                                   PlayStateReason reason,
                                   Urn itemUrn,
                                   long currentProgress,
                                   long duration,
                                   DateProvider dateProvider) {
        this(newState,
             reason,
             itemUrn,
             currentProgress,
             duration,
             PlaybackConstants.MediaType.UNKNOWN,
             0,
             dateProvider);
    }

    public PlaybackStateTransition(PlaybackState newState,
                                   PlayStateReason reason,
                                   Urn itemUrn,
                                   long currentProgress,
                                   long duration,
                                   String format,
                                   int bitrate,
                                   DateProvider dateProvider) {
        this.newState = newState;
        this.reason = reason;
        this.itemUrn = itemUrn;
        this.format = format;
        this.bitrate = bitrate;
        this.progress = new PlaybackProgress(currentProgress, duration, dateProvider, itemUrn);
    }

    public Urn getUrn() {
        return itemUrn;
    }

    public boolean isForUrn(Urn urn) {
        return getUrn().equals(urn);
    }

    public PlaybackState getNewState() {
        return newState;
    }

    public PlayStateReason getReason() {
        return reason;
    }

    public PlaybackProgress getProgress() {
        return progress;
    }

    public String getFormat() {
        return format;
    }

    public int getBitrate() {
        return bitrate;
    }

    boolean isPlaying() {
        return newState.isPlaying();
    }

    public boolean isPlayerPlaying() {
        return newState.isPlayerPlaying();
    }

    public boolean isPlayerIdle() {
        return newState == PlaybackState.IDLE;
    }

    public boolean isBuffering() {
        return newState.isBuffering();
    }

    public boolean playbackHasStopped() {
        return PlayStateReason.PLAYBACK_STOPPED.contains(reason);
    }

    public boolean wasError() {
        return PlayStateReason.ERRORS.contains(reason);
    }

    public boolean wasGeneralFailure() {
        return reason == PlayStateReason.ERROR_FAILED;
    }

    public boolean playbackEnded() {
        return newState == PlaybackState.IDLE && reason == PlayStateReason.PLAYBACK_COMPLETE;
    }

    public boolean isPaused() {
        return newState == PlaybackState.IDLE && reason == PlayStateReason.NONE;
    }

    public boolean isCastDisconnection() {
        return newState == PlaybackState.IDLE && reason == PlayStateReason.CAST_DISCONNECTED;
    }

    public String getExtraAttribute(int key) {
        return extraAttributes.get(key);
    }

    public PlaybackStateTransition addExtraAttribute(int key, String value) {
        this.extraAttributes.put(key, value);
        return this;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            PlaybackStateTransition that = (PlaybackStateTransition) o;
            return MoreObjects.equal(newState, that.newState)
                    && MoreObjects.equal(reason, that.reason)
                    && MoreObjects.equal(progress, that.progress)
                    && MoreObjects.equal(itemUrn, that.itemUrn);
        }
    }

    @Override
    public final int hashCode() {
        return MoreObjects.hashCode(newState, reason, progress, itemUrn);
    }

    @Override
    public String toString() {
        return "PlaybackStateTransition{" +
                "newState=" + newState +
                ", reason=" + reason +
                ", currentProgress=" + progress.getPosition() +
                ", duration=" + progress.getDuration() +
                ", itemUrn=" + itemUrn +
                ", extraAttributes=" + extraAttributes +
                '}';
    }
}
