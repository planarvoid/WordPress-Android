package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

import java.io.Serializable;

@RunWith(SoundCloudTestRunner.class)
public class PlaySourceTrackingInfoTest {

    @Test
    public void shouldBeEqualWithJustSourceContext() throws Exception {
        final PlaySourceTrackingInfo actual = new PlaySourceTrackingInfo("origin-url");
        final PlaySourceTrackingInfo expected = new PlaySourceTrackingInfo("origin-url");
        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldBeEqualWithSourceAndExplore() throws Exception {
        final PlaySourceTrackingInfo actual = new PlaySourceTrackingInfo("origin-url", "explore-tag");
        final PlaySourceTrackingInfo expected = new PlaySourceTrackingInfo("origin-url", "explore-tag");
        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldNotBeEqualWithDifferentExplore() throws Exception {
        final PlaySourceTrackingInfo actual = new PlaySourceTrackingInfo("origin-url", "explore-tag");
        final PlaySourceTrackingInfo expected = new PlaySourceTrackingInfo("origin-url", "explore-tag2");
        expect(actual).not.toEqual(expected);
    }

    @Test
    public void shouldNotBeEqualWithDifferentSource() throws Exception {
        final PlaySourceTrackingInfo actual = new PlaySourceTrackingInfo("origin-url", "explore-tag");
        final PlaySourceTrackingInfo expected = new PlaySourceTrackingInfo("origin-url2", "explore-tag");
        expect(actual).not.toEqual(expected);
    }

    @Test
    public void shouldBeSerializableFromJustExploreTag() throws Exception {
        Serializable original = new PlaySourceTrackingInfo("origin-url");
        Serializable copy = (Serializable) SerializationUtils.clone(original);
        expect(original).toEqual(copy);
    }

    @Test
    public void shouldCreateParams() throws Exception {
        final PlaySourceTrackingInfo playInfo = new PlaySourceTrackingInfo("origin-url", "explore-tag");
        expect(playInfo.toQueryParams()).toEqual("tracking-exploreTag=explore-tag&tracking-originUrl=origin-url");
    }

    @Test
    public void shouldPersistFromParams() throws Exception {
        Uri uri = Uri.parse("http://something.com");
        final PlaySourceTrackingInfo expected = new PlaySourceTrackingInfo("origin-url", "explore-tag");
        expect(PlaySourceTrackingInfo.fromUriParams(expected.appendAsQueryParams(uri.buildUpon()).build())).toEqual(expected);
    }
}
