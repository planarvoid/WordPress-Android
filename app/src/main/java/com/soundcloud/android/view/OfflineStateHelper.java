package com.soundcloud.android.view;

import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.java.optional.Optional;

import android.view.View;

import java.util.concurrent.TimeUnit;

final class OfflineStateHelper {

    private static final long UPDATE_DELAY = TimeUnit.MILLISECONDS.toMillis(200);

    private final View view;
    private final Callback callback;

    private Optional<Runnable> delayedState = Optional.absent();

    interface Callback {
        void onStateTransition(OfflineState state);
    }

    private OfflineStateHelper(View view, Callback callback) {
        this.view = view;
        this.callback = callback;
    }

    static OfflineStateHelper create(View view, Callback callback) {
        return new OfflineStateHelper(view, callback);
    }

    public void update(OfflineState oldState, OfflineState newState) {
        delayedState.ifPresent(runnable -> {
            view.removeCallbacks(runnable);
            delayedState = Optional.absent();
        });
        if (oldState == OfflineState.DOWNLOADING && newState == OfflineState.REQUESTED) {
            delayedState = Optional.of(() -> {
                callback.onStateTransition(newState);
                delayedState = Optional.absent();
            });
            view.postDelayed(delayedState.get(), UPDATE_DELAY);
        } else {
            callback.onStateTransition(newState);
        }
    }

}
