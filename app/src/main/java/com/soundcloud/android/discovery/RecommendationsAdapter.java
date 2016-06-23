package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.view.View;

class RecommendationsAdapter extends RecyclerItemAdapter<Recommendation, RecommendationsViewHolder>
        implements PlayingTrackAware {

    private static final int RECOMMENDATION_TYPE = 0;

    public RecommendationsAdapter(RecommendationRenderer recommendationRenderer) {
        super(recommendationRenderer);
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingCollectionUrn) {
        for (Recommendation viewModel : getItems()) {
            viewModel.setIsPlaying(viewModel.getTrack().getUrn().equals(currentlyPlayingCollectionUrn));
        }
        notifyDataSetChanged();
    }

    @Override
    protected RecommendationsViewHolder createViewHolder(View view) {
        return new RecommendationsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecommendationsViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
    }

    @Override
    public int getBasicItemViewType(int i) {
        return RECOMMENDATION_TYPE;
    }
}
