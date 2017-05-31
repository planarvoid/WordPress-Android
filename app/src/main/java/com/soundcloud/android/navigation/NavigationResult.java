package com.soundcloud.android.navigation;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import io.reactivex.functions.Action;

@AutoValue
public abstract class NavigationResult {
    public abstract NavigationTarget target();
    public abstract Action action();
    public abstract Optional<Urn> urn();

    public static NavigationResult create(NavigationTarget target, Action action) {
        return new AutoValue_NavigationResult(target, action, Optional.absent());
    }

    public static NavigationResult create(NavigationTarget target, Action action, Urn urn) {
        return new AutoValue_NavigationResult(target, action, Optional.of(urn));
    }
}
