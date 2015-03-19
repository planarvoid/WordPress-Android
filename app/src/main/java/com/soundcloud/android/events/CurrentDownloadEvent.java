package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;

public final class CurrentDownloadEvent {

    public static final int IDLE = 0;
    public static final int START = 1;
    public static final int STOP = 2;

    private final int kind;
    private final Urn trackUrn;

    public CurrentDownloadEvent(int kind, Urn trackUrn) {
        this.kind = kind;
        this.trackUrn = trackUrn;
    }

    public static CurrentDownloadEvent start(Urn trackUrn) {
        return new CurrentDownloadEvent(START, trackUrn);
    }

    public static CurrentDownloadEvent stop(Urn trackUrn) {
        return new CurrentDownloadEvent(STOP, trackUrn);
    }

    public static CurrentDownloadEvent idle() {
        return new CurrentDownloadEvent(IDLE, Urn.NOT_SET);
    }

    public int getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("kind", getKindName()).toString();
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    public boolean wasStopped() {
        return kind == STOP || kind == IDLE;
    }

    public boolean wasStarted() {
        return kind == START;
    }

    private String getKindName() {
        switch (getKind()){
            case START: return "START";
            case STOP: return "STOP";
            default: return "unknown";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CurrentDownloadEvent that = (CurrentDownloadEvent) o;

        return kind == that.kind && trackUrn.equals(that.trackUrn);
    }

    @Override
    public int hashCode() {
        int result = kind;
        result = 31 * result + trackUrn.hashCode();
        return result;
    }
}
