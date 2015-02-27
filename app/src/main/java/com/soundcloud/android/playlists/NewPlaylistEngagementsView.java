package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

public class NewPlaylistEngagementsView implements PlaylistEngagementsView {
    public NewPlaylistEngagementsView(Context context, Resources resources) {

    }

    @Override
    public void onViewCreated(View view) {
        final ViewGroup holder = (ViewGroup) view.findViewById(R.id.playlist_action_bar_holder);
        View engagementsView = View.inflate(view.getContext(), R.layout.new_playlist_action_bar, holder);
        ButterKnife.inject(this, engagementsView);
    }

    @Override
    public void setOnEngagement(OnEngagementListener listener) {

    }

    @Override
    public void showRepostToggle() {

    }

    @Override
    public void hideRepostToggle() {

    }

    @Override
    public void showShareButton() {

    }

    @Override
    public void hideShareButton() {

    }

    @Override
    public void updateLikeButton(int likesCount, boolean likedByUser) {

    }

    @Override
    public void updateRepostButton(int repostsCount, boolean repostedByUser) {

    }
}
