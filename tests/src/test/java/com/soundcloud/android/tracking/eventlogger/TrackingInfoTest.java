package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

import java.io.Serializable;

@RunWith(SoundCloudTestRunner.class)
public class TrackingInfoTest {

    @Test
    public void shouldBeEqualWithJustSourceContext() throws Exception {
        final TrackingInfo actual = new TrackingInfo("origin-url");
        final TrackingInfo expected = new TrackingInfo("origin-url");
        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldBeEqualWithSourceAndExplore() throws Exception {
        final TrackingInfo actual = new TrackingInfo("origin-url", "explore-tag");
        final TrackingInfo expected = new TrackingInfo("origin-url", "explore-tag");
        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldNotBeEqualWithDifferentExplore() throws Exception {
        final TrackingInfo actual = new TrackingInfo("origin-url", "explore-tag");
        final TrackingInfo expected = new TrackingInfo("origin-url", "explore-tag2");
        expect(actual).not.toEqual(expected);
    }

    @Test
    public void shouldNotBeEqualWithDifferentSource() throws Exception {
        final TrackingInfo actual = new TrackingInfo("origin-url", "explore-tag");
        final TrackingInfo expected = new TrackingInfo("origin-url2", "explore-tag");
        expect(actual).not.toEqual(expected);
    }

    @Test
    public void shouldBeSerializableFromJustExploreTag() throws Exception {
        Serializable original = new TrackingInfo("origin-url");
        Serializable copy = (Serializable) SerializationUtils.clone(original);
        expect(original).toEqual(copy);
    }

    @Test
    public void shouldPersistFromParams() throws Exception {
        Uri uri = Uri.parse("http://something.com");
        final TrackingInfo expected = new TrackingInfo("origin-url", "explore-tag");
        expect(TrackingInfo.fromUriParams(expected.appendAsQueryParams(uri.buildUpon()).build())).toEqual(expected);
    }
}
