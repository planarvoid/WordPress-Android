package com.soundcloud.android.search;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.ViewTypes;
import com.soundcloud.android.search.SearchPremiumContentRenderer.OnPremiumContentClickListener;
import com.soundcloud.android.search.SearchUpsellRenderer.OnUpsellClickListener;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.FollowableUserItemRenderer;
import com.soundcloud.android.view.adapters.NowPlayingAdapter;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class SearchResultsAdapter
        extends PagingRecyclerItemAdapter<ListItem, SearchResultsAdapter.SearchViewHolder>
        implements NowPlayingAdapter {

    static final int TYPE_USER = ViewTypes.DEFAULT_VIEW_TYPE;
    static final int TYPE_TRACK = ViewTypes.DEFAULT_VIEW_TYPE + 1;
    static final int TYPE_PLAYLIST = ViewTypes.DEFAULT_VIEW_TYPE + 2;
    static final int TYPE_PREMIUM_CONTENT = ViewTypes.DEFAULT_VIEW_TYPE + 3;
    static final int TYPE_UPSELL = ViewTypes.DEFAULT_VIEW_TYPE + 4;

    private final SearchPremiumContentRenderer searchPremiumContentRenderer;
    private final SearchUpsellRenderer searchUpsellRenderer;

    @Inject
    SearchResultsAdapter(TrackItemRenderer trackItemRenderer,
                         PlaylistItemRenderer playlistItemRenderer,
                         FollowableUserItemRenderer userItemRenderer,
                         SearchPremiumContentRenderer searchPremiumContentRenderer,
                         SearchUpsellRenderer searchUpsellRenderer) {
        super(new CellRendererBinding<>(TYPE_TRACK, trackItemRenderer),
                new CellRendererBinding<>(TYPE_PLAYLIST, playlistItemRenderer),
                new CellRendererBinding<>(TYPE_USER, userItemRenderer),
                new CellRendererBinding<>(TYPE_PREMIUM_CONTENT, searchPremiumContentRenderer),
                new CellRendererBinding<>(TYPE_UPSELL, searchUpsellRenderer));
        this.searchPremiumContentRenderer = searchPremiumContentRenderer;
        this.searchUpsellRenderer = searchUpsellRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        final SearchItem item = SearchItem.fromUrn(getItem(position).getEntityUrn());
        if (item.isUser()) {
            return TYPE_USER;
        } else if (item.isTrack()) {
            return TYPE_TRACK;
        } else if (item.isPlaylist()) {
            return TYPE_PLAYLIST;
        } else if (item.isPremiumContent()) {
            return TYPE_PREMIUM_CONTENT;
        } else if (item.isUpsell()) {
            return TYPE_UPSELL;
        } else {
            throw new IllegalStateException("Unexpected item type in " + SearchResultsAdapter.class.getSimpleName());
        }
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (ListItem viewModel : getItems()) {
            if (viewModel instanceof TrackItem) {
                final TrackItem trackModel = (TrackItem) viewModel;
                trackModel.setIsPlaying(trackModel.getEntityUrn().equals(currentlyPlayingUrn));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    protected SearchViewHolder createViewHolder(View itemView) {
        return new SearchViewHolder(itemView);
    }

    static class SearchViewHolder extends RecyclerView.ViewHolder {
        public SearchViewHolder(View itemView) {
            super(itemView);
        }
    }

    void setPremiumContentListener(OnPremiumContentClickListener listener) {
        this.searchPremiumContentRenderer.setPremiumContentListener(listener);
    }

    void setUpsellListener(OnUpsellClickListener listener) {
        this.searchUpsellRenderer.setUpsellClickListener(listener);
    }
}
