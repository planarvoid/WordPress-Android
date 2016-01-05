package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

import java.util.Date;

public class ApiPlaylistLikeTest {

    private final Date createdAt = new Date();
    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

    @Test
    public void createsPropertySetFromTrackDependency() {
        final PropertySet expected = apiPlaylist.toPropertySet().put(LikeProperty.CREATED_AT, createdAt);
        assertThat(new ApiPlaylistLike(apiPlaylist, createdAt).toPropertySet()).isEqualTo(expected);
    }

    @Test
    public void trackRecordReturnsApiTrack() {
        assertThat(new ApiPlaylistLike(apiPlaylist, createdAt).getPlaylistRecord()).isEqualTo(apiPlaylist);
    }
}
