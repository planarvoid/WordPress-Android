package com.soundcloud.android.collection;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.stations.StationsCollectionsTypes;

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
    private final FeatureFlags featureFlags;

    private final View.OnClickListener goToTrackLikesListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            navigator.openTrackLikes(v.getContext());
        }
    };

    private final View.OnClickListener goToRecentStationsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            navigator.openViewAllStations(v.getContext(), StationsCollectionsTypes.RECENT);
        }
    };

    @Inject
    public CollectionPreviewRenderer(Navigator navigator, Resources resources, FeatureOperations featureOperations,
                                     FeatureFlags featureFlags) {
        this.navigator = navigator;
        this.resources = resources;
        this.featureOperations = featureOperations;
        this.featureFlags = featureFlags;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View view = inflater.inflate(R.layout.collections_preview_item, parent, false);
        getLikesPreviewView(view).setOnClickListener(goToTrackLikesListener);
        setupRecentStationsView(getRecentStationsPreviewView(view));
        return view;
    }

    private void setupRecentStationsView(CollectionPreviewView recentStationsView) {
        recentStationsView.setVisibility(View.VISIBLE);
        recentStationsView.setOnClickListener(goToRecentStationsListener);
    }

    private CollectionPreviewView getRecentStationsPreviewView(View view) {
        return (CollectionPreviewView) view.findViewById(R.id.collection_recent_stations_preview);
    }

    private CollectionPreviewView getLikesPreviewView(View view) {
        return (CollectionPreviewView) view.findViewById(R.id.collection_likes_preview);
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionItem> list) {
        bindLikesView(list.get(position).getLikes(), view);
        setThumbnails(list.get(position).getStations(), getRecentStationsPreviewView(view));
    }

    private void bindLikesView(LikesItem likes, View view) {
        final CollectionPreviewView likesPreviewView = getLikesPreviewView(view);
        setThumbnails(likes.getUrns(), likesPreviewView);
        setLikesDownloadProgressIndicator(likes, view);
    }

    private void setThumbnails(List<Urn> entities, CollectionPreviewView previewView) {
        previewView.refreshThumbnails(entities, resources.getInteger(R.integer.collection_preview_thumbnail_count));
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
