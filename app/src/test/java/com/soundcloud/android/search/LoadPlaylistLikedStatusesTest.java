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
    private ApiPlaylist likedPlaylist;
    private ApiPlaylist playlist;

    @Before
    public void setUp() throws Exception {
        likedPlaylist = testFixtures().insertLikedPlaylist(new Date());
        playlist = testFixtures().insertPlaylist();

        command = new LoadPlaylistLikedStatuses(propeller());
    }

    @Test
    public void shouldReturnPlaylistLikeStatuses() {
        final List<PropertySet> input = Arrays.asList(likedPlaylist.toPropertySet(), playlist.toPropertySet());

        final Map<Urn, PropertySet> likedStatuses = command.call(input);

        assertThat(likedStatuses).hasSize(2);
        assertThat(likedStatuses.get(likedPlaylist.getUrn()).get(PlaylistProperty.IS_USER_LIKE)).isTrue();
        assertThat(likedStatuses.get(playlist.getUrn()).get(PlaylistProperty.IS_USER_LIKE)).isFalse();
    }

    @Test
    public void shouldOnlyReturnLikedStatusForPlaylists() {
        final ApiTrack track = testFixtures().insertTrack();
        final List<PropertySet> input = Arrays.asList(
                likedPlaylist.toPropertySet(), playlist.toPropertySet(), track.toPropertySet());

        final Map<Urn, PropertySet> likedStatuses = command.call(input);

        assertThat(likedStatuses.get(likedPlaylist.getUrn()).get(PlaylistProperty.IS_USER_LIKE)).isTrue();
        assertThat(likedStatuses.get(playlist.getUrn()).get(PlaylistProperty.IS_USER_LIKE)).isFalse();
        assertThat(likedStatuses.containsKey(track.getUrn())).isFalse();
    }

}
