package com.soundcloud.android.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.experiments.SearchPlayRelatedTracksConfig;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class SearchPlayQueueFilterTest {
    private static final ListItem TRACK_1 = PlayableFixtures.expectedPromotedTrack();
    private static final ListItem TRACK_2 = PlayableFixtures.expectedTrackForPlayer();

    @Mock SearchPlayRelatedTracksConfig playRelatedTracksConfig;

    @Test
    public void correctQueueWhenExperimentEnabled() throws Exception {
        SearchPlayQueueFilter filter = new SearchPlayQueueFilter(playRelatedTracksConfig);
        when(playRelatedTracksConfig.isEnabled()).thenReturn(true);

        assertThat(filter.correctQueue(trackList(), 1)).isEqualTo(Collections.singletonList(TRACK_2));
    }

    @Test
    public void doNotCorrectQueueWhenExperimentDisabled() throws Exception {
        SearchPlayQueueFilter filter = new SearchPlayQueueFilter(playRelatedTracksConfig);
        when(playRelatedTracksConfig.isEnabled()).thenReturn(false);
        final List<ListItem> expected = trackList();

        assertThat(filter.correctQueue(expected, 0)).isEqualTo(trackList());
    }

    @Test
    public void correctPositionWhenExperimentEnabled() throws Exception {
        SearchPlayQueueFilter filter = new SearchPlayQueueFilter(playRelatedTracksConfig);
        when(playRelatedTracksConfig.isEnabled()).thenReturn(true);
        final int position = 1;

        assertThat(filter.correctPosition(position)).isEqualTo(0);
    }

    @Test
    public void doNotCorrectPositionWhenExperimentDisabled() throws Exception {
        SearchPlayQueueFilter filter = new SearchPlayQueueFilter(playRelatedTracksConfig);
        when(playRelatedTracksConfig.isEnabled()).thenReturn(false);
        final int position = 1;

        assertThat(filter.correctPosition(position)).isEqualTo(position);
    }

    private List<ListItem> trackList() {
        final List<ListItem> trackList = new ArrayList<>();

        trackList.add(TRACK_1);
        trackList.add(TRACK_2);

        return trackList;
    }
}
