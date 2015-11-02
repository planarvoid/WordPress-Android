package com.soundcloud.android.playback;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.util.SparseArray;

import java.util.EnumSet;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

// TODO, extract transitions/reason/error codes to their own classes
@SuppressWarnings({"PMD.ExcessivePublicCount"})
public interface Player {

    @Deprecated // remove this when we get rid of or simplify mediaplayer
    void play(Urn track);
    void play(Urn track, long fromPos);
    void playUninterrupted(Urn track);
    void playOffline(Urn track, long fromPos);
    void playVideo(VideoPlaybackItem videoPlaybackItem);
    void resume();
    void pause();
    long seek(long ms, boolean performSeek);
    long getProgress();
    void setVolume(float v);
    void stop();
    void stopForTrackTransition();
    void destroy();
    void setListener(PlayerListener playerListener);
    // MediaPlayer specific. We can drop these when we drop mediaplayer, as they will be constant booleans in skippy
    boolean isSeekable();

    class StateTransition {
        public static final int EXTRA_PLAYBACK_PROTOCOL = 0;
        public static final int EXTRA_PLAYER_TYPE = 1;
        public static final int EXTRA_CONNECTION_TYPE = 2;
        public static final int EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE = 3;
        public static final int EXTRA_URI = 4;

        private static final String TRACK_URN_EXTRA = "TRACK_URN_EXTRA";
        private static final String PROGRESS_EXTRA = "PROGRESS_EXTRA";
        private static final String DURATION_EXTRA = "DURATION_EXTRA";

        private final PlayerState newState;
        private final Reason reason;
        private final PlaybackProgress progress;
        private final Optional<Urn> trackUrn;
        private final Optional<String> videoAdUrn;

        // used to pass various additional meta data with the event, often for tracking/analytics
        private final SparseArray<String> extraAttributes = new SparseArray<>(2);

        public static final StateTransition DEFAULT = new StateTransition(PlayerState.IDLE, Reason.NONE, Urn.NOT_SET);

        public StateTransition(PlayerState newState, Reason reason, Urn trackUrn) {
            this(newState, reason, trackUrn, 0, 0);
        }

        public StateTransition(PlayerState newState, Reason reason, Urn trackUrn, long currentProgress, long duration) {
            this(newState, reason, trackUrn, currentProgress, duration, new CurrentDateProvider());
        }

        public StateTransition(PlayerState newState, Reason reason, String videoAdUrn,
                               long currentProgress, long duration, CurrentDateProvider dateProvider) {
            this(newState, reason, Optional.<Urn>absent(), Optional.of(videoAdUrn), currentProgress, duration, dateProvider);
        }

        public StateTransition(PlayerState newState, Reason reason, Urn trackUrn,
                               long currentProgress, long duration, CurrentDateProvider dateProvider) {
            this(newState, reason, Optional.of(trackUrn), Optional.<String>absent(), currentProgress, duration, dateProvider);
        }

        private StateTransition(PlayerState newState,
                                Reason reason,
                                Optional<Urn> trackUrn,
                                Optional<String> videoAdUrn,
                                long currentProgress,
                                long duration,
                                CurrentDateProvider dateProvider) {
            checkArgument(trackUrn.isPresent() ^ videoAdUrn.isPresent(), "State transition needs to have either a track or video ad URN");
            this.newState = newState;
            this.reason = reason;
            this.trackUrn = trackUrn;
            this.videoAdUrn = videoAdUrn;
            this.progress = new PlaybackProgress(currentProgress, duration, dateProvider);
        }

        public boolean isForTrack() {
            return this.trackUrn.isPresent();
        }

        public boolean isForTrack(Urn trackUrn) {
            return isForTrack() && getTrackUrn().equals(trackUrn);
        }

        public boolean isForVideo() {
            return this.videoAdUrn.isPresent();
        }

        public boolean isForPlaybackItem(PlaybackItem playbackItem) {
            return ((playbackItem.getPlaybackType() == PlaybackType.VIDEO && isForVideo()) ||
                    (playbackItem.getPlaybackType() != PlaybackType.VIDEO && isForTrack(playbackItem.getTrackUrn())));
        }

        public Urn getTrackUrn() {
            return trackUrn.get();
        }

        public String getVideoAdUrn() {
            return videoAdUrn.get();
        }

        public PlayerState getNewState() {
            return newState;
        }

        public Reason getReason() {
            return reason;
        }

        public PlaybackProgress getProgress() {
            return progress;
        }

        boolean isPlaying() {
            return newState.isPlaying();
        }

        public boolean playSessionIsActive() {
            return newState.isPlaying() || (newState == PlayerState.IDLE && reason == Reason.PLAYBACK_COMPLETE);
        }

        public boolean isPlayerPlaying() {
            return newState.isPlayerPlaying();
        }

