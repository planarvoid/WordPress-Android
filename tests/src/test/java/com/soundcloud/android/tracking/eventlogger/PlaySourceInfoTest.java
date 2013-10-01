package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

import java.io.Serializable;

@RunWith(SoundCloudTestRunner.class)
public class PlaySourceInfoTest {

    @Test
    public void shouldBeEqualWithJustSourceContext() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo("origin-url");
        final PlaySourceInfo expected = new PlaySourceInfo("origin-url");
        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldBeEqualWithSourceAndExplore() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo("origin-url", "explore-tag");
        final PlaySourceInfo expected = new PlaySourceInfo("origin-url", "explore-tag");
        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldNotBeEqualWithDifferentExplore() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo("origin-url", "explore-tag");
        final PlaySourceInfo expected = new PlaySourceInfo("origin-url", "explore-tag2");
        expect(actual).not.toEqual(expected);
    }

    @Test
    public void shouldNotBeEqualWithDifferentSource() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo("origin-url", "explore-tag");
        final PlaySourceInfo expected = new PlaySourceInfo("origin-url2", "explore-tag");
        expect(actual).not.toEqual(expected);
    }

    @Test
    public void shouldBeSerializableFromJustExploreTag() throws Exception {
        Serializable original = new PlaySourceInfo("origin-url");
        Serializable copy = (Serializable) SerializationUtils.clone(original);
        expect(original).toEqual(copy);
    }

    @Test
    public void shouldCreateParams() throws Exception {
        final PlaySourceInfo playInfo = new PlaySourceInfo("origin-url", "explore-tag");
        expect(playInfo.toQueryParams()).toEqual("playSource-originUrl=origin-url&playSource-exploreTag=explore-tag");
    }

    @Test
    public void shouldPersistFromParams() throws Exception {
        Uri uri = Uri.parse("http://something.com");
        final PlaySourceInfo expected = new PlaySourceInfo("origin-url", "explore-tag");
        expect(PlaySourceInfo.fromUriParams(expected.appendAsQueryParams(uri.buildUpon()).build())).toEqual(expected);
    }
}
