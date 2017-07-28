package com.soundcloud.android.navigation;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.customtabs.CustomTabsMetadata;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.java.optional.Optional;

import android.content.Intent;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class NavigationResult {
    public abstract boolean isSuccess();

    public abstract NavigationTarget target();

    public abstract Optional<Intent> intent();

    public abstract List<Intent> taskStack();

    public abstract Optional<Urn> urn();

    public abstract Optional<PlaybackResult> playbackResult();

    public abstract Optional<String> toastMessage();

    public abstract Optional<CustomTabsMetadata> customTabsMetadata();


    public NavigationResult withToast(String message) {
        return new AutoValue_NavigationResult(isSuccess(), target(), intent(), taskStack(), urn(), playbackResult(), Optional.of(message), Optional.absent());
    }

    public static NavigationResult error(NavigationTarget target) {
        return new AutoValue_NavigationResult(false, target, Optional.absent(), Collections.emptyList(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());
    }

    public static NavigationResult forChromeCustomTab(NavigationTarget target, CustomTabsMetadata customTabsMetadata) {
        return new AutoValue_NavigationResult(true, target, Optional.absent(), Collections.emptyList(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.of(customTabsMetadata));
    }

    public static NavigationResult create(NavigationTarget target, Intent intent) {
        return new AutoValue_NavigationResult(true, target, Optional.of(intent), Collections.emptyList(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());
    }

    public static NavigationResult create(NavigationTarget target, PlaybackResult playbackResult) {
        return new AutoValue_NavigationResult(true, target, Optional.absent(), Collections.emptyList(), Optional.absent(), Optional.of(playbackResult), Optional.absent(), Optional.absent());
    }

    public static NavigationResult create(NavigationTarget target, Intent intent, Urn urn) {
        return new AutoValue_NavigationResult(true, target, Optional.of(intent), Collections.emptyList(), Optional.of(urn), Optional.absent(), Optional.absent(), Optional.absent());
    }

    public static NavigationResult create(NavigationTarget target, Intent intent, List<Intent> taskStack) {
        return new AutoValue_NavigationResult(true, target, Optional.of(intent), taskStack, Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());
    }
}
