package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

import java.util.Date;

public class ApiTrackRepostTest {

    public static final Date REPOST_DATE = new Date();
    private final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);

    @Test
    public void createsPropertySetFromTrackDependency() throws Exception {
        assertThat(new ApiTrackRepost(apiTrack, REPOST_DATE).toPropertySet()).isEqualTo(
                apiTrack.toPropertySet()
                        .put(PostProperty.IS_REPOST, true)
                        .put(PostProperty.CREATED_AT, REPOST_DATE));
    }

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        assertThat(new ApiTrackRepost(apiTrack, REPOST_DATE).getTrackRecord()).isEqualTo(apiTrack);
    }
}
