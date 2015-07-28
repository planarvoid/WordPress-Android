package com.soundcloud.android.stations;

import com.soundcloud.android.api.model.ApiStation;
import com.soundcloud.android.api.model.ApiStationInfo;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;

import java.util.Arrays;

public class StationFixtures {
    public static ApiStation getApiStationFixture() {
        final ModelCollection<ApiTrack> tracks = new ModelCollection<>(Arrays.asList(ModelFixtures.create(ApiTrack.class)));
        return new ApiStation(getApiStationInfoFixture(), tracks);
    }

    public static ApiStationInfo getApiStationInfoFixture() {
        final ApiTrack seedTrack = ModelFixtures.create(ApiTrack.class);
        return new ApiStationInfo(Urn.forTrackStation(seedTrack.getId()), "station " + System.currentTimeMillis(), "fixture-stations", seedTrack);
    }
}