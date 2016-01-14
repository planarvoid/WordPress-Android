package com.soundcloud.android.collection;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
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
    public CollectionPreviewRenderer(Navigator navigator, Resources resources, FeatureFlags featureFlags) {
        this.navigator = navigator;
        this.resources = resources;
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
        if (featureFlags.isEnabled(Flag.STATIONS_SOFT_LAUNCH)) {
            recentStationsView.setVisibility(View.VISIBLE);
            recentStationsView.setOnClickListener(goToRecentStationsListener);
        }
    }

    private CollectionPreviewView getRecentStationsPreviewView(View view) {
        return (CollectionPreviewView) view.findViewById(R.id.collection_recent_stations_preview);
    }

    private CollectionPreviewView getLikesPreviewView(View view) {
        return (CollectionPreviewView) view.findViewById(R.id.collection_likes_preview);
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionItem> list) {
        bindPreviewView(list.get(position).getLikes(), getLikesPreviewView(view));
        bindPreviewView(list.get(position).getStations(), getRecentStationsPreviewView(view));
    }

    private void bindPreviewView(List<Urn> entities, CollectionPreviewView previewView) {
        previewView.refreshThumbnails(entities, resources.getInteger(R.integer.collection_preview_thumbnail_count));
    }
}
