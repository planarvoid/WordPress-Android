package com.soundcloud.android.main;

import com.soundcloud.android.deeplinks.DeepLink;

import android.net.Uri;

final class NavigationIntentHelper {

    public static boolean shouldGoToStream(Uri data) {
        final DeepLink link = DeepLink.fromUri(data);
        return link.equals(DeepLink.HOME) || link.equals(DeepLink.STREAM);
    }

    public static boolean shoudGoToExplore(Uri data) {
        return DeepLink.fromUri(data).equals(DeepLink.EXPLORE);
    }

    public static boolean shouldGoToSearch(Uri data) {
        return DeepLink.fromUri(data).equals(DeepLink.SEARCH);
    }

    public static boolean resolvesToNavigationItem(Uri data) {
        return shouldGoToStream(data) || shoudGoToExplore(data) || shouldGoToSearch(data);
    }

}
