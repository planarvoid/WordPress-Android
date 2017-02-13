package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryItem.Kind.ChartItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.Empty;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.NewForYouItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.PlaylistTagsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedPlaylistsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedStationsItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedTracksFooterItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.RecommendedTracksItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.SearchItem;
import static com.soundcloud.android.discovery.DiscoveryItem.Kind.WelcomeUserItem;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.discovery.charts.ChartsBucketItemRenderer;
import com.soundcloud.android.discovery.newforyou.NewForYouBucketRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationBucketRenderer;
import com.soundcloud.android.discovery.recommendations.RecommendationsFooterRenderer;
import com.soundcloud.android.discovery.recommendedplaylists.RecommendedPlaylistsAdapter;
import com.soundcloud.android.discovery.recommendedplaylists.RecommendedPlaylistsBucketItem;
import com.soundcloud.android.discovery.recommendedplaylists.RecommendedPlaylistsBucketRenderer;
import com.soundcloud.android.discovery.welcomeuser.WelcomeUserItemRenderer;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.search.PlaylistTagsPresenter;
import com.soundcloud.android.stations.RecommendedStationsBucketRenderer;
import com.soundcloud.java.collections.Iterables;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory(allowSubclasses = true)
public class DiscoveryAdapter extends RecyclerItemAdapter<DiscoveryItem, RecyclerView.ViewHolder>
        implements RecommendedPlaylistsAdapter.QueryPositionProvider {

    private final PlaylistTagRenderer playlistTagRenderer;
    private final SearchItemRenderer searchItemRenderer;
    private final WelcomeUserItemRenderer welcomeUserItemRenderer;
    private final RecommendedStationsBucketRenderer stationsBucketRenderer;

    public void detach() {
        stationsBucketRenderer.detach();
    }

    public interface DiscoveryItemListenerBucket extends
            PlaylistTagsPresenter.Listener,
            SearchItemRenderer.SearchListener,
            RecommendedStationsBucketRenderer.Listener,
            WelcomeUserItemRenderer.Listener {
    }

    @SuppressWarnings("unchecked")
    DiscoveryAdapter(RecommendationBucketRenderer recommendationBucketRenderer,
                     @Provided PlaylistTagRenderer playlistTagRenderer,
                     @Provided SearchItemRenderer searchItemRenderer,
                     @Provided RecommendedStationsBucketRenderer stationsBucketRenderer,
                     @Provided RecommendedPlaylistsBucketRenderer recommendedPlaylistsBucketRenderer,
                     @Provided ChartsBucketItemRenderer chartsBucketItemRenderer,
                     @Provided RecommendationsFooterRenderer recommendationsFooterRenderer,
                     @Provided WelcomeUserItemRenderer welcomeUserItemRenderer,
                     @Provided EmptyDiscoveryItemRenderer emptyDiscoveryItemRenderer,
                     @Provided NewForYouBucketRenderer newForYouBucketRenderer) {
        super(new CellRendererBinding<>(RecommendedTracksItem.ordinal(), recommendationBucketRenderer),
              new CellRendererBinding<>(PlaylistTagsItem.ordinal(), playlistTagRenderer),
              new CellRendererBinding<>(SearchItem.ordinal(), searchItemRenderer),
              new CellRendererBinding<>(RecommendedStationsItem.ordinal(), stationsBucketRenderer),
              new CellRendererBinding<>(RecommendedPlaylistsItem.ordinal(), recommendedPlaylistsBucketRenderer),
              new CellRendererBinding<>(ChartItem.ordinal(), chartsBucketItemRenderer),
              new CellRendererBinding<>(RecommendedTracksFooterItem.ordinal(), recommendationsFooterRenderer),
              new CellRendererBinding<>(WelcomeUserItem.ordinal(), welcomeUserItemRenderer),
              new CellRendererBinding<>(Empty.ordinal(), emptyDiscoveryItemRenderer),
              new CellRendererBinding<>(NewForYouItem.ordinal(), newForYouBucketRenderer)
        );
        this.playlistTagRenderer = playlistTagRenderer;
        this.stationsBucketRenderer = stationsBucketRenderer;
        this.searchItemRenderer = searchItemRenderer;
        this.welcomeUserItemRenderer = welcomeUserItemRenderer;
        recommendedPlaylistsBucketRenderer.setQueryPositionProvider(this);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return getItem(position).getKind().ordinal();
    }

    @Override
    protected ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int queryPosition(String bucketKey, int bucketPosition) {
        int queryPosition = bucketPosition;
        for (DiscoveryItem discoveryItem : getItems()) {
            if (discoveryItem.getKind() == RecommendedPlaylistsItem) {
                final RecommendedPlaylistsBucketItem playlistsBucketItem = (RecommendedPlaylistsBucketItem) discoveryItem;
                if (!playlistsBucketItem.key().equals(bucketKey)) {
                    queryPosition += playlistsBucketItem.playlists().size();
                }
            }
        }
        return queryPosition;
    }

    void setDiscoveryListener(DiscoveryItemListenerBucket itemListener) {
        playlistTagRenderer.setOnTagClickListener(itemListener);
        searchItemRenderer.setSearchListener(itemListener);
        stationsBucketRenderer.setListener(itemListener);
        welcomeUserItemRenderer.setListener(itemListener);
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
        return Iterables.indexOf(getItems(), input -> input.getKind() == kind);
    }
}
