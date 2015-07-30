package com.soundcloud.android.discovery;

import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.ViewTypes;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class DiscoveryAdapter extends RecyclerItemAdapter<RecommendationItem, DiscoveryAdapter.DiscoveryViewHolder> {

    private final RecommendationItemRenderer itemRenderer;

    @Inject
    public DiscoveryAdapter(RecommendationItemRenderer itemRenderer) {
        super(itemRenderer);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public int getBasicItemViewType(int position) {
        return ViewTypes.DEFAULT_VIEW_TYPE;
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

    void setOnRecommendationClickListener(@NonNull RecommendationItemRenderer.OnRecommendationClickListener clickListener) {
        this.itemRenderer.setOnRecommendationClickListener(clickListener);
    }
}
