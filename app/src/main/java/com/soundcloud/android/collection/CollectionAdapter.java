package com.soundcloud.android.collection;

import com.soundcloud.android.collection.playhistory.PlayHistoryBucketRenderer;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketRenderer;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.ListDiffUtilCallback;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.UpdatableRecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItemRenderer;

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

public class CollectionAdapter extends UpdatableRecyclerItemAdapter<CollectionItem, RecyclerItemAdapter.ViewHolder> {

    private final OnboardingItemCellRenderer onboardingItemCellRenderer;
    private final OfflineOnboardingItemCellRenderer offlineOnboardingItemCellRenderer;
    private final UpsellItemCellRenderer upsellItemCellRenderer;
    private final PlayHistoryBucketRenderer playHistoryBucketRenderer;
    private final RecentlyPlayedBucketRenderer recentlyPlayedBuckerRenderer;

    @Inject
    CollectionAdapter(OnboardingItemCellRenderer onboardingItemCellRenderer,
                      OfflineOnboardingItemCellRenderer offlineOnboardingItemCellRenderer,
                      UpsellItemCellRenderer upsellItemCellRenderer,
                      CollectionPreviewRenderer collectionPreviewRenderer,
                      RecentlyPlayedBucketRenderer recentlyPlayedBucketRenderer,
                      PlayHistoryBucketRenderer playHistoryBucketRenderer) {
        super(
                new CellRendererBinding<>(CollectionItem.TYPE_ONBOARDING, onboardingItemCellRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_OFFLINE_ONBOARDING, offlineOnboardingItemCellRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_UPSELL, upsellItemCellRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PREVIEW, collectionPreviewRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_RECENTLY_PLAYED_BUCKET, recentlyPlayedBucketRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PLAY_HISTORY_BUCKET, playHistoryBucketRenderer));
        this.onboardingItemCellRenderer = onboardingItemCellRenderer;
        this.offlineOnboardingItemCellRenderer = offlineOnboardingItemCellRenderer;
        this.upsellItemCellRenderer = upsellItemCellRenderer;
        this.playHistoryBucketRenderer = playHistoryBucketRenderer;
        this.recentlyPlayedBuckerRenderer = recentlyPlayedBucketRenderer;

    }

    void detach() {
        recentlyPlayedBuckerRenderer.detach();
        playHistoryBucketRenderer.detach();
    }

    @NonNull
    @Override
    protected DiffUtil.Callback createDiffUtilCallback(List<CollectionItem> oldList, List<CollectionItem> newList) {
        return new ListDiffUtilCallback<CollectionItem>(oldList, newList) {
            @Override
            protected boolean areItemsTheSame(CollectionItem oldItem, CollectionItem newItem) {
                // Here is safe to consider the item type as an id, since buckets are not duplicated
                return oldItem.getType() == newItem.getType();
            }
        };
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

    void setOnboardingListener(OnboardingItemCellRenderer.Listener listener) {
        onboardingItemCellRenderer.setListener(listener);
    }

    void setOfflineOnboardingListener(OfflineOnboardingItemCellRenderer.Listener listener) {
        offlineOnboardingItemCellRenderer.setListener(listener);
    }

    void setUpsellListener(UpsellItemCellRenderer.Listener listener) {
        upsellItemCellRenderer.setListener(listener);
    }

    RecentlyPlayedBucketRenderer getRecentlyPlayedBucketRenderer() {
        return recentlyPlayedBuckerRenderer;
    }
}
