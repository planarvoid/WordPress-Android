package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.ScTextUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(SoundCloudTestRunner.class)
public class TrackSourceInfoTest {

    @Test
    public void shouldCreateManualSourceParams() throws Exception {
        expect(toQueryParams(new TrackSourceInfo().setTrigger(true))).toEqual("trigger=manual");
    }

    @Test
    public void shouldCreateAutoSourceParams() throws Exception {
        expect(toQueryParams(new TrackSourceInfo().setTrigger(false))).toEqual("trigger=auto");
    }

    @Test
    public void shouldCreateRecommenderSourceParams() throws Exception {
        expect(toQueryParams(TrackSourceInfo.fromRecommender("version1", ScTextUtils.EMPTY_STRING))).toEqual("source=recommender&source_version=version1");
    }

    @Test
    public void shouldMakeEmptyParams() throws Exception {
        expect(toQueryParams(TrackSourceInfo.EMPTY)).toEqual(ScTextUtils.EMPTY_STRING);
    }

    @Test
    public void shouldIncludePlaySourceParams() throws Exception {
        final TrackSourceInfo trackSourceInfo = TrackSourceInfo.fromRecommender("version1", "originUrl1");

        expect(trackSourceInfo.createEventLoggerParams(Uri.EMPTY))
                .toEqual("context=originUrl1&source=recommender&source_version=version1");
    }

    private String toQueryParams(TrackSourceInfo trackSourceInfo) {
        return trackSourceInfo.createEventLoggerParams(Content.ME_LIKES.uri);
    }
}
