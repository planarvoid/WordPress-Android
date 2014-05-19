package com.soundcloud.android.playback.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackUrn;

import android.content.Intent;

import java.util.EnumSet;

public interface Playa {

    public void play(Track track);
    public void play(Track track, long fromPos);
    public boolean resume();
    public void pause();
    public long seek(long ms, boolean performSeek);
    public long getProgress();
    public void setVolume(float v);
    public void stop();
    public void destroy();
    public void setListener(PlayaListener playaListener);
    // MediaPlayer specific. We can drop these when we drop mediaplayer, as they will be constant booleans in skippy
    public boolean isSeekable();
    public boolean isNotSeekablePastBuffer();

    public static class StateTransition {
        private PlayaState newState;
        private Reason reason;

        @VisibleForTesting
        private static final String DEBUG_EXTRA = "DEBUG_EXTRA";

        private String debugExtra;
        private TrackUrn trackUrn;

        public static final StateTransition DEFAULT = new StateTransition(PlayaState.IDLE, Reason.NONE);

        public StateTransition(PlayaState newState, Reason reason) {
            this.newState = newState;
            this.reason = reason;
        }

        public void setDebugExtra(String debugExtra){
            this.debugExtra = debugExtra;
        }

        @Deprecated
        public TrackUrn getTrackUrn() {
            return trackUrn;
        }

        @Deprecated
        public void setTrackUrn(TrackUrn trackUrn) {
            this.trackUrn = trackUrn;
        }

        public PlayaState getNewState() {
            return newState;
        }

        public Reason getReason() {
            return reason;
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

        public boolean isBuffering(){
            return newState.isBuffering();
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

        public String getDebugExtra() {
            return debugExtra;
        }

        public void addToIntent(Intent intent) {
            newState.addToIntent(intent);
            reason.addToIntent(intent);
            intent.putExtra(DEBUG_EXTRA, debugExtra);
        }

        public static StateTransition fromIntent(Intent intent) {
            final StateTransition stateTransition = new StateTransition(PlayaState.fromIntent(intent), Reason.fromIntent(intent));
            stateTransition.setDebugExtra(intent.getStringExtra(DEBUG_EXTRA));
            return stateTransition;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StateTransition that = (StateTransition) o;
            return Objects.equal(newState, that.newState) && Objects.equal(reason, that.reason)
                    && Objects.equal(debugExtra, that.debugExtra);
        }

        @Override
        public int hashCode() {
            int result = newState.hashCode();
            result = 31 * result + reason.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "StateTransition{" +
                    "newState=" + newState +
                    ", reason=" + reason +
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
