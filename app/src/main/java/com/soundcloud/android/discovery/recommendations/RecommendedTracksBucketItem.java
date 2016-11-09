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

    static RecommendedTracksBucketItem create(PropertySet source, List<Recommendation> recommendations) {
        return new AutoValue_RecommendedTracksBucketItem(Kind.RecommendedTracksItem, source, recommendations);
    }

    String getSeedTrackTitle() {
        return getSource().get(RecommendationProperty.SEED_TRACK_TITLE);
    }

    Urn getSeedTrackUrn() {
        return getSource().get(RecommendationProperty.SEED_TRACK_URN);
    }

    int getSeedTrackQueryPosition() {
        return getSource().get(RecommendationProperty.QUERY_POSITION);
    }

    Urn getQueryUrn() {
        return getSource().get(RecommendationProperty.QUERY_URN);
    }

    abstract PropertySet getSource();

    abstract List<Recommendation> getRecommendations();

    RecommendationReason getRecommendationReason() {
        return getSource().get(RecommendationProperty.REASON);
    }

    long getSeedTrackLocalId() {
        return getSource().get(RecommendationProperty.SEED_TRACK_LOCAL_ID);
    }

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

    static Function<DiscoveryItem, List<Recommendation>> TO_RECOMMENDATIONS = new Function<DiscoveryItem, List<Recommendation>>() {
        @Override
        public List<Recommendation> apply(DiscoveryItem input) {
            return ((RecommendedTracksBucketItem) input).getRecommendations();
        }
    };
}
