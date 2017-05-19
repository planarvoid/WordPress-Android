package com.soundcloud.android.main;

import com.soundcloud.android.deeplinks.DeepLink;

import android.net.Uri;

final class NavigationIntentHelper {

    static boolean shouldGoToStream(Uri data) {
        final DeepLink link = DeepLink.fromUri(data);
        return link.equals(DeepLink.HOME) || link.equals(DeepLink.STREAM);
    }

    static boolean shouldGoToSearch(Uri data) {
        return DeepLink.fromUri(data).equals(DeepLink.SEARCH);
    }

    static boolean resolvesToNavigationItem(Uri data) {
        return shouldGoToStream(data) || shouldGoToSearch(data);
    }

    private NavigationIntentHelper() {
    }
}
