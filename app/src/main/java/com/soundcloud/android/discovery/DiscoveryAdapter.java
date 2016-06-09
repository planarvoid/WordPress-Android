package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryItem.Kind.ChartItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.Empty;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.PlaylistTagsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.SearchItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedStationsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedTracksItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedTracksFooterItem;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.search.PlaylistTagsPresenter;
import com.soundcloud.android.stations.RecommendedStationsBucketRenderer;
import com.soundcloud.android.view.adapters.NowPlayingAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory(allowSubclasses = true)
class DiscoveryAdapter extends RecyclerItemAdapter<DiscoveryItem, DiscoveryAdapter.DiscoveryViewHolder> implements NowPlayingAdapter {

    private final PlaylistTagRenderer playlistTagRenderer;
    private final SearchItemRenderer searchItemRenderer;
    private final RecommendedStationsBucketRenderer stationsBucketRenderer;

    interface DiscoveryItemListenerBucket extends
            PlaylistTagsPresenter.Listener,
            SearchItemRenderer.SearchListener,
            RecommendedStationsBucketRenderer.Listener {
    }

    @SuppressWarnings("unchecked")
    DiscoveryAdapter(RecommendationBucketRenderer recommendationBucketRenderer,
                     @Provided PlaylistTagRenderer playlistTagRenderer,
                     @Provided SearchItemRenderer searchItemRenderer,
                     @Provided RecommendedStationsBucketRenderer stationsBucketRenderer,
                     @Provided ChartsItemRenderer chartsItemRenderer,
                     @Provided RecommendationsFooterRenderer recommendationsFooterRenderer,
                     @Provided EmptyDiscoveryItemRenderer emptyDiscoveryItemRenderer) {
        super(new CellRendererBinding<>(RecommendedTracksItem.ordinal(), recommendationBucketRenderer),
                new CellRendererBinding<>(PlaylistTagsItem.ordinal(), playlistTagRenderer),
                new CellRendererBinding<>(SearchItem.ordinal(), searchItemRenderer),
                new CellRendererBinding<>(RecommendedStationsItem.ordinal(), stationsBucketRenderer),
                new CellRendererBinding<>(ChartItem.ordinal(), chartsItemRenderer),
                new CellRendererBinding<>(RecommendedTracksFooterItem.ordinal(), recommendationsFooterRenderer),
                new CellRendererBinding<>(Empty.ordinal(), emptyDiscoveryItemRenderer)
        );
        this.playlistTagRenderer = playlistTagRenderer;
        this.stationsBucketRenderer = stationsBucketRenderer;
        this.searchItemRenderer = searchItemRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getKind().ordinal();
    }

    @Override
    protected DiscoveryViewHolder createViewHolder(View itemView) {
        return new DiscoveryViewHolder(itemView);
    }


    static class DiscoveryViewHolder extends RecyclerView.ViewHolder {
        DiscoveryViewHolder(View itemView) {
            super(itemView);
        }
    }

    void setDiscoveryListener(DiscoveryItemListenerBucket itemListener) {
        this.playlistTagRenderer.setOnTagClickListener(itemListener);
        this.searchItemRenderer.setSearchListener(itemListener);
        this.stationsBucketRenderer.setListener(itemListener);
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (DiscoveryItem discoveryItem : getItems()) {
            if (discoveryItem.getKind().equals(DiscoveryItem.Kind.RecommendedTracksItem)) {
                for (Recommendation viewModel : ((RecommendedTracksItem) discoveryItem).getRecommendations()) {
                    viewModel.setIsPlaying(currentlyPlayingUrn.equals(viewModel.getTrack().getUrn()));
                }
            }
        }
        notifyDataSetChanged();
    }

}
