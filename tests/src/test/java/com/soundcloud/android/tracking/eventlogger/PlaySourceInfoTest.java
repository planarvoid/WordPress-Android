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
    public void shouldBeEqual() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo("origin-url", 1234L, "explore-tag", "version-a");
        final PlaySourceInfo expected = new PlaySourceInfo("origin-url", 1234L, "explore-tag", "version-a");
        expect(actual).toEqual(expected);
    }

    @Test
    public void shouldNotBeEqual() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo("origin-url", 123L, "explore-tag", "version_1");
        expect(actual).not.toEqual(new PlaySourceInfo("origin-url2", 123L, "explore-tag", "version_1"));
        expect(actual).not.toEqual(new PlaySourceInfo("origin-url", 1234L, "explore-tag", "version_1"));
        expect(actual).not.toEqual(new PlaySourceInfo("origin-url", 123L, "explore-tag2", "version_1"));
        expect(actual).not.toEqual(new PlaySourceInfo("origin-url", 1234L, "explore-tag", "version_2"));
    }

    @Test
    public void shouldNotBeEqualWithDifferentExplore() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo("origin-url", 123L, "explore-tag", "version_1");
        final PlaySourceInfo expected = new PlaySourceInfo("origin-url", 123L, "explore-tag2", "version_1");
        expect(actual).not.toEqual(expected);
    }

    @Test
    public void shouldNotBeEqualWithDifferentOrigin() throws Exception {
        final PlaySourceInfo actual = new PlaySourceInfo("origin-url", 123L, "explore-tag", "rec_version_1");
        final PlaySourceInfo expected = new PlaySourceInfo("origin-url2", 123L, "explore-tag", "rec_version_1");
        expect(actual).not.toEqual(expected);
    }

    @Test
    public void shouldBeSerializeable() throws Exception {
        final Serializable original = new PlaySourceInfo("origin-url", 123L, "explore-tag", "version_1");
        Serializable copy = (Serializable) SerializationUtils.clone(original);
        expect(original).toEqual(copy);
    }

    @Test
    public void shouldCreateParams() throws Exception {
        final PlaySourceInfo playInfo = new PlaySourceInfo("origin-url", 1234L, "explore-tag", "version-a");
        expect(playInfo.toQueryParams()).toEqual("playSource-originUrl=origin-url&playSource-exploreTag=explore-tag&playSource-recommenderVersion=version-a&playSource-initialTrackId=1234");
    }

    @Test
    public void shouldPersistFromParams() throws Exception {
        Uri uri = Uri.parse("http://something.com");
        final PlaySourceInfo expected = new PlaySourceInfo("origin-url", 1234L, "explore-tag", "version-a");
        expect(PlaySourceInfo.fromUriParams(expected.appendAsQueryParams(uri.buildUpon()).build())).toEqual(expected);
    }
}
