package com.soundcloud.android.tracking.eventlogger;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class TrackSourceInfoTest {

//    @Test
//    public void shouldCreateManualSourceParams() throws Exception {
//        expect(toQueryParams(new TrackSourceInfo().setTrigger(true))).toEqual("trigger=manual");
//    }
//
//    @Test
//    public void shouldCreateAutoSourceParams() throws Exception {
//        expect(toQueryParams(new TrackSourceInfo().setTrigger(false))).toEqual("trigger=auto");
//    }
//
//    @Test
//    public void shouldCreateRecommenderSourceParams() throws Exception {
//        expect(toQueryParams(TrackSourceInfo.fromRecommender("version1", ScTextUtils.EMPTY_STRING))).toEqual("source=recommender&source_version=version1");
//    }
//
//    @Test
//    public void shouldMakeEmptyParams() throws Exception {
//        expect(toQueryParams(TrackSourceInfo.EMPTY)).toEqual(ScTextUtils.EMPTY_STRING);
//    }
//
//    @Test
//    public void shouldEncodeOriginUrl() throws Exception {
//        expect(toQueryParams(TrackSourceInfo.fromRecommender(ScTextUtils.EMPTY_STRING, "asdf"))).toEqual("context=asdf&source=recommender");
//        expect(toQueryParams(TrackSourceInfo.fromRecommender(ScTextUtils.EMPTY_STRING, "ASDF"))).toEqual("context=asdf&source=recommender");
//        expect(toQueryParams(TrackSourceInfo.fromRecommender(ScTextUtils.EMPTY_STRING, "ASDF FDSA"))).toEqual("context=asdf_fdsa&source=recommender");
//        expect(toQueryParams(TrackSourceInfo.fromRecommender(ScTextUtils.EMPTY_STRING, "ASDF & FDSA"))).toEqual("context=asdf_%26_fdsa&source=recommender");
//    }
//
//    @Test
//    public void shouldIncludePlaySourceParams() throws Exception {
//        final TrackSourceInfo trackSourceInfo = TrackSourceInfo.fromRecommender("version1", "originUrl1");
//
//        expect(trackSourceInfo.createEventLoggerParams())
//                .toEqual("context=originurl1&source=recommender&source_version=version1");
//    }
//
//    private String toQueryParams(TrackSourceInfo trackSourceInfo) {
//        return trackSourceInfo.createEventLoggerParams();
//    }
}
