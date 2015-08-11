package com.soundcloud.android.discovery;

import com.soundcloud.android.presentation.CellRendererBinding;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.ViewTypes;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import javax.inject.Inject;

public class RecommendedTracksAdapter extends RecyclerItemAdapter<RecommendedTrackItem, RecommendedTracksAdapter.RecommendationsViewHolder> {

    @Inject
    RecommendedTracksAdapter(RecommendedTrackItemRenderer recommendedTrackItemRenderer) {
        super(new CellRendererBinding<>(ViewTypes.DEFAULT_VIEW_TYPE, recommendedTrackItemRenderer));
    }

    @Override
    protected RecommendationsViewHolder createViewHolder(View itemView) {
        return new RecommendationsViewHolder(itemView);
    }

    @Override
    public int getBasicItemViewType(int i) {
        return ViewTypes.DEFAULT_VIEW_TYPE;
    }

    public static class RecommendationsViewHolder extends RecyclerView.ViewHolder {
        public RecommendationsViewHolder(View itemView) {
            super(itemView);
        }
    }
}
