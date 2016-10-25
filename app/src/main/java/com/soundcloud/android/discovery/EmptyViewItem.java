package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class EmptyViewItem extends DiscoveryItem {

    public static DiscoveryItem fromThrowable(Throwable throwable) {
        return new AutoValue_EmptyViewItem(Kind.Empty, throwable);
    }

    public abstract Throwable getThrowable();

}
