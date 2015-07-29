package com.soundcloud.android.recommendations;

import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.ViewTypes;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class RecommendationsAdapter extends RecyclerItemAdapter<RecommendationItem, RecommendationsAdapter.RecommendationViewHolder> {

    @Inject
    public RecommendationsAdapter(RecommendationItemRenderer itemRenderer) {
        super(itemRenderer);
    }

    @Override
    public int getBasicItemViewType(int position) {
        return ViewTypes.DEFAULT_VIEW_TYPE;
    }

    @Override
    protected RecommendationViewHolder createViewHolder(View itemView) {
        return new RecommendationViewHolder(itemView);
    }

    public static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        public RecommendationViewHolder(View itemView) {
            super(itemView);
        }
    }

}
