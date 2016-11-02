package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.adapters.PlayableViewItem;

import java.util.List;

@AutoValue
public abstract class RecommendedStationsBucketItem extends DiscoveryItem implements PlayableViewItem {


    public static RecommendedStationsBucketItem create(List<StationViewModel> stations) {
        return new AutoValue_RecommendedStationsBucketItem(Kind.RecommendedStationsItem, stations);
    }

    public abstract List<StationViewModel> getStations();

    public boolean updateNowPlaying(CurrentPlayQueueItemEvent event) {
        boolean updated = false;
        final Urn collectionUrn = event.getCollectionUrn();
        for (StationViewModel viewModel : getStations()) {
            final boolean isPlaying = collectionUrn.equals(viewModel.getStation().getUrn());
            if (viewModel.isPlaying() != isPlaying) {
                viewModel.setIsPlaying(isPlaying);
                updated = true;
            }
        }
        return updated;
    }
}
