package com.soundcloud.android.activity;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.addPendingIOException;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;
import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class MainTest {
    Main main;
    boolean error;
    Uri resolved;
    String action;
    Track track;

    @Before
    public void before() {
        main = new Main() {
            public void onUrlResolved(Uri uri, String action) {
                MainTest.this.resolved = uri;
                MainTest.this.action = action;
                super.onUrlResolved(uri, action);
            }

            public void onUrlError() {
                error = true;
                super.onUrlError();
            }

            @Override
            public void onTrackInfoLoaded(Track track, String action) {
                MainTest.this.track = track;
                super.onTrackInfoLoaded(track, action);
            }
        };
    }

    @After
    public void after() {
        expect(Robolectric.getFakeHttpLayer().hasPendingResponses()).toBeFalse();
    }

    @Test
    public void shouldReturnFalseForEmptyUrl() throws Exception {
        expect(main.handleViewUrl(new Intent(Intent.ACTION_VIEW, null))).toBeFalse();
    }

    @Test
    public void shouldHandleViewUrlIntent() throws Exception {
        addHttpResponseRule("GET", "/resolve?url=https%3A%2F%2Fsoundcloud.com%2Ftracks%2Fsometrack",
                new TestHttpResponse(302, "", new BasicHeader("Location", "https://api.soundcloud.com/tracks/12345")));

        TestHelper.addCannedResponse(getClass(), "/tracks/12345", "track.json");

        main.handleViewUrl(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://soundcloud.com/tracks/sometrack")));

        expect(error).toBeFalse();
        expect(resolved.toString()).toEqual("https://api.soundcloud.com/tracks/12345");
        expect(action).toBeNull();
        expect(track).not.toBeNull();
        expect(track.id).toEqual(12345L);
    }

    @Test
    public void shouldHandleErrorCode() throws Exception {
        addHttpResponseRule("GET", "/resolve?url=https%3A%2F%2Fapi.soundcloud.com%2Ftracks%2F12345",
                new TestHttpResponse(500, ""));

        main.handleViewUrl(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://api.soundcloud.com/tracks/12345")));

        expect(error).toBeTrue();
        expect(resolved).toBeNull();
        expect(action).toBeNull();
    }

    @Test
    public void shouldHandleIOException() throws Exception {
        addPendingIOException(null);

        main.handleViewUrl(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://api.soundcloud.com/tracks/12345")));

        expect(error).toBeTrue();
        expect(resolved).toBeNull();
        expect(action).toBeNull();
    }

    @Test
    public void shouldResolveLocallyFirst() throws Exception {
        Track t = new Track();
        t.user = new User();
        t.user.id = 6789L;
        t.id = 12345L;
        t.title = "Testing";

        expect(SoundCloudDB.insertTrack(Robolectric.application.getContentResolver(), t)).not.toBeNull();
        main.handleViewUrl(new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud:tracks:12345")));

        expect(error).toBeFalse();
        expect(resolved).toBeNull();
        expect(action).toBeNull();
        expect(track).not.toBeNull();
        expect(track.id).toEqual(12345L);
        expect(track.title).toEqual("Testing");
    }

    @Test
    public void shouldFallbackToRemoteResolvingIfNotAvailableLocally() throws Exception {
        TestHelper.addCannedResponse(getClass(), "/tracks/12345", "track.json");

        main.handleViewUrl(new Intent(Intent.ACTION_VIEW, Uri.parse("soundcloud:tracks:12345")));

        expect(error).toBeFalse();
        expect(resolved.toString()).toEqual("https://api.soundcloud.com/tracks/12345");
        expect(action).toBeNull();
        expect(track).not.toBeNull();
        expect(track.id).toEqual(12345L);
        expect(track.title).toEqual("recording on sunday night");
    }
}
