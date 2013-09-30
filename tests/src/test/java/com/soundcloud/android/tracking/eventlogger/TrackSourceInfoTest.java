package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class TrackSourceInfoTest {

    @Test
    public void shouldCreateManualSourceParams() throws Exception {
        expect(TrackSourceInfo.manual().toQueryParams()).toEqual("trigger=manual");
    }

    @Test
    public void shouldCreateAutoSourceParams() throws Exception {
        expect(TrackSourceInfo.auto().toQueryParams()).toEqual("trigger=auto");
    }

    @Test
    public void shouldCreateRecommenderSourceParams() throws Exception {
        expect(TrackSourceInfo.fromRecommender("version1").toQueryParams()).toEqual("source=recommender&trigger=auto&source_version=version1");
    }
}
