package com.soundcloud.android.collection;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.ApiSyncable;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
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
        final List<ApiPlaylist> input = Arrays.asList(likedPlaylist, playlist);

        final Map<Urn, Boolean> likedStatuses = command.call(Lists.transform(input, ApiPlaylist::getUrn));

        assertThat(likedStatuses).hasSize(2);
        assertThat(likedStatuses.get(likedPlaylist.getUrn())).isTrue();
        assertThat(likedStatuses.get(playlist.getUrn())).isFalse();
    }

    @Test
    public void shouldOnlyReturnLikedStatusForPlaylists() {
        final ApiTrack track = testFixtures().insertTrack();
        final List<ApiSyncable> input = Arrays.asList(likedPlaylist, playlist, track);

        final Map<Urn, Boolean> likedStatuses = command.call(Lists.transform(input, ApiSyncable::getUrn));

        assertThat(likedStatuses.get(likedPlaylist.getUrn())).isTrue();
        assertThat(likedStatuses.get(playlist.getUrn())).isFalse();
        assertThat(likedStatuses.containsKey(track.getUrn())).isFalse();
    }

}
