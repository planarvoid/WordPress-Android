package com.soundcloud.android.olddiscovery;

import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind.ChartItem;
import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind.Empty;
import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind.NewForYouItem;
import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind.PlaylistTagsItem;
import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind.RecommendedPlaylistsItem;
import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind.RecommendedStationsItem;
import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind.RecommendedTracksFooterItem;
import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind.RecommendedTracksItem;
import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind.SearchItem;
import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind.UpsellItem;
import static com.soundcloud.android.olddiscovery.OldDiscoveryItem.Kind.WelcomeUserItem;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.olddiscovery.charts.ChartsBucketItemRenderer;
import com.soundcloud.android.olddiscovery.newforyou.NewForYouBucketRenderer;
import com.soundcloud.android.olddiscovery.recommendations.RecommendationBucketRenderer;
import com.soundcloud.android.olddiscovery.recommendations.RecommendationsFooterRenderer;
import com.soundcloud.android.olddiscovery.recommendedplaylists.RecommendedPlaylistsAdapter;
import com.soundcloud.android.olddiscovery.recommendedplaylists.RecommendedPlaylistsBucketItem;
import com.soundcloud.android.olddiscovery.recommendedplaylists.RecommendedPlaylistsBucketRenderer;
import com.soundcloud.android.olddiscovery.welcomeuser.WelcomeUserItemRenderer;
import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.search.PlaylistTagsPresenter;
import com.soundcloud.android.search.SearchItemRenderer;
import com.soundcloud.android.stations.RecommendedStationsBucketRenderer;
import com.soundcloud.android.upsell.DiscoveryUpsellItemRenderer;
import com.soundcloud.java.collections.Iterables;

import android.support.v7.widget.RecyclerView;
import android.view.View;

@AutoFactory(allowSubclasses = true)
public class OldDiscoveryAdapter extends RecyclerItemAdapter<OldDiscoveryItem, RecyclerView.ViewHolder>
        implements RecommendedPlaylistsAdapter.QueryPositionProvider {

    private final PlaylistTagRenderer playlistTagRenderer;
    private final SearchItemRenderer searchItemRenderer;
    private final WelcomeUserItemRenderer welcomeUserItemRenderer;
    private final RecommendedStationsBucketRenderer stationsBucketRenderer;
    private final DiscoveryUpsellItemRenderer discoveryUpsellItemRenderer;

    public void detach() {
        stationsBucketRenderer.detach();
    }

    public void setUpsellItemListener(DiscoveryUpsellItemRenderer.Listener listener) {
        this.discoveryUpsellItemRenderer.setListener(listener);
    }

    public interface DiscoveryItemListenerBucket extends
            PlaylistTagsPresenter.Listener,
            SearchItemRenderer.SearchListener,
            RecommendedStationsBucketRenderer.Listener,
            WelcomeUserItemRenderer.Listener {
    }

    @SuppressWarnings("unchecked")
    OldDiscoveryAdapter(RecommendationBucketRenderer recommendationBucketRenderer,
                        @Provided PlaylistTagRenderer playlistTagRenderer,
                        @Provided SearchItemRenderer searchItemRenderer,
                        @Provided RecommendedStationsBucketRenderer stationsBucketRenderer,
                        @Provided RecommendedPlaylistsBucketRenderer recommendedPlaylistsBucketRenderer,
                        @Provided ChartsBucketItemRenderer chartsBucketItemRenderer,
                        @Provided RecommendationsFooterRenderer recommendationsFooterRenderer,
                        @Provided WelcomeUserItemRenderer welcomeUserItemRenderer,
                        @Provided EmptyOldDiscoveryItemRenderer emptyOldDiscoveryItemRenderer,
                        @Provided NewForYouBucketRenderer newForYouBucketRenderer,
                        @Provided DiscoveryUpsellItemRenderer discoveryUpsellItemRenderer) {
        super(new CellRendererBinding<>(RecommendedTracksItem.ordinal(), recommendationBucketRenderer),
              new CellRendererBinding<>(PlaylistTagsItem.ordinal(), playlistTagRenderer),
              new CellRendererBinding<>(SearchItem.ordinal(), searchItemRenderer),
              new CellRendererBinding<>(RecommendedStationsItem.ordinal(), stationsBucketRenderer),
              new CellRendererBinding<>(RecommendedPlaylistsItem.ordinal(), recommendedPlaylistsBucketRenderer),
              new CellRendererBinding<>(ChartItem.ordinal(), chartsBucketItemRenderer),
              new CellRendererBinding<>(RecommendedTracksFooterItem.ordinal(), recommendationsFooterRenderer),
              new CellRendererBinding<>(WelcomeUserItem.ordinal(), welcomeUserItemRenderer),
              new CellRendererBinding<>(Empty.ordinal(), emptyOldDiscoveryItemRenderer),
              new CellRendererBinding<>(NewForYouItem.ordinal(), newForYouBucketRenderer),
              new CellRendererBinding<>(UpsellItem.ordinal(), discoveryUpsellItemRenderer)
        );
        this.playlistTagRenderer = playlistTagRenderer;
        this.stationsBucketRenderer = stationsBucketRenderer;
        this.searchItemRenderer = searchItemRenderer;
        this.welcomeUserItemRenderer = welcomeUserItemRenderer;
        this.discoveryUpsellItemRenderer = discoveryUpsellItemRenderer;
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
        for (OldDiscoveryItem oldDiscoveryItem : getItems()) {
            if (oldDiscoveryItem.getKind() == RecommendedPlaylistsItem) {
                final RecommendedPlaylistsBucketItem playlistsBucketItem = (RecommendedPlaylistsBucketItem) oldDiscoveryItem;
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

    void setItem(int position, OldDiscoveryItem item) {
        if (containsItem(item)) {
            items.set(position, item);
            notifyItemChanged(position);
        } else {
            items.add(position, item);
            notifyItemInserted(position);
        }
    }

    private boolean containsItem(OldDiscoveryItem item) {
        return findItemIndex(item.getKind()) >= 0;
    }

    private int findItemIndex(final OldDiscoveryItem.Kind kind) {
        return Iterables.indexOf(getItems(), input -> input.getKind() == kind);
    }

}
