package com.soundcloud.android.discovery;

import com.soundcloud.java.optional.Optional;

class EmptyViewItem extends DiscoveryItem {
    public final Optional<Throwable> throwable;

    EmptyViewItem(Throwable throwable) {
        super(Kind.Empty);
        this.throwable = Optional.of(throwable);
    }

    public EmptyViewItem() {
        super(Kind.Empty);
        this.throwable = Optional.absent();
    }

    public static DiscoveryItem from(Throwable throwable) {
        return new EmptyViewItem(throwable);
    }
}
