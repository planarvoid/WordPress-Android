package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
public class TrackSourceInfoTest {

    @Test
    public void shouldCreateManualSourceParams() throws Exception {
        expect(toQueryParams(TrackSourceInfo.manual())).toEqual("trigger=manual");
    }

    @Test
    public void shouldCreateAutoSourceParams() throws Exception {
        expect(toQueryParams(TrackSourceInfo.auto())).toEqual("trigger=auto");
    }

    @Test
    public void shouldCreateRecommenderSourceParams() throws Exception {
        expect(toQueryParams(TrackSourceInfo.fromRecommender("version1"))).toEqual("trigger=auto&source=recommender&source_version=version1");
    }

    private String toQueryParams(TrackSourceInfo trackSourceInfo) {
        return trackSourceInfo.appendEventLoggerParams(new Uri.Builder()).build().getQuery().toString();
    }
}