        public boolean isPlayerIdle() {
            return newState == PlayerState.IDLE;
        }

        public boolean isBuffering() {
            return newState.isBuffering();
        }

        public boolean isPlayQueueComplete() {
            return reason == Reason.PLAY_QUEUE_COMPLETE;
        }

        public boolean playbackHasStopped() {
            return Reason.PLAYBACK_STOPPED.contains(reason);
        }

        public boolean wasError() {
            return Reason.ERRORS.contains(reason);
        }

        public boolean wasGeneralFailure() {
            return reason == Reason.ERROR_FAILED;
        }

        public boolean trackEnded() {
            return newState == PlayerState.IDLE && reason == Reason.PLAYBACK_COMPLETE;
        }

        public boolean isPaused() {
            return newState == PlayerState.IDLE && reason == Reason.NONE;
        }

        public String getExtraAttribute(int key) {
            return extraAttributes.get(key);
        }

        public void addExtraAttribute(int key, String value) {
            this.extraAttributes.put(key, value);
        }

        public void addToIntent(Intent intent) {
            newState.addToIntent(intent);
            reason.addToIntent(intent);
            intent.putExtra(TRACK_URN_EXTRA, getTrackUrn());
            intent.putExtra(PROGRESS_EXTRA, progress.getPosition());
            intent.putExtra(DURATION_EXTRA, progress.getDuration());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            } else {
                StateTransition that = (StateTransition) o;
                return MoreObjects.equal(newState, that.newState)
                        && MoreObjects.equal(reason, that.reason)
                        && MoreObjects.equal(progress, that.progress)
                        && MoreObjects.equal(trackUrn, that.trackUrn)
                        && MoreObjects.equal(videoAdUrn, that.videoAdUrn);
            }
        }

        @Override
        public int hashCode() {
            int result = newState.hashCode();
            result = 31 * result + reason.hashCode();
            result = 31 * result + progress.hashCode();
            result = 31 * result + trackUrn.hashCode();
            result = 31 * result + videoAdUrn.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "StateTransition{" +
                    "newState=" + newState +
                    ", reason=" + reason +
                    ", currentProgress=" + progress.getPosition() +
                    ", duration=" + progress.getDuration() +
                    ", trackUrn=" + trackUrn +
                    ", videoAdUrn=" + videoAdUrn +
                    ", extraAttributes=" + extraAttributes +
                    '}';
        }
    }

    /**
     * PLAYING : the internal player is currently playing
     * IDLE : this internal player is not playing, nor is there intent to play
     * BUFFERING : there is intent to play, but sound is not coming out of the speakers
     * Note : there is no state for buffering with no intent to play. We should just report that as IDLE
     */
    enum PlayerState {
        BUFFERING, PLAYING, IDLE;

        @VisibleForTesting
        static final String PLAYER_STATE_EXTRA = "PLAYER_STATE_EXTRA";

        /**
         * User Intent. e.g., should we show the play button or pause button *
         */
        public boolean isPlaying() {
            return this == PLAYING || this == BUFFERING;
        }

        public boolean isIdle() {
            return this == IDLE;
        }

        /**
         * Actual playback state. Is there sound coming out of the speakers or not *
         */
        public boolean isPlayerPlaying() {
            return this == PLAYING;
        }

        public boolean isBuffering() {
            return this == BUFFERING;
        }

        private void addToIntent(Intent intent) {
            intent.putExtra(PLAYER_STATE_EXTRA, ordinal());
        }
    }

    enum Reason {
        NONE, PLAYBACK_COMPLETE, PLAY_QUEUE_COMPLETE, ERROR_FAILED, ERROR_NOT_FOUND, ERROR_FORBIDDEN;

        public static final EnumSet<Reason> ERRORS =
                EnumSet.of(ERROR_FAILED, ERROR_NOT_FOUND, ERROR_FORBIDDEN);

        public static final EnumSet<Reason> PLAYBACK_STOPPED =
                EnumSet.of(PLAYBACK_COMPLETE, ERROR_FAILED, ERROR_NOT_FOUND, ERROR_FORBIDDEN);

        @VisibleForTesting
        static final String PLAYER_REASON_EXTRA = "PLAYER_REASON_EXTRA";

        private void addToIntent(Intent intent) {
            intent.putExtra(PLAYER_REASON_EXTRA, ordinal());
        }
    }

    interface PlayerListener {
        void onPlaystateChanged(StateTransition stateTransition);
        void onProgressEvent(long progress, long duration);
        // we might be able to get rid of this, if we just request focus before setting data source, however this is a change in behavior
        boolean requestAudioFocus();
    }

    enum Error {
        FAILED,
        MEDIA_NOT_FOUND,
        FORBIDDEN
    }
}
