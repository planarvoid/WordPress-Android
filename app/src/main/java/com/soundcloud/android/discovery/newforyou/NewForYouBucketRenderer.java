package com.soundcloud.android.discovery.newforyou;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class NewForYouBucketRenderer implements CellRenderer<NewForYouDiscoveryItem> {

    private final ImageOperations imageOperations;
    private final Navigator navigator;

    @Inject
    NewForYouBucketRenderer(ImageOperations imageOperations, Navigator navigator) {
        this.imageOperations = imageOperations;
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext())
                             .inflate(R.layout.new_for_you_bucket, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View bucketView, List<NewForYouDiscoveryItem> items) {
        List<ImageResource> imageResources = transform(items.get(position).newForYou().tracks(), TrackItem::from);

        bindCoverArtAnimation((NewForYouArtworkView) bucketView.findViewById(R.id.artwork), imageResources);
        bindViewAllView(bucketView.findViewById(R.id.view_all_container));
    }

    private void bindCoverArtAnimation(NewForYouArtworkView artworkView, List<ImageResource> imageResources) {
        artworkView.bind(imageOperations, imageResources);
    }

    private void bindViewAllView(View viewAllView) {
        // TODO: Open the correct screen
        viewAllView.setOnClickListener(view -> navigator.openTrackLikes(viewAllView.getContext()));
    }
}
