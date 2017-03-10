package com.soundcloud.android.discovery.newforyou;

import static com.soundcloud.java.collections.Lists.transform;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.EntityItemCreator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

public class NewForYouBucketRenderer implements CellRenderer<NewForYouDiscoveryItem> {

    private final ImageOperations imageOperations;
    private final Navigator navigator;
    private final EntityItemCreator entityItemCreator;

    @Inject
    NewForYouBucketRenderer(ImageOperations imageOperations, Navigator navigator, EntityItemCreator entityItemCreator) {
        this.imageOperations = imageOperations;
        this.navigator = navigator;
        this.entityItemCreator = entityItemCreator;
    }

    @Override
    public View createItemView(ViewGroup viewGroup) {
        return LayoutInflater.from(viewGroup.getContext())
                             .inflate(R.layout.new_for_you_bucket, viewGroup, false);
    }

    @Override
    public void bindItemView(int position, View bucketView, List<NewForYouDiscoveryItem> items) {
        List<ImageResource> imageResources = transform(items.get(position).newForYou().tracks(), entityItemCreator::trackItem);

        bindCoverArtAnimation((NewForYouArtworkView) bucketView.findViewById(R.id.artwork), imageResources);
        bindViewAllViews(bucketView.findViewById(R.id.view_all_container), bucketView.findViewById(R.id.new_for_you_bucket_header));
    }

    private void bindCoverArtAnimation(NewForYouArtworkView artworkView, List<ImageResource> imageResources) {
        artworkView.bindWithAnimation(imageOperations, imageResources);
    }

    private void bindViewAllViews(View viewAllView, View headerView) {
        viewAllView.setOnClickListener(getOnClickListener(viewAllView));
        headerView.setOnClickListener(getOnClickListener(viewAllView));
    }

    private View.OnClickListener getOnClickListener(View viewAllView) {
        return view -> navigator.openNewForYou(viewAllView.getContext());
    }
}
