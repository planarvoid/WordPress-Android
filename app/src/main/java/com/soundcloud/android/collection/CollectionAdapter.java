package com.soundcloud.android.collection;

import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketRenderer;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;

import android.view.View;

import javax.inject.Inject;

public class CollectionAdapter extends PagingRecyclerItemAdapter<CollectionItem, RecyclerItemAdapter.ViewHolder>
        implements CollectionHeaderRenderer.OnSettingsClickListener, CollectionPlaylistRemoveFilterRenderer.OnRemoveFilterListener {

    private final OnboardingItemCellRenderer onboardingItemCellRenderer;
    private Listener listener;

    interface Listener {
        void onPlaylistSettingsClicked(View view);

        void onRemoveFilterClicked();
    }

    @Inject
    public CollectionAdapter(OnboardingItemCellRenderer onboardingItemCellRenderer,
                             CollectionPreviewRenderer collectionPreviewRenderer,
                             CollectionHeaderRenderer headerRenderer,
                             CollectionPlaylistRemoveFilterRenderer removeFilterRenderer,
                             CollectionEmptyPlaylistsRenderer emptyPlaylistsRenderer,
                             CollectionPlaylistItemRenderer playlistRenderer,
                             CollectionTrackItemRenderer trackRenderer,
                             CollectionViewAllRenderer viewAllRenderer,
                             RecentlyPlayedBucketRenderer recentlyPlayedBucketRenderer) {
        super(
                new CellRendererBinding<>(CollectionItem.TYPE_ONBOARDING, onboardingItemCellRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PREVIEW, collectionPreviewRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_HEADER, headerRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PLAYLIST_REMOVE_FILTER, removeFilterRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PLAYLIST_EMPTY, emptyPlaylistsRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PLAYLIST_ITEM, playlistRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_TRACK_ITEM, trackRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_VIEW_ALL, viewAllRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_RECENTLY_PLAYED_BUCKET, recentlyPlayedBucketRenderer));
        this.onboardingItemCellRenderer = onboardingItemCellRenderer;

        headerRenderer.setOnSettingsClickListener(this);
        removeFilterRenderer.setOnRemoveFilterClickListener(this);
    }

    @Override
    public void onSettingsClicked(View view) {
        if (listener != null) {
            listener.onPlaylistSettingsClicked(view);
        }
    }

    @Override
    public void onRemoveFilter() {
        if (listener != null) {
            listener.onRemoveFilterClicked();
        }
    }

    void setOnboardingListener(OnboardingItemCellRenderer.Listener listener) {
        onboardingItemCellRenderer.setListener(listener);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getType();
    }
}
