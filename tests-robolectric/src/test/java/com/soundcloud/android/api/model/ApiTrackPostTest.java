package com.soundcloud.android.api.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ApiTrackPostTest {

    private final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);

    @Test
    public void createsPropertySetFromTrackDependency() throws Exception {
        final PropertySet expected = apiTrack.toPropertySet()
                .put(PostProperty.IS_REPOST, false)
                .put(PostProperty.CREATED_AT, apiTrack.getCreatedAt());

        expect(new ApiTrackPost(apiTrack).toPropertySet()).toEqual(expected);
    }

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        expect(new ApiTrackPost(apiTrack).getTrackRecord()).toEqual(apiTrack);
    }
}