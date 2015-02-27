package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

public class NewPlaylistEngagementsView extends PlaylistEngagementsView {

    @InjectView(R.id.toggle_like) ToggleButton likeToggle;

    public NewPlaylistEngagementsView(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public void onViewCreated(View view) {
        final ViewGroup holder = (ViewGroup) view.findViewById(R.id.playlist_action_bar_holder);
        View engagementsView = View.inflate(view.getContext(), R.layout.new_playlist_action_bar, holder);
        ButterKnife.inject(this, engagementsView);

        likeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListener().onToggleLike(likeToggle.isChecked());
            }
        });
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
        updateToggleButton(likeToggle,
                R.string.accessibility_like_action,
                R.plurals.accessibility_stats_likes,
                likesCount,
                likedByUser,
                R.string.accessibility_stats_user_liked);
    }

    @Override
    public void updateRepostButton(int repostsCount, boolean repostedByUser) {

    }
}
