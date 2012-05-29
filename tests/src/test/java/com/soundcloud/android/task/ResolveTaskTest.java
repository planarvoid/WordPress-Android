package com.soundcloud.android.task;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;
import static org.junit.Assert.fail;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Env;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.net.Uri;


@RunWith(DefaultTestRunner.class)
public class ResolveTaskTest {
    ResolveTask.ResolveListener listener;

    @Test
    public void testResolving() throws Exception {
        ResolveTask task = new ResolveTask(DefaultTestRunner.application);

        addHttpResponseRule("GET", "/resolve?url=http%3A%2F%2Fsoundcloud.com%2Ffoo%2Fbar",
                new TestHttpResponse(302, "", new BasicHeader("Location", "http://foo.com")));

        final Uri[] result = {null};
        listener = new ResolveTask.ResolveListener() {
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
                Uri.parse("soundcloud:users:1234"), Env.SANDBOX))
                .toEqual(Uri.parse("https://api.sandbox-soundcloud.com/users/1234"));

        expect(ResolveTask.resolveSoundCloudURI(
                Uri.parse("soundcloud:users:1234#show"), Env.SANDBOX))
                .toEqual(Uri.parse("https://api.sandbox-soundcloud.com/users/1234"));

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


    @Test
    public void shouldResolveWithAction() throws Exception {
        final Uri[] uri = {null};
        final String[] action = {null};

        listener = new ResolveTask.ResolveListener() {
            @Override public void onUrlResolved(Uri _uri, String _action) {
                uri[0] = _uri;
                action[0] = _action;
            }
            @Override public void onUrlError() {
                fail("error");
            }
        };
        ResolveTask.resolveSoundCloudURI(Uri.parse("soundcloud:users:1234#show"), Env.LIVE, listener);
        expect(uri[0]).toEqual(Uri.parse("https://api.soundcloud.com/users/1234"));
        expect(action[0]).toEqual("show");
    }
}
