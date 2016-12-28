package com.soundcloud.android.collection;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LoadTrackRepostStatusesTest extends StorageIntegrationTest {

    private LoadTrackRepostStatuses command;

    @Before
    public void setUp() throws Exception {
        command = new LoadTrackRepostStatuses(propeller());
    }

    @Test
    public void shouldReturnTrackRepostStatuses() {
        final ApiTrack repostedTrack = insertRepostedTrack();
        final ApiTrack track = testFixtures().insertTrack();

        List<ApiTrack> input = Arrays.asList(repostedTrack, track);

        Map<Urn, Boolean> repostStatuses = command.call(Lists.transform(input, ApiTrack::getUrn));

        assertThat(repostStatuses).hasSize(2);
        assertThat(repostStatuses.get(repostedTrack.getUrn())).isTrue();
        assertThat(repostStatuses.get(track.getUrn())).isFalse();
    }

    @Test
    public void shouldOnlyReturnLikedStatusForTracks() {
        final ApiPlaylist playlist = testFixtures().insertPlaylist();
        final List<ApiPlaylist> input = singletonList(playlist);

        Map<Urn, Boolean> repostStatuses = command.call(Lists.transform(input, ApiPlaylist::getUrn));

        assertThat(repostStatuses.containsKey(playlist.getUrn())).isFalse();
    }

    private ApiTrack insertRepostedTrack() {
        final ApiTrack repostedTrack = testFixtures().insertTrack();
        testFixtures().insertTrackRepost(repostedTrack.getId(), new Date().getTime());
        return repostedTrack;
    }

}
