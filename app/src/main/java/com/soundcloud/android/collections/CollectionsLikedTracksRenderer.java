package com.soundcloud.android.collections;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class CollectionsLikedTracksRenderer implements CellRenderer<CollectionsItem> {

    private final Navigator navigator;

    private final View.OnClickListener goToTrackLikesListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            navigator.openTrackLikes(v.getContext());
        }
    };

    @Inject
    public CollectionsLikedTracksRenderer(Navigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.collection_liked_tracks_item, parent, false);
        setGoToTrackLikesListener(view);
        return view;
    }

    private void setGoToTrackLikesListener(View view) {
        view.setOnClickListener(goToTrackLikesListener);
    }

    @Override
    public void bindItemView(int position, View view, List<CollectionsItem> list) {
        ((TextView) view.findViewById(R.id.liked_tracks_count))
                .setText(String.valueOf(list.get(position).getLikesCount()));
    }
}
