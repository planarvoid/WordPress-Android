package com.soundcloud.android.playlists;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;

import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

class PlaylistPresenter {

    private final ImageOperations imageOperations;

    private TextView titleView;
    private TextView usernameView;
    private TextView privateTitleView;
    private ImageView artworkView;
    private PlaylistWithTracks playlistWithTracks;

    private ApiImageSize artworkSize = ApiImageSize.Unknown;

    @Inject
    public PlaylistPresenter(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    public PlaylistPresenter setTitleView(TextView titleView) {
        this.titleView = titleView;
        return this;
    }

    public PlaylistPresenter setPrivateTitleView(TextView titleView) {
        this.privateTitleView = titleView;
        return this;
    }

    public PlaylistPresenter setUsernameView(TextView usernameView) {
        this.usernameView = usernameView;
        return this;
    }

    public PlaylistPresenter setTextVisibility(int visibility) {
        getTitleView().setVisibility(visibility);
        usernameView.setVisibility(visibility);
        return this;
    }

    public PlaylistPresenter setArtwork(ImageView artworkView, ApiImageSize artworkSize) {
        this.artworkView = artworkView;
        this.artworkSize = artworkSize;
        return this;
    }

    public void setPlaylist(PlaylistWithTracks item) {
        playlistWithTracks = item;

        getTitleView().setText(item.getTitle());
        usernameView.setText(item.getCreatorName());
        imageOperations.displayWithPlaceholder(item.getUrn(), artworkSize, artworkView);
    }

    private TextView getTitleView() {
        if (playlistWithTracks != null && playlistWithTracks.isPrivate()) {
            return privateTitleView;
        }
        return titleView;
    }
}
