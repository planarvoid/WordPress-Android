package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistsHelper {
    private ActivityTest activityTest;
    private PlaylistsScreen playlistsScreen;

    public PlaylistsHelper(ActivityTest activityTest, PlaylistsScreen playlistsScreen) {
        this.activityTest = activityTest;
        this.playlistsScreen = playlistsScreen;
    }

    public String getPlaylistItemTitle(int index) {
        return playlistsScreen.get(index).getTitle();
    }

    public boolean isPlaylistItemLiked(int index) {
        boolean result = playlistsScreen.get(index).clickOverflow().isLiked();
        activityTest.getSolo().goBack();
        return result;
    }

    public PlaylistsScreen togglePlaylistItemLike(int index) {
        playlistsScreen.get(index).clickOverflow().toggleLike();
        return playlistsScreen;
    }

    public PlaylistsScreen unlikePlaylistItemIfCurrentlyLiked(int index) {
        if (isPlaylistItemLiked(index)) {
            togglePlaylistItemLike(index);
        }
        return playlistsScreen;
    }

    public PlaylistsScreen likePlaylistItemEvenIfCurrentlyLiked(int index) {
        unlikePlaylistItemIfCurrentlyLiked(index);
        return togglePlaylistItemLike(index);
    }
}
