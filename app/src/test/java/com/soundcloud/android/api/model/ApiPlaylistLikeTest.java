package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

import java.util.Date;

public class ApiPlaylistLikeTest {

    private final Date createdAt = new Date();
    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

    @Test
    public void trackRecordReturnsApiTrack() {
        assertThat(new ApiPlaylistLike(apiPlaylist, createdAt).getPlaylistRecord()).isEqualTo(apiPlaylist);
    }
}
