package com.soundcloud.android.api.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.posts.PostProperty;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ApiPlaylistPostTest {

    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

    @Test
    public void createsPropertySetFromTrackDependency() throws Exception {
        final PropertySet expected = apiPlaylist.toPropertySet()
                .put(PostProperty.IS_REPOST, false)
                .put(PostProperty.CREATED_AT, apiPlaylist.getCreatedAt());

        expect(new ApiPlaylistPost(apiPlaylist).toPropertySet()).toEqual(expected);
    }

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        expect(new ApiPlaylistPost(apiPlaylist).getPlaylistRecord()).toEqual(apiPlaylist);
    }
}