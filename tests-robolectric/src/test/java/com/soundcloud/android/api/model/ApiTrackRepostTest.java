package com.soundcloud.android.api.model;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class ApiTrackRepostTest {

    public static final Date REPOST_DATE = new Date();
    private final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);

    @Test
    public void createsPropertySetFromTrackDependency() throws Exception {
        expect(new ApiTrackRepost(apiTrack, REPOST_DATE).toPropertySet()).toEqual(
                apiTrack.toPropertySet()
                        .put(PostProperty.IS_REPOST, true)
                        .put(PostProperty.CREATED_AT, REPOST_DATE));
    }

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        expect(new ApiTrackRepost(apiTrack, REPOST_DATE).getTrackRecord()).toEqual(apiTrack);
    }
}