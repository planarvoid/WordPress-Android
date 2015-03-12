package com.soundcloud.android.events;

import com.google.common.base.Objects;

public final class OfflineContentEvent {

    public static final int IDLE = 0;
    public static final int START = 1;
    public static final int STOP = 2;

    private final int kind;

    public OfflineContentEvent(int kind) {
        this.kind = kind;
    }

    public static OfflineContentEvent idle() {
        return new OfflineContentEvent(IDLE);
    }

    public static OfflineContentEvent start() {
        return new OfflineContentEvent(START);
    }

    public static OfflineContentEvent stop() {
        return new OfflineContentEvent(STOP);
    }

    public int getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("kind", getKindName()).toString();
    }

    private String getKindName() {
        switch (getKind()){
            case IDLE: return "IDLE";
            case START: return "START";
            case STOP: return "STOP";
            default: return "unknown";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return kind == ((OfflineContentEvent) o).getKind();
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(kind);
    }

}
