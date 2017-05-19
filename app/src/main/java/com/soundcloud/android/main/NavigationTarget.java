package com.soundcloud.android.main;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

import android.app.Activity;
import android.net.Uri;
import android.support.annotation.Nullable;

@AutoValue
public abstract class NavigationTarget {
    public abstract Activity activity();
    @Nullable
    public abstract String target();
    public abstract Optional<String> fallback();
    public abstract Screen screen();
    public abstract Optional<String> referrer();
    public abstract Builder toBuilder();

    public static Builder newBuilder() {
        return new AutoValue_NavigationTarget.Builder()
                .referrer(Optional.absent())
                .fallback(Optional.absent());
    }

    public static NavigationTarget forNavigation(Activity activity, @Nullable String target, Optional<String> fallback, Screen screen) {
        return newBuilder().activity(activity)
                           .target(target)
                           .fallback(fallback)
                           .screen(screen)
                           .build();
    }

    public static NavigationTarget forDeeplink(Activity activity, @Nullable String target, String referrer) {
        return newBuilder().activity(activity)
                           .target(target)
                           .screen(Screen.DEEPLINK)
                           .referrer(Optional.of(referrer))
                           .build();
    }

    public NavigationTarget withScreen(Screen screen) {
        return toBuilder().screen(screen).build();
    }

    public Uri targetUri() {
        return Uri.parse(target());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder activity(Activity activity);
        public abstract Builder target(@Nullable String target);
        public abstract Builder fallback(Optional<String> fallback);
        public abstract Builder screen(Screen screen);
        public abstract Builder referrer(Optional<String> referrer);
        public abstract NavigationTarget build();
    }
}
