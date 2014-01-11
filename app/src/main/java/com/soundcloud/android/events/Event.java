package com.soundcloud.android.events;

import com.google.common.base.Objects;

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

    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues().add("kind", mKind).add("attributes", mAttributes).toString();
    }
}
