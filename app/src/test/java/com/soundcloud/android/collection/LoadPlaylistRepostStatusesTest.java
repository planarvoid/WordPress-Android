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

public class LoadPlaylistRepostStatusesTest extends StorageIntegrationTest {

    private LoadPlaylistRepostStatuses command;

    @Before
    public void setUp() throws Exception {
        command = new LoadPlaylistRepostStatuses(propeller());
    }

    @Test
    public void shouldReturnPlaylistRepostStatuses() {
        final ApiPlaylist repostedPlaylist = insertRepostedPlaylist();
        final ApiPlaylist playlist = testFixtures().insertPlaylist();

        List<ApiPlaylist> input = Arrays.asList(repostedPlaylist, playlist);

        Map<Urn, Boolean> repostStatuses = command.call(Lists.transform(input, ApiPlaylist::getUrn));

        assertThat(repostStatuses).hasSize(2);
        assertThat(repostStatuses.get(repostedPlaylist.getUrn())).isTrue();
        assertThat(repostStatuses.get(playlist.getUrn())).isFalse();
    }

    @Test
    public void shouldOnlyReturnLikedStatusForPlaylists() {
        final ApiTrack track = testFixtures().insertTrack();
        final List<ApiTrack> input = singletonList(track);

        Map<Urn, Boolean> repostStatuses = command.call(Lists.transform(input, ApiTrack::getUrn));

        assertThat(repostStatuses.containsKey(track.getUrn())).isFalse();
    }

    private ApiPlaylist insertRepostedPlaylist() {
        final ApiPlaylist repostedPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistRepost(repostedPlaylist.getId(), new Date().getTime());
        return repostedPlaylist;
    }
}
