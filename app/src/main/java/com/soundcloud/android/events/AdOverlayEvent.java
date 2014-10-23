package com.soundcloud.android.events;

public class AdOverlayEvent {

    public static final int SHOWN = 0;
    public static final int HIDDEN = 1;

    private final int kind;

    public static AdOverlayEvent shown() {
        return new AdOverlayEvent(SHOWN);
    }
    public static AdOverlayEvent hidden() {
        return new AdOverlayEvent(HIDDEN);
    }


    public AdOverlayEvent(int kind) {
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return "AdOverlayEvent: " + kind;
    }

}
