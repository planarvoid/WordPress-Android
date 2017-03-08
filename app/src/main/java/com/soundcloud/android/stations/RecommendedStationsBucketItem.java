package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.adapters.PlayableViewItem;

import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class RecommendedStationsBucketItem extends DiscoveryItem implements PlayableViewItem {


    public static RecommendedStationsBucketItem create(List<StationViewModel> stations) {
        return new AutoValue_RecommendedStationsBucketItem(Kind.RecommendedStationsItem, stations);
    }

    public abstract List<StationViewModel> getStations();

    public RecommendedStationsBucketItem updateNowPlaying(CurrentPlayQueueItemEvent event) {
        final Urn collectionUrn = event.getCollectionUrn();
        final List<StationViewModel> stations = getStations();
        final List<StationViewModel> updatedStations = new ArrayList<>(stations.size());
        for (StationViewModel viewModel : stations) {
            final StationViewModel updatedModel;

            final boolean isPlaying = collectionUrn.equals(viewModel.getStation().getUrn());
            if (viewModel.isPlaying() != isPlaying) {
                updatedModel = StationViewModel.create(viewModel.getStation(), isPlaying);
            } else {
                updatedModel = viewModel;
            }
            updatedStations.add(updatedModel);
        }
        return RecommendedStationsBucketItem.create(updatedStations);
    }
}
