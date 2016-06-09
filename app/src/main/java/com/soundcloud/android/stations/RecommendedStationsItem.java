package com.soundcloud.android.stations;

import com.soundcloud.android.discovery.DiscoveryItem;

import java.util.List;

public class RecommendedStationsItem extends DiscoveryItem {
    final List<StationRecord> stationRecords;

    public RecommendedStationsItem(List<StationRecord> stationRecords) {
        super(Kind.RecommendedStationsItem);
        this.stationRecords = stationRecords;
    }

}
