package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class EmptyViewItem extends DiscoveryItem {

    public static DiscoveryItem fromThrowable(Throwable throwable) {
        return new AutoValue_EmptyViewItem(throwable);
    }

    EmptyViewItem() {
        super(Kind.Empty);
    }

    public abstract Throwable getThrowable();

}
