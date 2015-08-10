package com.soundcloud.android.stations;

import com.soundcloud.android.api.model.ApiStation;
import com.soundcloud.android.api.model.ApiStationInfo;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;

public class StationFixtures {
    public static ApiStation getApiStation() {
        return getApiStation(Urn.forTrackStation(123L));
    }

    public static ApiStation getApiStation(Urn station) {
        final ModelCollection<ApiTrack> tracks = new ModelCollection<>(ModelFixtures.create(ApiTrack.class, 1));
        return new ApiStation(getApiStationInfo(station), tracks);
    }

    private static ApiStationInfo getApiStationInfo(Urn station) {
        final ApiTrack seedTrack = ModelFixtures.create(ApiTrack.class);
        return new ApiStationInfo(station, "station " + System.currentTimeMillis(), "fixture-stations", seedTrack);
    }
}