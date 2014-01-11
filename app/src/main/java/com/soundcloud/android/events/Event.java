package com.soundcloud.android.events;

import java.util.Map;

public abstract class Event {

    protected int mKind;
    protected Map<String, String> mAttributes;

    public Event(int kind, Map<String, String> attributes) {
        this.mKind = kind;
        this.mAttributes = attributes;
    }

    public int getKind() {
        return mKind;
    }

    public Map<String, String> getAttributes() {
        return mAttributes;
    }


}
