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
import com.soundcloud.android.view.adapters.PlayingTrackAware;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class SearchResultsAdapter
        extends PagingRecyclerItemAdapter<ListItem, RecyclerView.ViewHolder>
        implements PlayingTrackAware {

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
        final SearchResultItem item = SearchResultItem.fromUrn(getItem(position).getUrn());
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

    List<ListItem> getResultItems() {
        final int viewType = getBasicItemViewType(0);
        if (viewType == TYPE_UPSELL || viewType == TYPE_PREMIUM_CONTENT) {
            return getItems().subList(1, getItems().size());
        }
        return getItems();
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (ListItem viewModel : getItems()) {
            final SearchResultItem item = SearchResultItem.fromUrn(viewModel.getUrn());
            if (item.isTrack()) {
                final TrackItem trackModel = (TrackItem) viewModel;
                trackModel.setIsPlaying(trackModel.getUrn().equals(currentlyPlayingUrn));
            } else if (item.isPremiumContent()) {
                ((SearchPremiumItem) viewModel).setTrackIsPlaying(currentlyPlayingUrn);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    void setPremiumContentListener(OnPremiumContentClickListener listener) {
        this.searchPremiumContentRenderer.setPremiumContentListener(listener);
    }

    void setUpsellListener(OnUpsellClickListener listener) {
        this.searchUpsellRenderer.setUpsellClickListener(listener);
    }
}
