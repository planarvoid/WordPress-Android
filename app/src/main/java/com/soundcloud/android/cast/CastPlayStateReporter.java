package com.soundcloud.android.cast;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.AudioPlaybackItem;
import com.soundcloud.android.playback.PlayStatePublisher;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.PlaybackType;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;

public class CastPlayStateReporter {

    private final PlayStatePublisher playStatePublisher;
    private final DateProvider dateProvider;
    private Optional<Listener> listener = Optional.absent();

    @Inject
    public CastPlayStateReporter(PlayStatePublisher playStatePublisher, CurrentDateProvider dateProvider) {
        this.playStatePublisher = playStatePublisher;
        this.dateProvider = dateProvider;
    }

    private void reportStateChange(PlaybackStateTransition stateTransition) {
        PlaybackProgress playbackProgress = stateTransition.getProgress();
        final AudioPlaybackItem playbackItem = AudioPlaybackItem.create(stateTransition.getUrn(),
                                                                        playbackProgress.getPosition(),
                                                                        playbackProgress.getDuration(),
                                                                        PlaybackType.AUDIO_DEFAULT);
        playStatePublisher.publish(stateTransition, playbackItem, false);

        if (listener.isPresent()) {
            listener.get().onStateChangePublished(stateTransition);
        }
    }

    public void setListener(Listener listener) {
        this.listener = Optional.fromNullable(listener);
    }

    private PlaybackStateTransition createStateTransition(PlaybackState state, PlayStateReason reason, Urn initialTrackUrnCandidate) {
        return getStateTransition(state, reason, initialTrackUrnCandidate, 0, 0);
    }

    private PlaybackStateTransition getStateTransition(PlaybackState state, PlayStateReason reason, Urn urn, long progress, long duration) {
        return new PlaybackStateTransition(state, reason, urn, progress, duration, dateProvider);
    }

    public void reportDisconnection(Urn urn, long progress, long duration) {
        reportIdle(PlayStateReason.CAST_DISCONNECTED, urn, progress, duration);
    }

    public void reportPlaying(Urn urn, long progress, long duration) {
        reportStateChange(getStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, urn, progress, duration));
    }

    public void reportPlayingReset(Urn resetedUrn) {
        reportStateChange(createStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, resetedUrn));
    }

    public void reportPlayingError(Urn urn) {
        reportStateChange(createStateTransition(PlaybackState.IDLE, PlayStateReason.ERROR_FAILED, urn));
    }

    public void reportPaused(Urn urn, long progress, long duration) {
        // we have to suppress pause events while we should be playing.
        // The receiver sends thes back often, as in when the track first loads, even if autoplay is true
        reportIdle(PlayStateReason.NONE, urn, progress, duration);
    }

    public void reportBuffering(Urn urn, long progress, long duration) {
        reportStateChange(getStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, urn, progress, duration));
    }

    public void reportIdle(PlayStateReason reason, Urn urn, long progress, long duration) {
        reportStateChange(getStateTransition(PlaybackState.IDLE, reason, urn, progress, duration));
    }

    public interface Listener {
        void onStateChangePublished(PlaybackStateTransition transition);
    }
}
