package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;

import java.util.ArrayList;

public class StationTest {

    @Test
    public void stationWithSeedTrackShouldPrependTheTracksListWirhTheSeed() {
        final Urn seed = Urn.forTrack(555L);
        final ApiStation sourceStation = StationFixtures.getApiStation();
        final Station stationWithSeed = Station.stationWithSeedTrack(sourceStation, seed);

        final ArrayList<StationTrack> expected = new ArrayList<>();
        expected.add(StationTrack.create(seed, Urn.NOT_SET));
        expected.addAll(sourceStation.getTracks());
        assertThat(stationWithSeed.getTracks()).isEqualTo(expected);
    }
}
