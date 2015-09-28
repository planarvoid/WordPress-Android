package com.soundcloud.android.discovery;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.ViewTypes;
import com.soundcloud.android.search.PlaylistTagsPresenter;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

class DiscoveryAdapter extends RecyclerItemAdapter<DiscoveryItem, DiscoveryAdapter.DiscoveryViewHolder> {

    static final int RECOMMENDATION_SEED_TYPE = ViewTypes.DEFAULT_VIEW_TYPE;
    static final int PLAYLIST_TAGS_TYPE = ViewTypes.DEFAULT_VIEW_TYPE + 1;
    static final int SEARCH_TYPE = ViewTypes.DEFAULT_VIEW_TYPE + 2;

    private final RecommendationItemRenderer trackRecommendationRenderer;
    private final PlaylistTagRenderer playlistTagRenderer;
    private final SearchItemRenderer searchItemRenderer;

    interface DiscoveryItemListener extends
            RecommendationItemRenderer.OnRecommendationClickListener,
            PlaylistTagsPresenter.Listener,
            SearchItemRenderer.SearchListener {
    }

    @Inject
    DiscoveryAdapter(RecommendationItemRenderer trackRecommendationRenderer, PlaylistTagRenderer playlistTagRenderer,
                     SearchItemRenderer searchItemRenderer) {
        super(new CellRendererBinding<>(ViewTypes.DEFAULT_VIEW_TYPE, trackRecommendationRenderer),
                new CellRendererBinding<>(PLAYLIST_TAGS_TYPE, playlistTagRenderer),
                new CellRendererBinding<>(SEARCH_TYPE, searchItemRenderer));
        this.trackRecommendationRenderer = trackRecommendationRenderer;
        this.playlistTagRenderer = playlistTagRenderer;
        this.searchItemRenderer = searchItemRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        switch (getItem(position).getKind()) {
            case TrackRecommendationItem:
                return RECOMMENDATION_SEED_TYPE;

            case PlaylistTagsItem:
                return PLAYLIST_TAGS_TYPE;

            case SearchItem:
                return SEARCH_TYPE;

            default:
                throw new IllegalArgumentException("Unhandled discovery item kind " + getItem(position).getKind());
        }
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

    void setDiscoveryListener(DiscoveryItemListener itemListener) {
        this.trackRecommendationRenderer.setOnRecommendationClickListener(itemListener);
        this.playlistTagRenderer.setOnTagClickListener(itemListener);
        this.searchItemRenderer.setSearchListener(itemListener);
    }
}
