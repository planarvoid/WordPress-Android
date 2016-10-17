package com.soundcloud.android.collection;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.CellRenderer;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class CollectionPreviewRenderer implements CellRenderer<CollectionItem> {

    private final Navigator navigator;
    private final Resources resources;
    private final FeatureOperations featureOperations;
    private final ImageOperations imageOperations;

    private final View.OnClickListener goToTrackLikesListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            navigator.openTrackLikes(v.getContext());
        }
    };

    private final View.OnClickListener goToStationsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            navigator.openLikedStations(v.getContext());
        }
    };

    private final View.OnClickListener goToPlaylistsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            navigator.openPlaylistsCollection(v.getContext());
        }
    };

    @Inject
    public CollectionPreviewRenderer(Navigator navigator,
                              Resources resources,
                              FeatureOperations featureOperations,
                              ImageOperations imageOperations) {
        this.navigator = navigator;
        this.resources = resources;
        this.featureOperations = featureOperations;
        this.imageOperations = imageOperations;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View view = inflater.inflate(R.layout.collections_preview_item, parent, false);
        getLikesPreviewView(view).setOnClickListener(goToTrackLikesListener);
        return view;
    }

    private void setupStationsView(CollectionPreviewView stationsView) {
        stationsView.setTitle(resources.getString(R.string.stations_collection_title_liked_stations));
        stationsView.setVisibility(View.VISIBLE);
        stationsView.setOnClickListener(goToStationsListener);
    }

    private void setupPlaylistsView(CollectionPreviewView playlistsView, View divider) {
        divider.setVisibility(View.VISIBLE);
        playlistsView.setVisibility(View.VISIBLE);
        playlistsView.setOnClickListener(goToPlaylistsListener);
    }

    private CollectionPreviewView getPlaylistsPreviewView(View view) {
        return (CollectionPreviewView) view.findViewById(R.id.collection_playlists_preview);
    }

    private View getPlaylistsPreviewDividerView(View view) {
        return view.findViewById(R.id.collection_playlists_preview_divider);
    }

    private CollectionPreviewView getStationsPreviewView(View view) {
        return (CollectionPreviewView) view.findViewById(R.id.collection_stations_preview);
    }

    private CollectionPreviewView getLikesPreviewView(View view) {
        return (CollectionPreviewView) view.findViewById(R.id.collection_likes_preview);
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionItem> list) {
        PreviewCollectionItem item = (PreviewCollectionItem) list.get(position);
        bindLikesView(item.getLikes(), view);

        if (item.getStations().isPresent()) {
            setThumbnails(item.getStations().get(), getStationsPreviewView(view));
            setupStationsView(getStationsPreviewView(view));
        }

        if (item.getPlaylists().isPresent()) {
            final CollectionPreviewView playlistsPreviewView = getPlaylistsPreviewView(view);
            final View divider = getPlaylistsPreviewDividerView(view);
            setupPlaylistsView(playlistsPreviewView, divider);
            setThumbnails(item.getPlaylists().get(), playlistsPreviewView);
        }
    }

    private void bindLikesView(LikesItem likes, View view) {
        final CollectionPreviewView likesPreviewView = getLikesPreviewView(view);
        setThumbnails(likes.getTrackPreviews(), likesPreviewView);
        setLikesDownloadProgressIndicator(likes, view);
    }

    private void setThumbnails(List<? extends ImageResource> imageResources, CollectionPreviewView previewView) {
        previewView.refreshThumbnails(imageOperations, imageResources,
                                      resources.getInteger(R.integer.collection_preview_thumbnail_count));
    }

    private void setLikesDownloadProgressIndicator(LikesItem likes, View likesView) {
        final DownloadImageView downloadProgressIcon = (DownloadImageView) likesView.findViewById(R.id.collection_download_state);

        if (featureOperations.isOfflineContentEnabled()) {
            downloadProgressIcon.setState(likes.getDownloadState());
        } else {
            downloadProgressIcon.setState(OfflineState.NOT_OFFLINE);
        }
    }

}
