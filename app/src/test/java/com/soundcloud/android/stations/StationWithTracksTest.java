package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationFixtures.createStationInfoTrack;
import static com.soundcloud.android.stations.StationFixtures.getStationWithTracks;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class StationWithTracksTest {

    @Test
    public void calculatesMostPlayedArtists() {
        final List<StationInfoTrack> tracks = Arrays.asList(createStationInfoTrack(3, "Madonna"),
                                                            createStationInfoTrack(1, "Dr. Dre"),
                                                            createStationInfoTrack(2, "Gummi bear"),
                                                            createStationInfoTrack(2, "Madonna"),
                                                            createStationInfoTrack(1, "Ostry"));

        final StationWithTracks stationWithTracks = getStationWithTracks(Urn.forTrackStation(123L), tracks);
        assertThat(stationWithTracks.getMostPlayedArtists()).containsExactly("Madonna", "Gummi bear", "Dr. Dre");
    }
}
