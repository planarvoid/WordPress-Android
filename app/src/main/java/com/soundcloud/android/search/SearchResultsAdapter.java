package com.soundcloud.android.search;

import static com.soundcloud.android.search.SearchResultsAdapter.Kind.*;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
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

    enum Kind {
        TYPE_USER,
        TYPE_TRACK,
        TYPE_PLAYLIST,
        TYPE_PREMIUM_CONTENT,
        TYPE_UPSELL,
        TYPE_HEADER
    }

    private final SearchPremiumContentRenderer searchPremiumContentRenderer;
    private final SearchUpsellRenderer searchUpsellRenderer;

    @Inject
    SearchResultsAdapter(TrackItemRenderer trackItemRenderer,
                         PlaylistItemRenderer playlistItemRenderer,
                         FollowableUserItemRenderer userItemRenderer,
                         SearchPremiumContentRenderer searchPremiumContentRenderer,
                         SearchUpsellRenderer searchUpsellRenderer,
                         SearchResultHeaderRenderer searchResultHeaderRenderer) {
        super(new CellRendererBinding<>(TYPE_TRACK.ordinal(), trackItemRenderer),
              new CellRendererBinding<>(TYPE_PLAYLIST.ordinal(), playlistItemRenderer),
              new CellRendererBinding<>(TYPE_USER.ordinal(), userItemRenderer),
              new CellRendererBinding<>(TYPE_PREMIUM_CONTENT.ordinal(), searchPremiumContentRenderer),
              new CellRendererBinding<>(TYPE_UPSELL.ordinal(), searchUpsellRenderer),
              new CellRendererBinding<>(TYPE_HEADER.ordinal(), searchResultHeaderRenderer));
        this.searchPremiumContentRenderer = searchPremiumContentRenderer;
        this.searchUpsellRenderer = searchUpsellRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        final ListItem searchListItem = getItem(position);
        if (searchListItem instanceof SearchResultHeaderRenderer.SearchResultHeader) {
            return TYPE_HEADER.ordinal();
        } else if (searchListItem instanceof UpsellSearchableItem) {
            return TYPE_UPSELL.ordinal();
        } else if (searchListItem instanceof SearchPremiumItem) {
            return TYPE_PREMIUM_CONTENT.ordinal();
        }
        final SearchResultItem item = SearchResultItem.fromUrn(searchListItem.getUrn());
        if (item.isUser()) {
            return TYPE_USER.ordinal();
        } else if (item.isTrack()) {
            return TYPE_TRACK.ordinal();
        } else if (item.isPlaylist()) {
            return TYPE_PLAYLIST.ordinal();
        } else {
            throw new IllegalStateException("Unexpected item type in " + SearchResultsAdapter.class.getSimpleName() + " - "+searchListItem.toString());
        }
    }

    List<ListItem> getResultItems() {
        final int viewType = getBasicItemViewType(0);
        if (viewType == TYPE_UPSELL.ordinal() || viewType == TYPE_PREMIUM_CONTENT.ordinal()) {
            return getItems().subList(1, getItems().size());
        }
        return getItems();
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (int i = 0; i < items.size(); i++) {
            ListItem viewModel = items.get(i);
            final SearchResultItem item = SearchResultItem.fromUrn(viewModel.getUrn());
            if (item.isTrack()) {
                final TrackItem trackModel = (TrackItem) viewModel;
                final boolean isPlaying = trackModel.getUrn().equals(currentlyPlayingUrn);
                final TrackItem trackItem = trackModel.withPlayingState(isPlaying);
                items.set(i, trackItem);
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
