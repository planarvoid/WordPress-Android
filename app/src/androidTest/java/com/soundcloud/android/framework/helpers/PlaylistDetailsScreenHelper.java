package com.soundcloud.android.framework.helpers;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.screens.PlaylistDetailsScreen;

public class PlaylistDetailsScreenHelper {

    public static void assertLikeActionOnUnlikedPlaylist(PlaylistDetailsScreen playlistDetailsScreen) {
        assertFalse("Playlist cannot be liked; playlist is already liked", playlistDetailsScreen.isLiked());
        playlistDetailsScreen.touchToggleLike();
        assertTrue("Playlist should be liked", playlistDetailsScreen.isLiked());
    }
}
