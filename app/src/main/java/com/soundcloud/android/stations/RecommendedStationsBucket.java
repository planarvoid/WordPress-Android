package com.soundcloud.android.stations;

import com.soundcloud.android.discovery.DiscoveryItem;

import java.util.List;

class RecommendedStationsBucket extends DiscoveryItem {
    final List<StationRecord> stationRecords;

    RecommendedStationsBucket(List<StationRecord> stationRecords) {
        super(Kind.StationRecommendationItem);
        this.stationRecords = stationRecords;
    }

}
