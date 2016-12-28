package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

public class ApiTrackPostTest {

    private final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        assertThat(new ApiTrackPost(apiTrack).getTrackRecord()).isEqualTo(apiTrack);
    }
}
