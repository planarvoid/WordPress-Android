package com.soundcloud.android.playlists;

import butterknife.Bind;
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
class PlaylistDetailsView {

    private final ImageOperations imageOperations;

    @Bind(R.id.title) TextView titleView;
    @Bind(R.id.username) TextView usernameView;
    @Bind(R.id.title_private) TextView privateTitleView;
    @Bind(R.id.artwork) ImageView artworkView;
    @Bind(R.id.btn_play) View playButton;

    private PlaylistItem playlistInfo;
    private ApiImageSize artworkSize = ApiImageSize.Unknown;

    public PlaylistDetailsView(@Provided ImageOperations imageOperations, View headerView) {
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

    public void setPlaylist(PlaylistItem item, boolean hasTracks) {
        playlistInfo = item;
        getTitleView().setText(item.getTitle());
        getTitleView().setVisibility(View.VISIBLE);
        usernameView.setVisibility(View.VISIBLE);
        usernameView.setText(item.getCreatorName());
        usernameView.setEnabled(true);

        imageOperations.displayWithPlaceholder(item, artworkSize, artworkView);

        if (hasTracks) {
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
