package com.soundcloud.android.playlists;

import android.content.Context;

interface PlaylistDetailsInputs /* remove after new fragment live */ {

    void onHeaderPlayButtonClicked();

    void onCreatorClicked();

    void onItemTriggered(PlaylistDetailTrackItem item);

    void onItemTriggered(PlaylistDetailUpsellItem item);

    void onEnterEditMode();

    void onExitEditMode();

    void onPlayNext();

    void onToggleLike(boolean isLiked);

    void onToggleRepost(boolean isReposted);

    void onShareClicked();

    void onMakeOfflineAvailable(Context context);

    void onMakeOfflineUnavailable();

    void onMakeOfflineUpsell();

    void onOverflowUpsell();

    void onOverflowUpsellImpression();

    void onPlayShuffled();

    void onDeletePlaylist();
}
