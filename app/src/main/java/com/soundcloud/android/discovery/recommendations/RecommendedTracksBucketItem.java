package com.soundcloud.android.discovery.recommendations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.adapters.PlayableViewItem;
import com.soundcloud.java.functions.Function;

import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class RecommendedTracksBucketItem extends DiscoveryItem implements PlayableViewItem {

    static Function<DiscoveryItem, List<Recommendation>> TO_RECOMMENDATIONS = input -> ((RecommendedTracksBucketItem) input).recommendations();

    @Override
    public abstract Kind getKind();

    abstract String seedTrackTitle();

    abstract Urn seedTrackUrn();

    abstract int seedTrackQueryPosition();

    abstract Urn queryUrn();

    abstract RecommendationReason recommendationReason();

    abstract long seedTrackLocalId();

    abstract List<Recommendation> recommendations();


    static RecommendedTracksBucketItem create(RecommendationSeed seed, List<Recommendation> recommendations) {
        return create(
                seed.seedTrackTitle(),
                seed.seedTrackUrn(),
                seed.queryPosition(),
                seed.queryUrn(),
                seed.reason(),
                seed.seedTrackLocalId(),
                recommendations);
    }

    public static RecommendedTracksBucketItem create(String seedTrackTitle,
                                                     Urn seedTrackUrn,
                                                     int seedTrackQueryPosition,
                                                     Urn queryUrn,
                                                     RecommendationReason recommendationReason, long seedTrackLocalId, List<Recommendation> recommendations) {
        return builder()
                .getKind(Kind.RecommendedTracksItem)
                .seedTrackTitle(seedTrackTitle)
                .seedTrackUrn(seedTrackUrn)
                .seedTrackQueryPosition(seedTrackQueryPosition)
                .queryUrn(queryUrn)
                .recommendationReason(recommendationReason)
                .seedTrackLocalId(seedTrackLocalId)
                .recommendations(recommendations)
                .build();
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_RecommendedTracksBucketItem.Builder();
    }

    public RecommendedTracksBucketItem updateNowPlaying(CurrentPlayQueueItemEvent event) {
        final Urn nowPlayingUrn = event.getCurrentPlayQueueItem().getUrnOrNotSet();
        final List<Recommendation> recommendations = recommendations();
        final List<Recommendation> updatedRecommendations = new ArrayList<>(recommendations.size());
        for (Recommendation viewModel : recommendations) {
            final boolean isPlaying = nowPlayingUrn.equals(viewModel.getTrackUrn());
            if (viewModel.isPlaying() != isPlaying) {
                final Recommendation updatedRecommendation = viewModel.toBuilder().setPlaying(isPlaying).build();
                updatedRecommendations.add(updatedRecommendation);
            } else {
                updatedRecommendations.add(viewModel);
            }
        }
        return toBuilder().recommendations(updatedRecommendations).build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder getKind(Kind getKind);

        public abstract Builder seedTrackTitle(String seedTrackTitle);

        public abstract Builder seedTrackUrn(Urn seedTrackUrn);

        public abstract Builder seedTrackQueryPosition(int seedTrackQueryPosition);

        public abstract Builder queryUrn(Urn queryUrn);

        public abstract Builder recommendationReason(RecommendationReason recommendationReason);

        public abstract Builder seedTrackLocalId(long seedTrackLocalId);

        public abstract Builder recommendations(List<Recommendation> recommendations);

        public abstract RecommendedTracksBucketItem build();
    }
}
