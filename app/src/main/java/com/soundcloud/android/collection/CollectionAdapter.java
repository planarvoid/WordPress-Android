package com.soundcloud.android.collection;

import com.soundcloud.android.collection.playhistory.PlayHistoryBucketRenderer;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketRenderer;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItemRenderer;

import android.view.View;

import javax.inject.Inject;

public class CollectionAdapter extends PagingRecyclerItemAdapter<CollectionItem, RecyclerItemAdapter.ViewHolder> {

    private final OnboardingItemCellRenderer onboardingItemCellRenderer;
    private final PlayHistoryBucketRenderer playHistoryBucketRenderer;

    @Inject
    public CollectionAdapter(OnboardingItemCellRenderer onboardingItemCellRenderer,
                             CollectionPreviewRenderer collectionPreviewRenderer,
                             RecentlyPlayedBucketRenderer recentlyPlayedBucketRenderer,
                             PlayHistoryBucketRenderer playHistoryBucketRenderer) {
        super(
                new CellRendererBinding<>(CollectionItem.TYPE_ONBOARDING, onboardingItemCellRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PREVIEW, collectionPreviewRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_RECENTLY_PLAYED_BUCKET, recentlyPlayedBucketRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PLAY_HISTORY_BUCKET, playHistoryBucketRenderer));
        this.onboardingItemCellRenderer = onboardingItemCellRenderer;
        this.playHistoryBucketRenderer = playHistoryBucketRenderer;

    }

    void setOnboardingListener(OnboardingItemCellRenderer.Listener listener) {
        onboardingItemCellRenderer.setListener(listener);
    }

    @Override
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getType();
    }

    void setTrackClickListener(TrackItemRenderer.Listener listener) {
        playHistoryBucketRenderer.setTrackClickListener(listener);
    }
}
