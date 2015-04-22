package com.soundcloud.android.framework.helpers;


import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.screens.elements.PlaylistItemElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistItemElementHelper {

    public static boolean isLiked(ActivityTest activityTest, PlaylistItemElement playlistItemElement) {
        boolean result = playlistItemElement.clickOverflow().isLiked();
        activityTest.getSolo().goBack();
        return result;
    }

    public static void assertLikeActionOnPlaylist(ActivityTest activityTest, PlaylistItemElement playlistItemElement) {
        if (isLiked(activityTest, playlistItemElement)) {
            playlistItemElement.clickOverflow().toggleLike();
        }
        assertFalse("Playlist should be unliked after clicking unlike from overflow",
                isLiked(activityTest, playlistItemElement));
        playlistItemElement.clickOverflow().toggleLike();
        assertTrue("Playlist is unliked. Should be liked.", isLiked(activityTest, playlistItemElement));
    }

    public static void assertUnlikeActionOnLikedPlaylist(ActivityTest activityTest, PlaylistItemElement playlistItemElement) {
        assertTrue("Unable to unlike playlist; playlist is already unliked.", isLiked(activityTest, playlistItemElement));
        playlistItemElement.clickOverflow().toggleLike();
        assertFalse("Playlist is liked. Should be unliked.", isLiked(activityTest, playlistItemElement));
    }
}
