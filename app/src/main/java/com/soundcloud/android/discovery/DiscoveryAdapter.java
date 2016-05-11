package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryItem.Kind.ChartItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.PlaylistTagsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.SearchItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.StationRecommendationItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.TrackRecommendationItem;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.search.PlaylistTagsPresenter;
import com.soundcloud.android.view.adapters.NowPlayingAdapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class DiscoveryAdapter extends RecyclerItemAdapter<DiscoveryItem, DiscoveryAdapter.DiscoveryViewHolder> implements NowPlayingAdapter {

    private final RecommendationBucketRenderer recommendationBucketRenderer;
    private final PlaylistTagRenderer playlistTagRenderer;
    private final SearchItemRenderer searchItemRenderer;

    interface DiscoveryItemListenerBucket extends
            PlaylistTagsPresenter.Listener,
            SearchItemRenderer.SearchListener {
    }

    @Inject
    @SuppressWarnings("unchecked")
    DiscoveryAdapter(RecommendationBucketRenderer recommendationBucketRenderer,
                     PlaylistTagRenderer playlistTagRenderer,
                     SearchItemRenderer searchItemRenderer,
                     RecommendedStationsBucketRenderer stationsBucketRenderer,
                     ChartsItemRenderer chartsItemRenderer) {
        super(new CellRendererBinding<>(TrackRecommendationItem.ordinal(), recommendationBucketRenderer),
                new CellRendererBinding<>(PlaylistTagsItem.ordinal(), playlistTagRenderer),
                new CellRendererBinding<>(SearchItem.ordinal(), searchItemRenderer),
                new CellRendererBinding<>(StationRecommendationItem.ordinal(), stationsBucketRenderer),
                new CellRendererBinding<>(ChartItem.ordinal(), chartsItemRenderer));
        this.recommendationBucketRenderer = recommendationBucketRenderer;
        this.playlistTagRenderer = playlistTagRenderer;
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
        public DiscoveryViewHolder(View itemView) {
            super(itemView);
        }
    }

    void setDiscoveryListener(DiscoveryItemListenerBucket itemListener) {
        this.playlistTagRenderer.setOnTagClickListener(itemListener);
        this.searchItemRenderer.setSearchListener(itemListener);
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingUrn) {
        for (DiscoveryItem discoveryItem : getItems()) {
            if (discoveryItem.getKind().equals(DiscoveryItem.Kind.TrackRecommendationItem)) {
                for (RecommendationViewModel viewModel : ((RecommendationBucket) discoveryItem).getRecommendations()) {
                    viewModel.setIsPlaying(currentlyPlayingUrn.equals(viewModel.getTrack().getUrn()));
                }
            }
        }

        notifyDataSetChanged();
    }
}
