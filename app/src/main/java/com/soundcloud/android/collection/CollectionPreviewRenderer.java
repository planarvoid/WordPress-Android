package com.soundcloud.android.collection;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.checks.Preconditions;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.StringRes;
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
    private final FeatureFlags featureFlags;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    @Inject
    public CollectionPreviewRenderer(Navigator navigator,
                                     Resources resources,
                                     FeatureOperations featureOperations,
                                     ImageOperations imageOperations,
                                     PerformanceMetricsEngine performanceMetricsEngine,
                                     FeatureFlags featureFlags) {
        this.navigator = navigator;
        this.resources = resources;
        this.featureOperations = featureOperations;
        this.imageOperations = imageOperations;
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.featureFlags = featureFlags;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View view = inflater.inflate(R.layout.collections_preview_item, parent, false);
        getLikesPreviewView(view).setOnClickListener(this::onGoToTrackLikesClick);
        return view;
    }

    @VisibleForTesting
    void onGoToTrackLikesClick(View v) {
        performanceMetricsEngine.startMeasuring(MetricType.LIKED_TRACKS_FIRST_PAGE_LOAD);
        navigator.openTrackLikes(v.getContext());
    }

    @VisibleForTesting
    void onGoToPlaylistsAndAlbumsClick(Activity activity) {
        performanceMetricsEngine.startMeasuring(MetricType.PLAYLISTS_LOAD);
        navigator.openPlaylistsAndAlbumsCollection(activity);
    }

    private void setupStationsView(CollectionPreviewView stationsView) {
        stationsView.setTitle(resources.getString(R.string.stations_collection_title_liked_stations));
        stationsView.setVisibility(View.VISIBLE);
        stationsView.setOnClickListener(this::onGoToStationsClick);
    }

    @VisibleForTesting
    void onGoToStationsClick(View v) {
        performanceMetricsEngine.startMeasuring(MetricType.LIKED_STATIONS_LOAD);
        navigator.openLikedStations(v.getContext());
    }

    private CollectionPreviewView setupPlaylistsView(View parent, @StringRes int titleRes, View.OnClickListener onClickListener) {
        final CollectionPreviewView playlistsView = (CollectionPreviewView) parent.findViewById(R.id.collection_playlists_preview);
        final View divider = parent.findViewById(R.id.collection_playlists_preview_divider);
        divider.setVisibility(View.VISIBLE);
        playlistsView.setVisibility(View.VISIBLE);
        playlistsView.setTitle(resources.getString(titleRes));
        playlistsView.setOnClickListener(onClickListener);
        return playlistsView;
    }

    private CollectionPreviewView setupAlbumsView(View parent, View.OnClickListener onClickListener) {
        final CollectionPreviewView albumsView = (CollectionPreviewView) parent.findViewById(R.id.collection_albums_preview);
        final View divider = parent.findViewById(R.id.collection_albums_preview_divider);
        divider.setVisibility(View.VISIBLE);
        albumsView.setVisibility(View.VISIBLE);
        albumsView.setOnClickListener(onClickListener);
        return albumsView;
    }

    private CollectionPreviewView getStationsPreviewView(View view) {
        return (CollectionPreviewView) view.findViewById(R.id.collection_stations_preview);
    }

    private CollectionPreviewView getLikesPreviewView(View view) {
        return (CollectionPreviewView) view.findViewById(R.id.collection_likes_preview);
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionItem> list) {
        checkArgument(view.getContext() instanceof Activity);
        Activity activity = (Activity) view.getContext();
        PreviewCollectionItem item = (PreviewCollectionItem) list.get(position);
        bindLikesView(item.getLikes(), view);

        item.getStations().ifPresent(stationRecords -> {
            setThumbnails(stationRecords, getStationsPreviewView(view));
            setupStationsView(getStationsPreviewView(view));
        });

        item.getPlaylistsAndAlbums().ifPresent(playlistsAndAlbums -> {
            CollectionPreviewView playlistsPreviewView = setupPlaylistsView(view, R.string.collections_playlists_header, v -> onGoToPlaylistsAndAlbumsClick(activity));
            setThumbnails(playlistsAndAlbums, playlistsPreviewView);
        });

        item.getPlaylists().ifPresent(playlists -> {
            CollectionPreviewView playlistsPreviewView = setupPlaylistsView(view, R.string.collections_playlists_separate_header, v -> navigator.openPlaylistsCollection(activity));
            setThumbnails(playlists, playlistsPreviewView);
        });

        item.getAlbums().ifPresent(albums -> {
            CollectionPreviewView albumsPreviewView = setupAlbumsView(view, v -> navigator.openAlbumsCollection(activity));
            setThumbnails(albums, albumsPreviewView);
        });
    }

    private void bindLikesView(LikesItem likes, View view) {
        final CollectionPreviewView likesPreviewView = getLikesPreviewView(view);
        setThumbnails(likes.trackPreviews(), likesPreviewView);
        setLikesDownloadProgressIndicator(likes, view);
    }

    private void setThumbnails(List<? extends ImageResource> imageResources, CollectionPreviewView previewView) {
        previewView.refreshThumbnails(imageOperations, imageResources,
                                      resources.getInteger(R.integer.collection_preview_thumbnail_count));
    }

    private void setLikesDownloadProgressIndicator(LikesItem likes, View likesView) {
        final DownloadImageView downloadProgressIcon = (DownloadImageView) likesView.findViewById(R.id.collection_download_state);
        if (featureFlags.isEnabled(Flag.NEW_OFFLINE_ICONS)) {
            downloadProgressIcon.setVisibility(View.GONE);
        } else {
            downloadProgressIcon.setState(featureOperations.isOfflineContentEnabled()
                                          ? likes.offlineState()
                                          : OfflineState.NOT_OFFLINE, false);
        }
    }
}
