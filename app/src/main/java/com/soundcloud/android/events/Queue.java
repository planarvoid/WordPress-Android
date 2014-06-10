package com.soundcloud.android.events;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public final class Queue<T> {

    private static int runningId = 0;

    @NotNull
    public final String name;
    @NotNull
    public final Class<T> eventType;
    @Nullable
    T defaultEvent;

    final int id;

    Queue(@NotNull String name, @NotNull Class<T> eventType) {
        this.name = name;
        this.eventType = eventType;
        this.id = runningId++;
    }

    Queue(String name, Class<T> eventType, @Nullable T defaultEvent) {
        this(name, eventType);
        this.defaultEvent = defaultEvent;
    }

    public static <T> Queue<T> create(String name, Class<T> eventType, T defaultEvent) {
        return new Queue<T>(name, eventType, defaultEvent);
    }

    public static <T> Queue<T> create(String name, Class<T> eventType) {
        return new Queue<T>(name, eventType);
    }

    public static <T> Queue<T> create(Class<T> eventType, T defaultEvent) {
        return new Queue<T>(eventType.getSimpleName(), eventType, defaultEvent);
    }

    public static <T> Queue<T> create(Class<T> eventType) {
        return new Queue<T>(eventType.getSimpleName() + "Queue", eventType);
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public boolean equals(Object that) {
        return (that != null && that instanceof Queue && ((Queue) that).id == this.id);
    }

    @Override
    public String toString() {
        return this.name + "[" + this.eventType.getCanonicalName() + "]";
    }
}
