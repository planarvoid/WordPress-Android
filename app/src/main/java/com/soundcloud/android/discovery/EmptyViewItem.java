package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class EmptyViewItem extends OldDiscoveryItem {

    public static OldDiscoveryItem fromThrowable(Throwable throwable) {
        return new AutoValue_EmptyViewItem(Kind.Empty, throwable);
    }

    public abstract Throwable getThrowable();

}
