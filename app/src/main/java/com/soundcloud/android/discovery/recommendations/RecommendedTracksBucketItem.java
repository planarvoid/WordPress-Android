package com.soundcloud.android.discovery.recommendations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.adapters.PlayableViewItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;

import java.util.List;

@AutoValue
public abstract class RecommendedTracksBucketItem extends DiscoveryItem implements PlayableViewItem {

    static RecommendedTracksBucketItem create(RecommendationSeed seed, List<Recommendation> recommendations) {

        return new AutoValue_RecommendedTracksBucketItem(Kind.RecommendedTracksItem,
                                                         seed.seedTrackTitle(),
                                                         seed.seedTrackUrn(),
                                                         seed.queryPosition(),
                                                         seed.queryUrn(),
                                                         seed.reason(),
                                                         seed.seedTrackLocalId(),
                                                         recommendations);
    }

    abstract String getSeedTrackTitle();

    abstract Urn getSeedTrackUrn();

    abstract int getSeedTrackQueryPosition();

    abstract Urn getQueryUrn();

    abstract RecommendationReason getRecommendationReason();

    abstract long getSeedTrackLocalId();

    abstract List<Recommendation> getRecommendations();

    public boolean updateNowPlaying(CurrentPlayQueueItemEvent event) {
        final Urn nowPlayingUrn = event.getCurrentPlayQueueItem().getUrnOrNotSet();
        boolean updated = false;
        for (Recommendation viewModel : getRecommendations()) {
            final boolean isPlaying = nowPlayingUrn.equals(viewModel.getTrackUrn());
            if (viewModel.isPlaying() != isPlaying) {
                viewModel.setIsPlaying(isPlaying);
                updated = true;
            }
        }
        return updated;
    }

    static Function<DiscoveryItem, List<Recommendation>> TO_RECOMMENDATIONS = input -> ((RecommendedTracksBucketItem) input).getRecommendations();
}
