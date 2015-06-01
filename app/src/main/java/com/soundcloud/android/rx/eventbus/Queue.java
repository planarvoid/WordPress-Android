package com.soundcloud.android.rx.eventbus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Queue<T> {

    private static int runningId;

    @NotNull public final String name;
    @NotNull public final Class<T> eventType;

    final int id;
    final boolean replayLast;
    @Nullable final T defaultEvent;

    public static final class Builder<T> {
        private String name;
        private final Class<T> eventType;
        private boolean replayLast;
        private T defaultEvent;

        Builder(Class<T> eventType) {
            this.eventType = eventType;
        }

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> replay() {
            this.replayLast = true;
            return this;
        }

        public Builder<T> replay(T defaultEvent) {
            this.replayLast = true;
            this.defaultEvent = defaultEvent;
            return this;
        }

        public Queue<T> get() {
            if (name == null) {
                name = eventType.getSimpleName() + "Queue";
            }
            return new Queue<>(name, eventType, replayLast, defaultEvent);
        }
    }

    public static <T> Builder<T> of(Class<T> eventType) {
        return new Builder<>(eventType);
    }

    Queue(@NotNull String name, @NotNull Class<T> eventType, boolean replayLast, @Nullable T defaultEvent) {
        this.name = name;
        this.eventType = eventType;
        this.replayLast = replayLast;
        this.defaultEvent = defaultEvent;
        this.id = runningId++;
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
