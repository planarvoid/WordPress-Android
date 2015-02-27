package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ToggleButton;

import javax.inject.Inject;

public class LegacyPlaylistEngagementsView extends PlaylistEngagementsView {

    @InjectView(R.id.toggle_like) ToggleButton likeToggle;
    @InjectView(R.id.toggle_repost) ToggleButton repostToggle;
    @InjectView(R.id.btn_share) ImageButton shareButton;

    public LegacyPlaylistEngagementsView(Context context, Resources resources) {
        super(context, resources);
    }

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


    public void updateLikeButton(int count, boolean userLiked) {
        updateToggleButton(likeToggle,
                R.string.accessibility_like_action,
                R.plurals.accessibility_stats_likes,
                count,
                userLiked,
                R.string.accessibility_stats_user_liked);
    }

    public void updateRepostButton(int count, boolean userReposted) {
        updateToggleButton(repostToggle,
                R.string.accessibility_repost_action,
                R.plurals.accessibility_stats_reposts,
                count,
                userReposted,
                R.string.accessibility_stats_user_reposted);
    }

    @Override
    public void showRepostToggle() {
        repostToggle.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideRepostToggle() {
        repostToggle.setVisibility(View.GONE);
    }

    @Override
    public void showShareButton() {
        shareButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideShareButton() {
        shareButton.setVisibility(View.GONE);
    }

}
