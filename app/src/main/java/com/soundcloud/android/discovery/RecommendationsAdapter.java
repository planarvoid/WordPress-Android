package com.soundcloud.android.discovery;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.view.View;

@AutoFactory(allowSubclasses = true)
class RecommendationsAdapter extends RecyclerItemAdapter<Recommendation, RecommendationsViewHolder> implements PlayingTrackAware {

    private static final int RECOMMENDATION_TYPE = 0;

    public RecommendationsAdapter(Screen screen, @Provided RecommendationRendererFactory rendererFactory) {
        super(rendererFactory.create(screen));
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
