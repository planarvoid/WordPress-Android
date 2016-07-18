package com.soundcloud.android.discovery;

import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketItem;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketRenderer;
import com.soundcloud.android.presentation.CellRenderer;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

class RecentlyPlayedDiscoveryBucketRenderer implements CellRenderer<RecentlyPlayedBucketDiscoveryItem> {

    private final RecentlyPlayedBucketRenderer renderer;

    @Inject
    RecentlyPlayedDiscoveryBucketRenderer(RecentlyPlayedBucketRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return renderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<RecentlyPlayedBucketDiscoveryItem> items) {
        RecentlyPlayedBucketDiscoveryItem item = items.get(position);
        RecentlyPlayedBucketItem bucket = RecentlyPlayedBucketItem.create(item.getRecentlyPlayed());
        renderer.bindItemView(0, itemView, Collections.singletonList(bucket));
    }
}
