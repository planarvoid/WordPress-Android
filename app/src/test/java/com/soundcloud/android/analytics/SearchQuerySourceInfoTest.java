package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class SearchQuerySourceInfoTest {

    private SearchQuerySourceInfo searchQuerySourceInfo;

    @Test
    public void shouldGetPositionFromClickPosition() {
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, Urn.forPlaylist(321L));
        searchQuerySourceInfo.setQueryResults(new ArrayList<>(Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L), Urn.forUser(789L))));

        assertThat(searchQuerySourceInfo.getUpdatedResultPosition(Urn.forTrack(123L))).isEqualTo(5);
    }

    @Test
    public void shouldGetPositionFromPlayPosition() {
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, Urn.forTrack(123L));
        searchQuerySourceInfo.setQueryResults(new ArrayList<>(Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L), Urn.forUser(789L))));

        assertThat(searchQuerySourceInfo.getUpdatedResultPosition(Urn.forTrack(456L))).isEqualTo(1);
    }

    @Test
    public void shouldUseClickPositionOnTracksWithoutResults() {
        searchQuerySourceInfo = new SearchQuerySourceInfo(new Urn("some:search:urn"), 5, Urn.forTrack(123L));

        assertThat(searchQuerySourceInfo.getUpdatedResultPosition(Urn.forTrack(456L))).isEqualTo(5);
    }
}
