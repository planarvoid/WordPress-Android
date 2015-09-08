package com.soundcloud.android.collections;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;

import android.support.annotation.VisibleForTesting;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

class CollectionsLikedTracksRenderer implements CellRenderer<CollectionsItem> {

    @VisibleForTesting
    static final int EXTRA_HOLDER_VIEWS = 1;
    static final int MAX_LIKE_IMAGES = 3;

    private final Navigator navigator;
    private final ImageOperations imageOperations;

    private final View.OnClickListener goToTrackLikesListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            navigator.openTrackLikes(v.getContext());
        }
    };

    @Inject
    public CollectionsLikedTracksRenderer(Navigator navigator, ImageOperations imageOperations) {
        this.navigator = navigator;
        this.imageOperations = imageOperations;
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
        final ViewGroup holder = (ViewGroup) view.findViewById(R.id.holder);
        populateLikedArtwork(position, list, holder);
    }

    private void populateLikedArtwork(int position, List<CollectionsItem> list, ViewGroup holder) {

        final List<Urn> likes = list.get(position).getLikes();
        final LayoutInflater inflater = LayoutInflater.from(holder.getContext());
        final int imagesToDisplay = Math.min(MAX_LIKE_IMAGES, likes.size());

        for (int i = 0; i < imagesToDisplay; i++) {
            if (needsViewForIndex(holder, i)) {
                inflateImageViewIntoHolder(holder, inflater);
            }
            ImageView icon = (ImageView) holder.getChildAt(i + EXTRA_HOLDER_VIEWS);
            imageOperations.displayWithPlaceholder(likes.get(i), ApiImageSize.getListItemImageSize(holder.getResources()), icon);
        }

        removeExtraImageViews(holder, imagesToDisplay);
    }

    private boolean needsViewForIndex(ViewGroup holder, int i) {
        return holder.getChildCount() == i + EXTRA_HOLDER_VIEWS;
    }

    private void inflateImageViewIntoHolder(ViewGroup holder, LayoutInflater inflater) {
        inflater.inflate(R.layout.collections_liked_track_icon_sm, holder);
    }

    private void removeExtraImageViews(ViewGroup holder, int displayedImages) {
        int extraPosition = displayedImages + EXTRA_HOLDER_VIEWS;
        while (extraPosition < holder.getChildCount()){
            holder.removeViewAt(extraPosition);
        }
    }
}
