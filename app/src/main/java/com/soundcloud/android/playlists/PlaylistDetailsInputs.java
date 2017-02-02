package com.soundcloud.android.playlists;

interface PlaylistDetailsInputs /* remove after new fragment live */ {

    void onHeaderPlayButtonClicked();

    void onCreatorClicked();

    void onPlayAtPosition(Integer position);

    void onEnterEditMode();

    void onExitEditMode();

    void onPlayNext();

    void onToggleLike(boolean isLiked);

    void onToggleRepost(boolean isReposted);

    void onShare();

    void onMakeOfflineAvailable();

    void onMakeOfflineUnavailable();

    void onUpsell();

    void onOverflowUpsell();

    void onOverflowUpsellImpression();

    void onPlayShuffled();

    void onDeletePlaylist();
}
