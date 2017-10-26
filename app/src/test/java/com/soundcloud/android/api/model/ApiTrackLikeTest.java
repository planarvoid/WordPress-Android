package com.soundcloud.android.api.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.TrackFixtures;
import org.junit.Test;

import java.util.Date;

public class ApiTrackLikeTest {

    private final Date createdAt = new Date();
    private final ApiTrack apiTrack = TrackFixtures.apiTrack();

    @Test
    public void trackRecordReturnsApiTrack() {
        assertThat(new ApiTrackLike(apiTrack, createdAt).getTrackRecord()).isEqualTo(apiTrack);
    }
}
