package com.soundcloud.android.discovery;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.ViewTypes;
import com.soundcloud.android.search.PlaylistTagsPresenter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class DiscoveryAdapter extends RecyclerItemAdapter<DiscoveryItem, DiscoveryAdapter.DiscoveryViewHolder> {

    public static final int RECOMMENDATION_SEED_TYPE = ViewTypes.DEFAULT_VIEW_TYPE;
    public static final int PLAYLIST_TAGS_TYPE = ViewTypes.DEFAULT_VIEW_TYPE + 1;

    private final RecommendationItemRenderer trackRecommendationRenderer;
    private final PlaylistTagRenderer playlistTagRenderer;

    public interface DiscoveryItemListener extends
            RecommendationItemRenderer.OnRecommendationClickListener, PlaylistTagsPresenter.Listener {
    }

    @Inject
    public DiscoveryAdapter(RecommendationItemRenderer trackRecommendationRenderer, PlaylistTagRenderer playlistTagRenderer) {
        super(new CellRendererBinding<>(ViewTypes.DEFAULT_VIEW_TYPE, trackRecommendationRenderer),
                new CellRendererBinding<>(PLAYLIST_TAGS_TYPE, playlistTagRenderer));
        this.trackRecommendationRenderer = trackRecommendationRenderer;
        this.playlistTagRenderer = playlistTagRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        switch (getItem(position).getKind()) {
            case TrackRecommendationItem:
                return RECOMMENDATION_SEED_TYPE;

            case PlaylistTagsItem:
                return PLAYLIST_TAGS_TYPE;

            default:
                throw new IllegalArgumentException("Unhandled discovery item kind " + getItem(position).getKind());
        }
    }

    @Override
    protected DiscoveryViewHolder createViewHolder(View itemView) {
        return new DiscoveryViewHolder(itemView);
    }

    public static class DiscoveryViewHolder extends RecyclerView.ViewHolder {
        public DiscoveryViewHolder(View itemView) {
            super(itemView);
        }
    }

    void setOnRecommendationClickListener(@NonNull DiscoveryItemListener itemListener) {
        this.trackRecommendationRenderer.setOnRecommendationClickListener(itemListener);
        this.playlistTagRenderer.setOnTagClickListener(itemListener);
    }
}
