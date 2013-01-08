package com.soundcloud.android.task.fetch;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class ResolveFetchTaskTest {

    @Test
    public void shouldFixUris() throws Exception {
        expect(ResolveFetchTask.fixUri(Uri.parse("http://soundcloud.com/foo/follow")))
                .toEqual("http://soundcloud.com/foo");

        expect(ResolveFetchTask.fixUri(Uri.parse("http://soundcloud.com/foo/favorite")))
                .toEqual("http://soundcloud.com/foo");

        expect(ResolveFetchTask.fixUri(Uri.parse("http://soundcloud.com/foo/bla/baz")))
                .toEqual("http://soundcloud.com/foo/bla/baz");
    }

    @Test
    public void shouldFixClickTrackingUri() throws Exception {
        expect(ResolveFetchTask.fixClickTrackingUrl(
                Uri.parse("http://soundcloud.com/-/t/click/postman-email-follower?url=http://soundcloud.com/angelika-ochmann")))
                .toEqual("http://soundcloud.com/angelika-ochmann");


        expect(ResolveFetchTask.fixClickTrackingUrl(
                Uri.parse("http://soundcloud.com/angelika-ochmann?url=foo")))
                .toEqual("http://soundcloud.com/angelika-ochmann?url=foo");

        expect(ResolveFetchTask.fixClickTrackingUrl(
                Uri.parse("soundcloud:users:123")))
                .toEqual("soundcloud:users:123");


        expect(ResolveFetchTask.fixClickTrackingUrl(
                Uri.parse("http://unrelated.com")))
                .toEqual("http://unrelated.com");
    }
}
