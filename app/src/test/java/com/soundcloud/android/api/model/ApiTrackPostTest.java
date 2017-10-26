package com.soundcloud.android.api.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.TrackFixtures;
import org.junit.Test;

public class ApiTrackPostTest {

    private final ApiTrack apiTrack = TrackFixtures.apiTrack();

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        assertThat(new ApiTrackPost(apiTrack).getTrackRecord()).isEqualTo(apiTrack);
    }
}
