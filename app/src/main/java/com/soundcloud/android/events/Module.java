package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Module {
    public static final String STREAM = "stream";

    public static Module create(String name, String resource) {
        return new AutoValue_Module(name, resource);
    }

    public abstract String getName();

    public abstract String getResource();
}
