package com.soundcloud.android.search;

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

public class LoadPlaylistLikedStatusesTest extends StorageIntegrationTest {

    private LoadPlaylistLikedStatuses command;

    @Before
    public void setUp() throws Exception {
        command = new LoadPlaylistLikedStatuses(propeller());
    }

    @Test
    public void shouldReturnPlaylistLikeStatuses() throws Exception {
        ApiPlaylist apiPlaylist1 = testFixtures().insertLikedPlaylist(new Date());
        ApiPlaylist apiPlaylist2 = testFixtures().insertPlaylist();
        List<PropertySet> input = Arrays.asList(apiPlaylist1.toPropertySet(), apiPlaylist2.toPropertySet());

        Map<Urn, PropertySet> likedStatuses = command.call(input);

        assertThat(likedStatuses).hasSize(2);
        assertThat(likedStatuses.get(apiPlaylist1.getUrn()).get(PlaylistProperty.IS_LIKED)).isTrue();
        assertThat(likedStatuses.get(apiPlaylist2.getUrn()).get(PlaylistProperty.IS_LIKED)).isFalse();
    }

    @Test
    public void shouldOnlyReturnLikedStatusForPlaylists() throws Exception {
        final ApiPlaylist likedPlaylist = testFixtures().insertLikedPlaylist(new Date());
        final ApiPlaylist unlikedPlaylist = testFixtures().insertPlaylist();
        final ApiTrack track = testFixtures().insertTrack();

        List<PropertySet> input = Arrays.asList(
                likedPlaylist.toPropertySet(), unlikedPlaylist.toPropertySet(), track.toPropertySet());


        Map<Urn, PropertySet> likedStatuses = command.call(input);

        assertThat(likedStatuses.get(likedPlaylist.getUrn()).get(PlaylistProperty.IS_LIKED)).isTrue();
        assertThat(likedStatuses.get(unlikedPlaylist.getUrn()).get(PlaylistProperty.IS_LIKED)).isFalse();
        assertThat(likedStatuses.containsKey(track.getUrn())).isFalse();
    }

}
