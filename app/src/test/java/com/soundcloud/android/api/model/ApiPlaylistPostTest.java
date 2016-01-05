package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

public class ApiPlaylistPostTest {

    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

    @Test
    public void createsPropertySetFromTrackDependency() throws Exception {
        final PropertySet expected = apiPlaylist.toPropertySet()
                .put(PostProperty.IS_REPOST, false)
                .put(PostProperty.CREATED_AT, apiPlaylist.getCreatedAt());

        assertThat(new ApiPlaylistPost(apiPlaylist).toPropertySet()).isEqualTo(expected);
    }

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        assertThat(new ApiPlaylistPost(apiPlaylist).getPlaylistRecord()).isEqualTo(apiPlaylist);
    }
}
