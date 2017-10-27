package com.soundcloud.android.api.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.PlaylistFixtures;
import org.junit.Test;

import java.util.Date;

public class ApiPlaylistRepostTest {

    private static final Date REPOST_DATE = new Date();
    private final ApiPlaylist apiPlaylist = PlaylistFixtures.apiPlaylist();

    @Test
    public void trackRecordReturnsApiTrack() {
        assertThat(new ApiPlaylistRepost(apiPlaylist, REPOST_DATE).getPlaylistRecord()).isEqualTo(apiPlaylist);
    }
}
