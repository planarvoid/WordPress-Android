package com.soundcloud.android.collections;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.stations.StationsCollectionsTypes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class CollectionsPreviewRenderer implements CellRenderer<CollectionsItem> {

    private final Navigator navigator;

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
    public CollectionsPreviewRenderer(Navigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View view = inflater.inflate(R.layout.collections_preview_item, parent, false);
        getLikesPreviewView(view).setOnClickListener(goToTrackLikesListener);
        getRecentStationsPreviewView(view).setOnClickListener(goToRecentStationsListener);
        return view;
    }

    private CollectionPreviewView getRecentStationsPreviewView(View view) {
        return (CollectionPreviewView) view.findViewById(R.id.collection_recent_stations_preview);
    }

    private CollectionPreviewView getLikesPreviewView(View view) {
        return (CollectionPreviewView) view.findViewById(R.id.collection_likes_preview);
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionsItem> list) {
        bindPreviewView(list.get(position).getLikes(), getLikesPreviewView(view));
        bindPreviewView(list.get(position).getStations(), getRecentStationsPreviewView(view));
    }

    private void bindPreviewView(List<Urn> entities, CollectionPreviewView previewView) {
        if (entities.isEmpty()) {
            previewView.setVisibility(View.GONE);
        } else {
            previewView.setVisibility(View.VISIBLE);
            previewView.populateArtwork(entities);
        }
    }
}
