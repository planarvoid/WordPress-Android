package com.soundcloud.android.api.model;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.TrackFixtures;
import org.junit.Test;

import java.util.Date;

public class ApiTrackRepostTest {

    public static final Date REPOST_DATE = new Date();
    private final ApiTrack apiTrack = TrackFixtures.apiTrack();

    @Test
    public void trackRecordReturnsApiTrack() throws Exception {
        assertThat(new ApiTrackRepost(apiTrack, REPOST_DATE).getTrackRecord()).isEqualTo(apiTrack);
    }
}
