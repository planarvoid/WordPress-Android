package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationFixtures.getStationWithTracks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.annotations.Issue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

public class StationInfoTracksBucketRendererTest extends AndroidUnitTest {

    @Mock StationInfoAdapter.StationInfoClickListener clickListener;
    @Mock StationTrackRendererFactory trackRendererFactory;
    @Mock StationTrackRenderer trackRenderer;

    private StationInfoTracksBucketRenderer stationInfoTracksBucketRenderer;

    @Before
    public void setUp()  {
        when(trackRendererFactory.create(clickListener)).thenReturn(trackRenderer);
        stationInfoTracksBucketRenderer = new StationInfoTracksBucketRenderer(
                clickListener,
                trackRendererFactory);
    }

    @Test
    @Issue(ref = "https://github.com/soundcloud/SoundCloud-Android/issues/6195")
    public void scrollShouldNotAccessPositionBiggerThanTrackListSize() {
        final int lastPlayedPosition = 0;
        final StationInfoTracksBucket bucket = StationInfoTracksBucket
                .from(getStationWithTracks(Urn.forArtistStation(123),
                                           Collections.<StationInfoTrack>emptyList(),
                                           lastPlayedPosition),
                      Urn.NOT_SET);

        assertThat(stationInfoTracksBucketRenderer.scrollPosition(bucket)).isEqualTo(0);
    }

}