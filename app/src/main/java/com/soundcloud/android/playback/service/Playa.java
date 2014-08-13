package com.soundcloud.android.playback.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;

import android.content.Intent;

import java.util.EnumSet;

// TODO, extract transitions/reason/error codes to their own classes
@SuppressWarnings({"PMD.ExcessivePublicCount"})
public interface Playa {

    void play(PropertySet track);
    void play(PropertySet track, long fromPos);
    void playUninterrupted(PropertySet track);
    boolean resume();
    void pause();
    long seek(long ms, boolean performSeek);
    long getProgress();
    void setVolume(float v);
    void stop();
    void destroy();
    void setListener(PlayaListener playaListener);
    // MediaPlayer specific. We can drop these when we drop mediaplayer, as they will be constant booleans in skippy
    boolean isSeekable();
    boolean isNotSeekablePastBuffer();

    static class StateTransition {
        private final PlayaState newState;
        private final Reason reason;
        private final PlaybackProgress progress;

        private static final String TRACK_URN_EXTRA = "TRACK_URN_EXTRA";
        private static final String PROGRESS_EXTRA = "PROGRESS_EXTRA";
        private static final String DURATION_EXTRA = "DURATION_EXTRA";

        private TrackUrn trackUrn;

        public static final StateTransition DEFAULT = new StateTransition(PlayaState.IDLE, Reason.NONE);

        @Deprecated
        public StateTransition(PlayaState newState, Reason reason) {
            this(newState, reason, TrackUrn.NOT_SET);
        }

        public StateTransition(PlayaState newState, Reason reason, TrackUrn trackUrn) {
            this(newState, reason, trackUrn, 0, 0);
        }

        public StateTransition(PlayaState newState, Reason reason, TrackUrn trackUrn, long currentProgress, long duration) {
            this.newState = newState;
            this.reason = reason;
            this.trackUrn = trackUrn;
            progress = new PlaybackProgress(currentProgress, duration);
        }

        public TrackUrn getTrackUrn() {
            return trackUrn;
        }

        public boolean isForTrack(TrackUrn trackUrn) {
            return this.trackUrn != null && this.trackUrn.equals(trackUrn);
        }

        public PlayaState getNewState() {
            return newState;
        }

        public Reason getReason() {
            return reason;
        }

        public PlaybackProgress getProgress() {
            return progress;
        }

        boolean isPlaying(){
            return newState.isPlaying();
        }

        public boolean playSessionIsActive(){
            return newState.isPlaying() || (newState == PlayaState.IDLE && reason == Reason.TRACK_COMPLETE);
        }

        public boolean isPlayerPlaying(){
            return newState.isPlayerPlaying();
        }

        public boolean isPlayerIdle(){
            return newState == PlayaState.IDLE;
        }

        public boolean isBuffering(){
            return newState.isBuffering();
        }

        public boolean isPlayQueueComplete(){
            return reason == Reason.PLAY_QUEUE_COMPLETE;
        }

        public boolean playbackHasStopped(){
            return Reason.PLAYBACK_STOPPED.contains(reason);
        }

        public boolean wasError(){
            return Reason.ERRORS.contains(reason);
        }

        public boolean trackEnded() {
            return newState == Playa.PlayaState.IDLE && reason == Reason.TRACK_COMPLETE;
        }

        public boolean isPaused() {
            return newState == PlayaState.IDLE && reason == Reason.NONE;
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
                return Objects.equal(newState, that.newState)
                        && Objects.equal(reason, that.reason)
                        && Objects.equal(progress, that.progress)
                        && Objects.equal(trackUrn, that.trackUrn);
            }
        }

        @Override
        public int hashCode() {
            int result = newState.hashCode();
            result = 31 * result + reason.hashCode();
            result = 31 * result + progress.hashCode();
            result = 31 * result + (trackUrn != null ? trackUrn.hashCode() : 0);
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
                    '}';
        }
    }

    /**
     * PLAYING : the internal player is currently playing
     * IDLE : this internal player is not playing, nor is there intent to play
     * BUFFERING : there is intent to play, but sound is not coming out of the speakers
     * Note : there is no state for buffering with no intent to play. We should just report that as IDLE
     */
    public enum PlayaState {
        BUFFERING, PLAYING, IDLE;

        @VisibleForTesting
        static final String PLAYER_STATE_EXTRA = "PLAYER_STATE_EXTRA";

        /** User Intent. e.g., should we show the play button or pause button **/
        public boolean isPlaying(){
            return this == PLAYING || this == BUFFERING;
        }

        public boolean isIdle() {
            return this == IDLE;
        }

        /** Actual playback state. Is there sound coming out of the speakers or not **/
        public boolean isPlayerPlaying() {
            return this == PLAYING;
        }

        public boolean isBuffering() {
            return this == BUFFERING;
        }

        private void addToIntent(Intent intent) {
            intent.putExtra(PLAYER_STATE_EXTRA, ordinal());
        }

        private static PlayaState fromIntent(Intent intent) {
            if (!intent.hasExtra(PLAYER_STATE_EXTRA)){
                throw new IllegalStateException("No state ordinal found in intent");
            }
            final int reasonOrdinal = intent.getIntExtra(PLAYER_STATE_EXTRA, -1);
            if (reasonOrdinal < 0 || reasonOrdinal >= values().length){
                throw new IllegalStateException("Ordinal of player transition state is out of bounds");
            }
            return values()[intent.getIntExtra(PLAYER_STATE_EXTRA, -1)];
        }
    }

    public enum Reason {
        NONE, TRACK_COMPLETE, PLAY_QUEUE_COMPLETE, ERROR_FAILED, ERROR_NOT_FOUND, ERROR_FORBIDDEN;

        public static final EnumSet<Reason> ERRORS =
                EnumSet.of(ERROR_FAILED, ERROR_NOT_FOUND, ERROR_FORBIDDEN);

        public static final EnumSet<Reason> PLAYBACK_STOPPED =
                EnumSet.of(TRACK_COMPLETE, ERROR_FAILED, ERROR_NOT_FOUND, ERROR_FORBIDDEN);

        @VisibleForTesting
        static final String PLAYER_REASON_EXTRA = "PLAYER_REASON_EXTRA";

        private void addToIntent(Intent intent) {
            intent.putExtra(PLAYER_REASON_EXTRA, ordinal());
        }

        private static Reason fromIntent(Intent intent) {
            if (!intent.hasExtra(PLAYER_REASON_EXTRA)){
                throw new IllegalStateException("No reason ordinal found in intent");
            }
            final int reasonOrdinal = intent.getIntExtra(PLAYER_REASON_EXTRA, -1);
            if (reasonOrdinal < 0 || reasonOrdinal >= values().length){
                throw new IllegalStateException("Ordinal of player transition reason is out of bounds");
            }
            return values()[reasonOrdinal];
        }
    }

    public interface PlayaListener {
        void onPlaystateChanged(StateTransition stateTransition);
        void onProgressEvent(long progress, long duration);
        // we might be able to get rid of this, if we just request focus before setting data source, however this is a change in behavior
        boolean requestAudioFocus();
    }

    public enum Error{
        FAILED,
        MEDIA_NOT_FOUND,
        FORBIDDEN
    }
}
