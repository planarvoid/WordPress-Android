package com.soundcloud.android.api.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class ApiTrackLikeTest {

    private final Date createdAt = new Date();
    private final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);

    @Test
    public void createsPropertySetFromTrackDependency() throws Exception {
        final PropertySet expected = apiTrack.toPropertySet().put(LikeProperty.CREATED_AT, createdAt);
        expect(new ApiTrackLike(apiTrack, createdAt).toPropertySet()).toEqual(expected);
    }

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        expect(new ApiTrackLike(apiTrack, createdAt).getTrackRecord()).toEqual(apiTrack);
    }
}