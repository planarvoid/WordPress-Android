package com.soundcloud.android.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

import java.util.Date;

public class ApiTrackLikeTest {

    private final Date createdAt = new Date();
    private final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);

    @Test
    public void trackRecordReturnsApiTrack() {
        assertThat(new ApiTrackLike(apiTrack, createdAt).getTrackRecord()).isEqualTo(apiTrack);
    }
}
