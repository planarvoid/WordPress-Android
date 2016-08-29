package com.soundcloud.android.collection;

import com.soundcloud.android.collection.playhistory.PlayHistoryBucketRenderer;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedBucketRenderer;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.tracks.TrackItemRenderer;

import android.view.View;

import javax.inject.Inject;

public class CollectionAdapter extends PagingRecyclerItemAdapter<CollectionItem, RecyclerItemAdapter.ViewHolder>
        implements CollectionPlaylistHeaderRenderer.OnSettingsClickListener, CollectionPlaylistRemoveFilterRenderer.OnRemoveFilterListener {

    private final OnboardingItemCellRenderer onboardingItemCellRenderer;
    private final PlayHistoryBucketRenderer playHistoryBucketRenderer;
    private Listener listener;

    interface Listener {
        void onPlaylistSettingsClicked(View view);

        void onRemoveFilterClicked();
    }

    @Inject
    public CollectionAdapter(OnboardingItemCellRenderer onboardingItemCellRenderer,
                             CollectionPreviewRenderer collectionPreviewRenderer,
                             CollectionPlaylistHeaderRenderer headerRenderer,
                             CollectionPlaylistRemoveFilterRenderer removeFilterRenderer,
                             CollectionEmptyPlaylistsRenderer emptyPlaylistsRenderer,
                             CollectionPlaylistItemRenderer playlistRenderer,
                             RecentlyPlayedBucketRenderer recentlyPlayedBucketRenderer,
                             PlayHistoryBucketRenderer playHistoryBucketRenderer) {
        super(
                new CellRendererBinding<>(CollectionItem.TYPE_ONBOARDING, onboardingItemCellRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PREVIEW, collectionPreviewRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PLAYLIST_HEADER, headerRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PLAYLIST_REMOVE_FILTER, removeFilterRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PLAYLIST_EMPTY, emptyPlaylistsRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PLAYLIST_ITEM, playlistRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_RECENTLY_PLAYED_BUCKET, recentlyPlayedBucketRenderer),
                new CellRendererBinding<>(CollectionItem.TYPE_PLAY_HISTORY_BUCKET, playHistoryBucketRenderer));
        this.onboardingItemCellRenderer = onboardingItemCellRenderer;
        this.playHistoryBucketRenderer = playHistoryBucketRenderer;

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

    public void setTrackClickListener(TrackItemRenderer.Listener listener) {
        playHistoryBucketRenderer.setTrackClickListener(listener);
    }
}
