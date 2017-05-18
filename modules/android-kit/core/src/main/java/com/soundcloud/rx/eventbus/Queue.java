package com.soundcloud.rx.eventbus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.functions.Action1;

/**
 * An event queue descriptor. The {@link EventBus} will turn this into a
 * an Rx {@link rx.subjects.Subject} according to the queue configuration.
 *
 * @param <T> the event type of this queue
 */
public final class Queue<T> {

    private static int runningId;

    @NotNull public final String name;
    @NotNull public final Class<T> eventType;

    final int id;
    final boolean replayLast;
    @Nullable final T defaultEvent;
    @Nullable final Action1<Throwable> onError;

    public static final class Builder<T> {
        private String name;
        private final Class<T> eventType;
        private boolean replayLast;
        private T defaultEvent;
        private Action1<Throwable> onError;

        Builder(Class<T> eventType) {
            this.eventType = eventType;
        }

        /**
         * @param name the name of the queue (optional)
         */
        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Whether the queue should replay the last event when subscribed to.
         */
        public Builder<T> replay() {
            this.replayLast = true;
            return this;
        }

        /**
         * Whether the queue should replay the last event when subscribed to, or the given
         * default event if there were no prior events.
         */
        public Builder<T> replay(T defaultEvent) {
            this.replayLast = true;
            this.defaultEvent = defaultEvent;
            return this;
        }

        /**
         * @param onError a custom error handler to process queue errors
         */
        public Builder<T> onError(Action1<Throwable> onError) {
            this.onError = onError;
            return this;
        }

        public Queue<T> get() {
            if (name == null) {
                name = eventType.getSimpleName() + "Queue";
            }
            return new Queue<>(name, eventType, replayLast, defaultEvent, onError);
        }
    }

    public static <T> Builder<T> of(Class<T> eventType) {
        return new Builder<>(eventType);
    }

    Queue(@NotNull String name, @NotNull Class<T> eventType, boolean replayLast, @Nullable T defaultEvent,
          @Nullable Action1<Throwable> onError) {
        this.name = name;
        this.eventType = eventType;
        this.replayLast = replayLast;
        this.defaultEvent = defaultEvent;
        this.onError = onError;
        this.id = runningId++;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public boolean equals(Object that) {
        return that != null && that instanceof Queue && ((Queue) that).id == this.id;
    }

    @Override
    public String toString() {
        return this.name + "[" + this.eventType.getCanonicalName() + "]";
    }
}
