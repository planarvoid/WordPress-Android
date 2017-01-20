package com.soundcloud.android.playlists;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.util.AnimUtils;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

@AutoFactory(allowSubclasses = true)
class PlaylistHeaderView {

    private final ImageOperations imageOperations;

    @BindView(R.id.title) TextView titleView;
    @BindView(R.id.username) TextView usernameView;
    @BindView(R.id.title_private) TextView privateTitleView;
    @BindView(R.id.artwork) ImageView artworkView;
    @BindView(R.id.btn_play) View playButton;

    private PlaylistDetailHeaderItem item;
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

    public void setPlaylist(PlaylistDetailHeaderItem item, boolean showPlayButton) {
        this.item = item;
        getTitleView().setText(this.item.title());
        getTitleView().setVisibility(View.VISIBLE);
        usernameView.setVisibility(View.VISIBLE);
        usernameView.setText(this.item.creatorName());
        usernameView.setEnabled(true);

        imageOperations.displayWithPlaceholder(this.item, artworkSize, artworkView);

        if (showPlayButton) {
            AnimUtils.showView(playButton, true);
        } else {
            playButton.setVisibility(View.GONE);
        }
    }

    private TextView getTitleView() {
        if (item != null && item.isPrivate()) {
            return privateTitleView;
        }
        return titleView;
    }
}
