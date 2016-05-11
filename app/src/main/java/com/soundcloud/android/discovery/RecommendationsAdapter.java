package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.adapters.NowPlayingAdapter;

import android.view.View;

import javax.inject.Inject;

class RecommendationsAdapter extends RecyclerItemAdapter<RecommendationViewModel, RecommendationsViewHolder> implements NowPlayingAdapter {
    private static final int RECOMMENDATION_TYPE = 0;

    @Inject
    public RecommendationsAdapter(RecommendationRenderer renderer) {
        super(renderer);
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingCollectionUrn) {
        for (RecommendationViewModel viewModel : getItems()) {
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
