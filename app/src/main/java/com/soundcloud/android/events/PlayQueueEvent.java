package com.soundcloud.android.events;

import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import rx.functions.Func1;

public class PlayQueueEvent {

    public static final int NEW_QUEUE = 0;
    public static final int TRACK_CHANGE = 1;
    public static final int QUEUE_UPDATE = 2;

    private final int kind;
    private final TrackUrn currentTrackUrn;

    public static final Func1<PlayQueueEvent, Boolean> TRACK_HAS_CHANGED_FILTER = new Func1<PlayQueueEvent, Boolean>() {
        @Override
        public Boolean call(PlayQueueEvent playQueueEvent) {
            return playQueueEvent.kind == NEW_QUEUE || playQueueEvent.kind == TRACK_CHANGE;
        }
    };

    public PlayQueueEvent(int kind, TrackUrn currentTrackUrn) {
        this.kind = kind;
        this.currentTrackUrn = currentTrackUrn;
    }

    public int getKind() {
        return kind;
    }

    public static PlayQueueEvent fromNewQueue(TrackUrn currentTrackUrn) {
        return new PlayQueueEvent(NEW_QUEUE, currentTrackUrn);
    }

    @Deprecated
    public static PlayQueueEvent fromNewQueue(long currentTrackId) {
        return fromNewQueue(Urn.forTrack(currentTrackId));
    }

    public static PlayQueueEvent fromTrackChange(TrackUrn currentTrackUrn) {
        return new PlayQueueEvent(TRACK_CHANGE, currentTrackUrn);
    }

    @Deprecated
    public static PlayQueueEvent fromTrackChange(long currentTrackId) {
        return fromTrackChange(Urn.forTrack(currentTrackId));
    }

    public static PlayQueueEvent fromQueueUpdate(TrackUrn currentTrackUrn) {
        return new PlayQueueEvent(QUEUE_UPDATE, currentTrackUrn);
    }

    public TrackUrn getCurrentTrackUrn() {
        return currentTrackUrn;
    }

    public boolean isContentChange() {
        return kind == NEW_QUEUE || kind == QUEUE_UPDATE;
    }

}
