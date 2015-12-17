package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Test;

public class ApiTrackPostTest {

    private final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);

    @Test
    public void createsPropertySetFromTrackDependency() throws Exception {
        final PropertySet expected = apiTrack.toPropertySet()
                .put(PostProperty.IS_REPOST, false)
                .put(PostProperty.CREATED_AT, apiTrack.getCreatedAt());

        assertThat(new ApiTrackPost(apiTrack).toPropertySet()).isEqualTo(expected);
    }

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        assertThat(new ApiTrackPost(apiTrack).getTrackRecord()).isEqualTo(apiTrack);
    }
}
