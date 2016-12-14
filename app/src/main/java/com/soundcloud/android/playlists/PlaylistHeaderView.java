package com.soundcloud.android.playlists;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.util.AnimUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

@AutoFactory(allowSubclasses = true)
class PlaylistHeaderView {

    private final ImageOperations imageOperations;

    @BindView(R.id.title) TextView titleView;
    @BindView(R.id.username) TextView usernameView;
    @BindView(R.id.title_private) TextView privateTitleView;
    @BindView(R.id.artwork) ImageView artworkView;
    @BindView(R.id.btn_play) View playButton;

    private PlaylistItem playlistInfo;
    private ApiImageSize artworkSize = ApiImageSize.Unknown;

    public PlaylistHeaderView(@Provided ImageOperations imageOperations, View headerView) {
        this.imageOperations = imageOperations;
        this.artworkSize = ApiImageSize.getFullImageSize(headerView.getResources());

        ButterKnife.bind(this, headerView);
    }

    public void setOnPlayButtonClickListener(View.OnClickListener playButtonListener) {
        playButton.setOnClickListener(playButtonListener);
    }

    public void setOnCreatorButtonClickListener(View.OnClickListener creatorClickListener) {
        usernameView.setOnClickListener(creatorClickListener);
    }

    public void setPlaylist(PlaylistItem item, boolean showPlayButton) {
        this.playlistInfo = item;
        getTitleView().setText(playlistInfo.getTitle());
        getTitleView().setVisibility(View.VISIBLE);
        usernameView.setVisibility(View.VISIBLE);
        usernameView.setText(playlistInfo.getCreatorName());
        usernameView.setEnabled(true);

        imageOperations.displayWithPlaceholder(playlistInfo, artworkSize, artworkView);

        if (showPlayButton) {
            AnimUtils.showView(playButton, true);
        } else {
            playButton.setVisibility(View.GONE);
        }
    }

    private TextView getTitleView() {
        if (playlistInfo != null && playlistInfo.isPrivate()) {
            return privateTitleView;
        }
        return titleView;
    }
}
