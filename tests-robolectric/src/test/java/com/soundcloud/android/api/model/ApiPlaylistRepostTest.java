package com.soundcloud.android.api.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class ApiPlaylistRepostTest {

    public static final Date REPOST_DATE = new Date();
    private final ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);

    @Test
    public void createsPropertySetFromTrackDependencyWithRepost() throws Exception {
        expect(new ApiPlaylistRepost(apiPlaylist, REPOST_DATE).toPropertySet()).toEqual(
                apiPlaylist.toPropertySet()
                        .put(PostProperty.IS_REPOST, true)
                        .put(PostProperty.CREATED_AT, REPOST_DATE));
    }

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        expect(new ApiPlaylistRepost(apiPlaylist, REPOST_DATE).getPlaylistRecord()).toEqual(apiPlaylist);
    }
}