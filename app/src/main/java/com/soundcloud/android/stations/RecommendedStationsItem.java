package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.DiscoveryItem;

import java.util.List;

@AutoValue
public abstract class RecommendedStationsItem extends DiscoveryItem {

    protected RecommendedStationsItem() {
        super(Kind.RecommendedStationsItem);
    }

    public static RecommendedStationsItem create(List<StationRecord> stationRecords) {
        return new AutoValue_RecommendedStationsItem(stationRecords);
    }

    public abstract List<StationRecord> getStationRecords();

}
