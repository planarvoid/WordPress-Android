package com.soundcloud.android.search;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
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

        List<PropertySet> input = Arrays.asList(repostedPlaylist.toPropertySet(), playlist.toPropertySet());

        Map<Urn, PropertySet> repostStatuses = command.call(input);

        assertThat(repostStatuses).hasSize(2);
        assertThat(repostStatuses.get(repostedPlaylist.getUrn()).get(PlaylistProperty.IS_REPOSTED)).isTrue();
        assertThat(repostStatuses.get(playlist.getUrn()).get(PlaylistProperty.IS_REPOSTED)).isFalse();
    }

    @Test
    public void shouldOnlyReturnLikedStatusForPlaylists() {
        final ApiTrack track = testFixtures().insertTrack();
        final List<PropertySet> input = singletonList(track.toPropertySet());

        Map<Urn, PropertySet> repostStatuses = command.call(input);

        assertThat(repostStatuses.containsKey(track.getUrn())).isFalse();
    }

    private ApiPlaylist insertRepostedPlaylist() {
        final ApiPlaylist repostedPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistRepost(repostedPlaylist.getId(), new Date().getTime());
        return repostedPlaylist;
    }
}
