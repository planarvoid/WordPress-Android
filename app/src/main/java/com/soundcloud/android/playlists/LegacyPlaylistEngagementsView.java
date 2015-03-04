package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ToggleButton;

public class LegacyPlaylistEngagementsView extends PlaylistEngagementsView {

    @InjectView(R.id.toggle_like) ToggleButton likeToggle;
    @InjectView(R.id.toggle_repost) ToggleButton repostToggle;
    @InjectView(R.id.btn_share) ImageButton shareButton;

    public LegacyPlaylistEngagementsView(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public void onViewCreated(View view) {
        final ViewGroup holder = (ViewGroup) view.findViewById(R.id.playlist_action_bar_holder);
        View engagementsView = View.inflate(view.getContext(), R.layout.legacy_playlist_action_bar, holder);
        ButterKnife.inject(this, engagementsView);

        likeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListener().onToggleLike(likeToggle.isChecked());
            }
        });

        repostToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListener().onToggleRepost(repostToggle.isChecked());
            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getListener().onShare();
            }
        });
    }

    @Override
    public void onDestroyView() {
        ButterKnife.reset(this);
    }

    public void updateLikeItem(int count, boolean userLiked) {
        updateToggleButton(likeToggle,
                R.string.accessibility_like_action,
                R.plurals.accessibility_stats_likes,
                count,
                userLiked,
                R.string.accessibility_stats_user_liked);
    }

    @Override
    void showOfflineAvailability(boolean isAvailable) {
        // no-op
    }

    @Override
    void showUpsell() {
        // no-op
    }

    @Override
    void hideOfflineContentOptions() {
        // no-op
    }

    @Override
    void showPublicOptionsForYourTrack() {
        shareButton.setVisibility(View.VISIBLE);
        repostToggle.setVisibility(View.GONE);
    }

    @Override
    void showPublicOptions(int repostsCount, boolean repostedByUser) {
        updateToggleButton(repostToggle,
                R.string.accessibility_repost_action,
                R.plurals.accessibility_stats_reposts,
                repostsCount,
                repostedByUser,
                R.string.accessibility_stats_user_reposted);
        repostToggle.setVisibility(View.VISIBLE);
        shareButton.setVisibility(View.VISIBLE);
    }

    @Override
    void hidePublicOptions() {
        repostToggle.setVisibility(View.GONE);
        shareButton.setVisibility(View.GONE);
    }

    @Override
    void setInfoText(String message) {
        // no-op
    }

}
