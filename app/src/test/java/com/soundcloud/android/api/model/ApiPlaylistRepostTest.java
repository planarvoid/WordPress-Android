package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

import java.util.Date;

public class ApiPlaylistRepostTest {

    public static final Date REPOST_DATE = new Date();
    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

    @Test
    public void createsPropertySetFromTrackDependencyWithRepost() {
        assertThat(new ApiPlaylistRepost(apiPlaylist, REPOST_DATE).toPropertySet()).isEqualTo(
                apiPlaylist.toPropertySet()
                        .put(PostProperty.IS_REPOST, true)
                        .put(PostProperty.CREATED_AT, REPOST_DATE));
    }

    @Test
    public void trackRecordReturnsApiTrack() {
        assertThat(new ApiPlaylistRepost(apiPlaylist, REPOST_DATE).getPlaylistRecord()).isEqualTo(apiPlaylist);
    }
}
