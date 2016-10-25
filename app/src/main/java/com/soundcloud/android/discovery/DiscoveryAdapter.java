package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryItem.Kind.ChartItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.Empty;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.PlaylistTagsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedStationsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedTracksFooterItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedTracksItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.SearchItem;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.discovery.charts.ChartsBucketItemRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationBucketRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationsFooterRenderer;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.search.PlaylistTagsPresenter;
import com.soundcloud.android.stations.RecommendedStationsBucketRenderer;
import com.soundcloud.android.view.adapters.PlayingTrackAware;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory(allowSubclasses = true)
public class DiscoveryAdapter extends RecyclerItemAdapter<DiscoveryItem, DiscoveryAdapter.DiscoveryViewHolder>
        implements PlayingTrackAware {

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
                     @Provided ChartsBucketItemRenderer chartsBucketItemRenderer,
                     @Provided RecommendationsFooterRenderer recommendationsFooterRenderer,
                     @Provided EmptyDiscoveryItemRenderer emptyDiscoveryItemRenderer) {
        super(new CellRendererBinding<>(RecommendedTracksItem.ordinal(), recommendationBucketRenderer),
              new CellRendererBinding<>(PlaylistTagsItem.ordinal(), playlistTagRenderer),
              new CellRendererBinding<>(SearchItem.ordinal(), searchItemRenderer),
              new CellRendererBinding<>(RecommendedStationsItem.ordinal(), stationsBucketRenderer),
              new CellRendererBinding<>(ChartItem.ordinal(), chartsBucketItemRenderer),
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
    public void updateNowPlaying(Urn playingUrn) {
        for (int position = 0; position < getItemCount(); position++) {
            DiscoveryItem discoveryItem = getItem(position);
            if (RecommendedTracksItem.equals(discoveryItem.getKind())) {
                ((PlayingTrackAware) discoveryItem).updateNowPlaying(playingUrn);
                notifyItemChanged(position);
            }
        }
    }

    void setItem(int position, DiscoveryItem item) {
        if (containsItem(item)) {
            items.set(position, item);
            notifyItemChanged(position);
        } else {
            items.add(position, item);
            notifyItemInserted(position);
        }
    }

    private boolean containsItem(DiscoveryItem item) {
        return findItemIndex(item.getKind()) >= 0;
    }

    private int findItemIndex(final DiscoveryItem.Kind kind) {
        return Iterables.indexOf(getItems(), new Predicate<DiscoveryItem>() {
            @Override
            public boolean apply(DiscoveryItem input) {
                return input.getKind() == kind;
            }
        });
    }

    void updateNowPlayingWithCollection(Urn collectionUrn, Urn trackUrn) {
        for (int position = 0; position < getItemCount(); position++) {
            DiscoveryItem discoveryItem = getItem(position);
            if (discoveryItem instanceof PlayingTrackAware) {
                final PlayingTrackAware playingTrackAware = (PlayingTrackAware) discoveryItem;
                if (RecommendedTracksItem.equals(discoveryItem.getKind())) {
                    playingTrackAware.updateNowPlaying(trackUrn);
                    notifyItemChanged(position);
                }
                if (RecommendedStationsItem.equals(discoveryItem.getKind())) {
                    playingTrackAware.updateNowPlaying(collectionUrn);
                    stationsBucketRenderer.notifyAdapter();
                }
            }
        }
    }

}
