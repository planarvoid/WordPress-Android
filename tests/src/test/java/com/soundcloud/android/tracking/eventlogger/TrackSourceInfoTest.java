package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

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

    @Test
    public void shouldIncludePlaySourceParams() throws Exception {
        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(123L).originUrl("originUrl1").exploreTag("exploreTag1").build();
        final TrackSourceInfo trackSourceInfo = TrackSourceInfo.fromRecommender("version1");

        expect(trackSourceInfo.createEventLoggerParams(playSourceInfo))
                .toEqual("context=originUrl1&exploreTag=exploreTag1&trigger=auto&source=recommender&source_version=version1");
    }

    private String toQueryParams(TrackSourceInfo trackSourceInfo) {
        return trackSourceInfo.createEventLoggerParams(Mockito.mock(PlaySourceInfo.class));
    }
}
