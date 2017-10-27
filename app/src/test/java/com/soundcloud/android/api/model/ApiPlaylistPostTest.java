package com.soundcloud.android.api.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.PlaylistFixtures;
import org.junit.Test;

public class ApiPlaylistPostTest {

    private final ApiPlaylist apiPlaylist = PlaylistFixtures.apiPlaylist();

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        assertThat(new ApiPlaylistPost(apiPlaylist).getPlaylistRecord()).isEqualTo(apiPlaylist);
    }
}
