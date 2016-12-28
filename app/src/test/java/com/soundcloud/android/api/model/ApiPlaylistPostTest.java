package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

public class ApiPlaylistPostTest {

    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        assertThat(new ApiPlaylistPost(apiPlaylist).getPlaylistRecord()).isEqualTo(apiPlaylist);
    }
}
