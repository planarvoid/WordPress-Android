package com.soundcloud.android.introductoryoverlay;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.java.optional.Optional;

import android.graphics.drawable.Drawable;
import android.view.View;

@AutoValue
public abstract class IntroductoryOverlay {
    public static Builder builder() {
        return new AutoValue_IntroductoryOverlay.Builder()
                .icon(Optional.absent())
                .event(Optional.absent());
    }

    public abstract String overlayKey();
    public abstract View targetView();
    public abstract int title();
    public abstract int description();
    public abstract Optional<Drawable> icon();
    public abstract Optional<TrackingEvent> event();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder overlayKey(String overlayKey);
        public abstract Builder targetView(View targetView);
        public abstract Builder title(int title);
        public abstract Builder description(int description);
        public abstract Builder icon(Optional<Drawable> icon);
        public abstract Builder event(Optional<TrackingEvent> event);
        public abstract IntroductoryOverlay build();
    }

}
