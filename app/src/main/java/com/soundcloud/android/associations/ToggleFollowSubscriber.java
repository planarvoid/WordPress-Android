package com.soundcloud.android.associations;

import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.widget.ToggleButton;

import java.lang.ref.WeakReference;

public final class ToggleFollowSubscriber extends DefaultSubscriber<Boolean> {

    private final WeakReference<ToggleButton> toggleButtonRef;

    public ToggleFollowSubscriber(ToggleButton toggleButton) {
        this.toggleButtonRef = new WeakReference<>(toggleButton);
    }

    @Override
    public void onNext(Boolean isFollowing) {
        ToggleButton button = toggleButtonRef.get();
        if (button != null) {
            button.setChecked(isFollowing);
        }
    }
}
