package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

import java.util.List;

@AutoValue
public abstract class RecommendedStationsBucketItem extends DiscoveryItem implements PlayingTrackAware {


    public static RecommendedStationsBucketItem create(List<StationViewModel> stations) {
        return new AutoValue_RecommendedStationsBucketItem(Kind.RecommendedStationsItem, stations);
    }

    public abstract List<StationViewModel> getStations();

    public void updateNowPlaying(Urn nowPlaying) {
        for (StationViewModel viewModel : getStations()) {
            viewModel.setIsPlaying(nowPlaying.equals(viewModel.getStation().getUrn()));
        }
    }
}
