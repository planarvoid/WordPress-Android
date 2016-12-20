package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.OverflowMenuOptions;
import com.soundcloud.android.utils.ViewUtils;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class PlaylistCollectionItemRenderer implements CellRenderer<PlaylistCollectionPlaylistItem> {

    private final ImageOperations imageOperations;
    private final Resources resources;
    private final Navigator navigator;
    private final FeatureOperations featureOperations;
    private final PlaylistItemMenuPresenter playlistItemMenuPresenter;

    @Inject
    public PlaylistCollectionItemRenderer(ImageOperations imageOperations,
                                          Resources resources,
                                          Navigator navigator,
                                          FeatureOperations featureOperations,
                                          PlaylistItemMenuPresenter playlistItemMenuPresenter) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.navigator = navigator;
        this.featureOperations = featureOperations;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.collection_playlist_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View view, List<PlaylistCollectionPlaylistItem> list) {
        final PlaylistCollectionPlaylistItem item = list.get(position);
        final PlaylistItem playlistItem = item.getPlaylistItem();
        final ImageView artwork = (ImageView) view.findViewById(R.id.artwork);
        final TextView title = (TextView) view.findViewById(R.id.title);
        final TextView creator = (TextView) view.findViewById(R.id.creator);
        final View privateIndicator = view.findViewById(R.id.private_indicator);
        final View likeIndicator = view.findViewById(R.id.like_indicator);
        final TextView trackCount = (TextView) view.findViewById(R.id.track_count);

        view.setOnClickListener(goToPlaylist(playlistItem));

        title.setText(playlistItem.getTitle());
        creator.setText(playlistItem.getCreatorName());
        privateIndicator.setVisibility(playlistItem.isPrivate() ? View.VISIBLE : View.GONE);
        likeIndicator.setVisibility(playlistItem.isLikedByCurrentUser() ? View.VISIBLE : View.GONE);
        trackCount.setText(String.valueOf(playlistItem.getTrackCount()));

        imageOperations.displayInAdapterView(
                playlistItem,
                ApiImageSize.getFullImageSize(resources),
                artwork
        );

        setupOverFlow(view.findViewById(R.id.overflow_button), playlistItem);
        setDownloadProgressIndicator(view, playlistItem);
    }

    private void setupOverFlow(final View button, final PlaylistItem playlistItem) {
        final OverflowMenuOptions options = OverflowMenuOptions.builder().showOffline(true).build();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistItemMenuPresenter.show(button, playlistItem, options);
            }
        });
        ViewUtils.extendTouchArea(button);
    }

    private void setDownloadProgressIndicator(View itemView, PlaylistItem playlistItem) {
        final DownloadImageView downloadProgressIcon = (DownloadImageView) itemView.findViewById(R.id.item_download_state);

        if (featureOperations.isOfflineContentEnabled()) {
            downloadProgressIcon.setState(playlistItem.getDownloadState());
        } else {
            downloadProgressIcon.setState(OfflineState.NOT_OFFLINE);
        }
    }

    private View.OnClickListener goToPlaylist(final PlaylistItem playlistItem) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigator.legacyOpenPlaylist(view.getContext(), playlistItem.getUrn(), Screen.PLAYLISTS);
            }
        };
    }

}
