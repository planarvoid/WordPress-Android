package com.soundcloud.android.api.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class ApiPlaylistLikeTest {

    private final Date createdAt = new Date();
    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

    @Test
    public void createsPropertySetFromTrackDependency() throws Exception {
        final PropertySet expected = apiPlaylist.toPropertySet().put(LikeProperty.CREATED_AT, createdAt);
        expect(new ApiPlaylistLike(apiPlaylist, createdAt).toPropertySet()).toEqual(expected);
    }

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        expect(new ApiPlaylistLike(apiPlaylist, createdAt).getPlaylistRecord()).toEqual(apiPlaylist);
    }
}