package com.soundcloud.android.deeplinks;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.api.legacy.Env;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;


@RunWith(DefaultTestRunner.class)
public class ResolveTaskTest {
    @Test
    public void testResolve() throws Exception {
        ResolveTask task = new ResolveTask(DefaultTestRunner.application.getCloudAPI());

        addHttpResponseRule(
                TestHelper.createRegexRequestMatcherForUriWithClientId(HttpGet.METHOD_NAME,
                        "/resolve?url=http%3A%2F%2Fsoundcloud.com%2Ffoo%2Fbar"),
                new TestHttpResponse(302, "", new BasicHeader("Location", "http://foo.com")));

        expect(task.execute(Uri.parse("http://soundcloud.com/foo/bar")).get()).toEqual(Uri.parse("http://foo.com"));
    }

    @Test
    public void testResolvingWithListener() throws Exception {
        ResolveTask task = new ResolveTask(DefaultTestRunner.application.getCloudAPI());

        addHttpResponseRule(
                TestHelper.createRegexRequestMatcherForUriWithClientId(HttpGet.METHOD_NAME,
                        "/resolve?url=http%3A%2F%2Fsoundcloud.com%2Ffoo%2Fbar"),

                new TestHttpResponse(302, "", new BasicHeader("Location", "http://foo.com")));

        final Uri[] result = {null};
        ResolveTask.ResolveListener listener = new ResolveTask.ResolveListener() {
            @Override public void onUrlResolved(Uri uri, String action) {
                result[0] = uri;
            }
            @Override public void onUrlError() {
            }
        };

        task.setListener(listener);
        task.execute(Uri.parse("http://soundcloud.com/foo/bar"));
        expect(result[0]).toEqual(Uri.parse("http://foo.com"));
    }

    @Test
    public void resolveSoundCloudUri() throws Exception {
        expect(ResolveTask.resolveSoundCloudURI(
                Uri.parse("soundcloud:users:1234"), Env.LIVE))
                .toEqual(Uri.parse("https://api.soundcloud.com/users/1234"));

        expect(ResolveTask.resolveSoundCloudURI(
                Uri.parse("soundcloud:tracks:1234"), Env.LIVE))
                .toEqual(Uri.parse("https://api.soundcloud.com/tracks/1234"));

        expect(ResolveTask.resolveSoundCloudURI(
                Uri.parse("soundcloud:tracks:1234#play"), Env.LIVE))
                .toEqual(Uri.parse("https://api.soundcloud.com/tracks/1234"));

        expect(ResolveTask.resolveSoundCloudURI(
                Uri.parse("SOUNDCLOUD:tracks:1234"), Env.LIVE))
                .toEqual(Uri.parse("https://api.soundcloud.com/tracks/1234"));

        expect(ResolveTask.resolveSoundCloudURI(
                Uri.parse("soundcloud:tracks"), Env.LIVE)).toBeNull();

        expect(ResolveTask.resolveSoundCloudURI(
                Uri.parse("soundcloud:users"), Env.LIVE)).toBeNull();


        expect(ResolveTask.resolveSoundCloudURI(
                Uri.parse("http://soundcloud.com/1234"), Env.LIVE)).toBeNull();


        expect(ResolveTask.resolveSoundCloudURI(
                Uri.parse("foobar:blaz"), Env.LIVE)).toBeNull();
    }
}
