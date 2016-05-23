package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryItem.Kind.ChartItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.PlaylistTagsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.SearchItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.StationRecommendationItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.TrackRecommendationItem;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.main.Screen;
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
    DiscoveryAdapter(Screen screen,
                     @Provided RecommendationBucketRendererFactory recommendationBucketRendererFactory,
                     @Provided PlaylistTagRenderer playlistTagRenderer,
                     @Provided SearchItemRenderer searchItemRenderer,
                     @Provided RecommendedStationsBucketRenderer stationsBucketRenderer,
                     @Provided ChartsItemRenderer chartsItemRenderer) {
        super(new CellRendererBinding<>(TrackRecommendationItem.ordinal(), recommendationBucketRendererFactory.create(screen, true)),
                new CellRendererBinding<>(PlaylistTagsItem.ordinal(), playlistTagRenderer),
                new CellRendererBinding<>(SearchItem.ordinal(), searchItemRenderer),
                new CellRendererBinding<>(StationRecommendationItem.ordinal(), stationsBucketRenderer),
                new CellRendererBinding<>(ChartItem.ordinal(), chartsItemRenderer));
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
            if (discoveryItem.getKind().equals(DiscoveryItem.Kind.TrackRecommendationItem)) {
                for (Recommendation viewModel : ((RecommendationBucket) discoveryItem).getRecommendations()) {
                    viewModel.setIsPlaying(currentlyPlayingUrn.equals(viewModel.getTrack().getUrn()));
                }
            }
        }

        notifyDataSetChanged();
    }
}
