package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.PlaylistItemIndicatorsView;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.view.OverflowAnchorImageView;
import com.soundcloud.java.optional.Optional;

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
    private final PlaylistItemIndicatorsView playlistItemIndicatorsView;

    @Inject
    PlaylistCollectionItemRenderer(ImageOperations imageOperations,
                                   Resources resources,
                                   Navigator navigator,
                                   FeatureOperations featureOperations,
                                   PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                   PlaylistItemIndicatorsView playlistItemIndicatorsView) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.navigator = navigator;
        this.featureOperations = featureOperations;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
        this.playlistItemIndicatorsView = playlistItemIndicatorsView;
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
        final TextView trackCount = (TextView) view.findViewById(R.id.track_count);
        final View container = view.findViewById(R.id.collections_playlist_item);
        final OverflowAnchorImageView overflowButton = (OverflowAnchorImageView) view.findViewById(R.id.overflow_button);

        container.setOnClickListener(goToPlaylist(playlistItem));
        title.setText(playlistItem.title());
        creator.setText(playlistItem.creatorName());
        trackCount.setText(String.valueOf(playlistItem.trackCount()));
        setupOverFlow(overflowButton, playlistItem);
        imageOperations.displayInAdapterView(
                playlistItem,
                ApiImageSize.getFullImageSize(resources),
                artwork
        );
        playlistItemIndicatorsView.setupView(view, playlistItem.isPrivate(), playlistItem.isUserLike(),
                                             featureOperations.isOfflineContentEnabled()
                                             ? Optional.of(playlistItem.offlineState())
                                             : Optional.absent());
    }

    private void setupOverFlow(final OverflowAnchorImageView button, final PlaylistItem playlistItem) {
        button.setOnClickListener(v -> playlistItemMenuPresenter.show(button, playlistItem));
        OverflowButtonBackground.install(button);
    }

    private View.OnClickListener goToPlaylist(final PlaylistItem playlistItem) {
        return view -> navigator.legacyOpenPlaylist(view.getContext(), playlistItem.getUrn(), Screen.PLAYLISTS);
    }
}
