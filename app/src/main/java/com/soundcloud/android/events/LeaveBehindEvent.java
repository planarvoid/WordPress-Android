package com.soundcloud.android.events;

public class LeaveBehindEvent {

    public static final int SHOWN = 0;
    public static final int HIDDEN = 1;

    private final int kind;

    public static LeaveBehindEvent shown() {
        return new LeaveBehindEvent(SHOWN);
    }
    public static LeaveBehindEvent hidden() {
        return new LeaveBehindEvent(HIDDEN);
    }


    public LeaveBehindEvent(int kind) {
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return "LeaveBehindEvent: " + kind;
    }

}
