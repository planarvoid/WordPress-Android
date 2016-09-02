package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationFixtures.createStationInfoTrack;
import static com.soundcloud.android.stations.StationFixtures.getStationWithTracks;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class StationWithTracksTest extends AndroidUnitTest {

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
