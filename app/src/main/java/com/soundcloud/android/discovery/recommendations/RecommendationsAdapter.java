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
        bucketId = recommendationBucket.seedTrackLocalId();
        onNext(recommendationBucket.recommendations());
    }

    long bucketId() {
        return bucketId;
    }

    @Override
    public void updateNowPlaying(Urn currentlyPlayingCollectionUrn) {
        for (int i = 0; i < items.size(); i++) {
            final Recommendation viewModel = items.get(i);

            final boolean isPlaying = viewModel.getTrack().getUrn().equals(currentlyPlayingCollectionUrn);
            if (isPlaying != viewModel.isPlaying()) {
                final Recommendation updatedViewModel = viewModel.toBuilder().setPlaying(isPlaying).build();
                items.set(i, updatedViewModel);
                notifyItemChanged(i);
            }
        }
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
