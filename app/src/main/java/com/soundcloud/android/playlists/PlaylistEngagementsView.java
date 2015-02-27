package com.soundcloud.android.playlists;

import android.view.View;

public interface PlaylistEngagementsView {
    void onViewCreated(View view);

    void setOnEngagement(OnEngagementListener listener);

    void showRepostToggle();

    void hideRepostToggle();

    void showShareButton();

    void hideShareButton();

    void updateLikeButton(int likesCount, boolean likedByUser);

    void updateRepostButton(int repostsCount, boolean repostedByUser);

    public interface OnEngagementListener {
        void onToggleLike(boolean isLiked);
        void onToggleRepost(boolean isReposted);
        void onShare();
    }
}
