package com.soundcloud.android.users;

import com.soundcloud.android.R;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.DrawableRes;

import java.util.Arrays;

enum Network {
    DISCOGS("discogs", Optional.of("Discogs"), R.drawable.favicon_discogs),
    MYSPACE("myspace", Optional.of("Myspace"), R.drawable.favicon_myspace),
    PERSONAL("personal", Optional.absent(), R.drawable.favicon_website);

    private final String network;
    private final Optional<String> displayName;
    private final int drawableId;

    Network(String network, Optional<String> displayName, @DrawableRes int drawableId) {
        this.network = network;
        this.displayName = displayName;
        this.drawableId = drawableId;
    }

    static Network from(String network) {
        return Iterables.find(Arrays.asList(Network.values()), item -> item.network.equals(network), PERSONAL);
    }

    public Optional<String> displayName() {
        return displayName;
    }

    @DrawableRes
    public int drawableId() {
        return drawableId;
    }
}
