package com.soundcloud.android.playlists;

import com.soundcloud.android.model.Urn;

interface PlaylistDetailsInputs /* remove after new fragment live */ {

    void onHeaderPlayButtonClicked();

    void onCreatorClicked();

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
