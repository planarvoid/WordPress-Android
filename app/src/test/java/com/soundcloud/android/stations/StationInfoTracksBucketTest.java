package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class StationInfoTracksBucketTest extends AndroidUnitTest {

    @Test
    public void calculatesMostPlayedArtists() {
        final List<StationInfoTrack> tracks = Arrays.asList(createStationInfoTrack(3, "Madonna"),
                                                            createStationInfoTrack(1, "Dr. Dre"),
                                                            createStationInfoTrack(2, "Gummi bear"),
                                                            createStationInfoTrack(2, "Madonna"),
                                                            createStationInfoTrack(1, "Ostry"));

        final StationInfoTracksBucket bucket = StationInfoTracksBucket.from(tracks, 0);
        assertThat(bucket.getMostPlayedArtists(3)).containsExactly("Madonna", "Gummi bear", "Dr. Dre");
    }

    private StationInfoTrack createStationInfoTrack(int playCount, String artistName) {
        final PropertySet trackState = TestPropertySets.fromApiTrack()
                                                       .put(TrackProperty.PLAY_COUNT, playCount)
                                                       .put(TrackProperty.CREATOR_NAME, artistName);
        return StationInfoTrack.from(TrackItem.from(trackState));
    }
}
