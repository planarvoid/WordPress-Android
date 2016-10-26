package com.soundcloud.android.discovery.recommendations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import android.view.View;

class RecommendationsAdapter extends RecyclerItemAdapter<Recommendation, RecyclerItemAdapter.ViewHolder>
        implements PlayingTrackAware {

    private static final int RECOMMENDATION_TYPE = 0;
    private static final long DEFAULT_ID = -1;
    private long bucketId;

    public RecommendationsAdapter(RecommendationRenderer recommendationRenderer) {
        super(recommendationRenderer);
        bucketId = DEFAULT_ID;
    }

    boolean hasBucketItem() {
        return bucketId != DEFAULT_ID;
    }

    void setRecommendedTracksBucketItem(RecommendedTracksBucketItem recommendationBucket) {
        clear();
        bucketId = recommendationBucket.getSeedTrackLocalId();
        onNext(recommendationBucket.getRecommendations());
    }

    long bucketId() {
        return bucketId;
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingCollectionUrn) {
        for (Recommendation viewModel : getItems()) {
            viewModel.setIsPlaying(viewModel.getTrack().getUrn().equals(currentlyPlayingCollectionUrn));
        }
        notifyDataSetChanged();
    }

    @Override
    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getBasicItemViewType(int i) {
        return RECOMMENDATION_TYPE;
    }
}
