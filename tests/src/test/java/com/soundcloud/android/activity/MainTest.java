package com.soundcloud.android.activity;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.addPendingIOException;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.auth.AuthenticatorService;
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
    String action;
    Track track;
    User user;

    @Before
    public void before() {
        main = new Main() {
            @Override
            public void onError(long modelId) {
                error = true;
            }

            @Override
            public void onTrackLoaded(Track track, String action) {
                MainTest.this.track = track;
                super.onTrackLoaded(track, action);
            }

            @Override
            protected void onUserLoaded(User u, String action) {
                MainTest.this.user = u;
                super.onUserLoaded(u, action);
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
        addHttpResponseRule("GET", "/resolve?url=http%3A%2F%2Fsoundcloud.com%2Ftracks%2Fsometrack",
                new TestHttpResponse(302, "", new BasicHeader("Location", "https://api.soundcloud.com/tracks/12345")));

        TestHelper.addCannedResponse(getClass(), "/tracks/12345", "track.json");

        main.handleViewUrl(new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://soundcloud.com/tracks/sometrack")));

        expect(error).toBeFalse();
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
        expect(action).toBeNull();
    }

    @Test
    public void shouldHandleIOException() throws Exception {
        addPendingIOException(null);

        main.handleViewUrl(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://api.soundcloud.com/tracks/12345")));

        expect(error).toBeTrue();
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
        expect(action).toBeNull();
        expect(track).not.toBeNull();
        expect(track.id).toEqual(12345L);
        expect(track.title).toEqual("recording on sunday night");
    }

    @Test
    public void shouldResolveTrackUrlsWithSecretToken() throws Exception {

        addHttpResponseRule("GET", "/resolve?url=http%3A%2F%2Fsoundcloud.com%2Ftracks%2Fsometrack%2Fs-SECRET",
                new TestHttpResponse(302, "", new BasicHeader("Location", "https://api.soundcloud.com/tracks/12345?secret_token=s-SECRET")));

        TestHelper.addCannedResponse(getClass(), "/tracks/12345?secret_token=s-SECRET", "track.json");

        main.handleViewUrl(new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://soundcloud.com/tracks/sometrack/s-SECRET")));

        expect(error).toBeFalse();
        expect(action).toBeNull();
        expect(track).not.toBeNull();
        expect(track.id).toEqual(12345L);
    }

    @Test
    public void shouldHandleFacebookDeeplinkIntent() throws Exception {
        addHttpResponseRule("/resolve?url=http%3A%2F%2Fsoundcloud.com%2Fjohnpeelarchive%3Ffb_action_ids%3D10151612282280249%26fb_action_types%3Dsoundcloud%3Afollow%26fb_source%3Daggregation%26fb_aggregation_id%3D10150389352581799",
                new TestHttpResponse(302, "", new BasicHeader("Location", "https://api.soundcloud.com/users/12345")));

        TestHelper.addCannedResponse(getClass(), "/users/12345", "user.json");

        main.handleViewUrl(new Intent("com.facebook.application.19507961798",
                Uri.parse("http://soundcloud.com/johnpeelarchive?fb_action_ids=10151612282280249&fb_action_types=soundcloud:follow&fb_source=aggregation&fb_aggregation_id=10150389352581799")));

        expect(error).toBeFalse();
        expect(action).toBeNull();
        expect(user).not.toBeNull();
        expect(user.id).toEqual(3135930L);
        expect(user.username).toEqual("SoundCloud Android @ MWC");
    }

    @Test
    public void shouldGoToRecordAfterLoggingIn() throws Exception {
        Main main = new Main();
        main.setIntent(new Intent().putExtra(AuthenticatorService.KEY_ACCOUNT_RESULT, "sth"));
        main.onCreate(null);
        expect(main.getTabHost().getCurrentTabTag()).toEqual(Main.Tab.RECORD.tag);
    }

    @Test
    public void shouldGoToSpecificTab() throws Exception {
        Main main = new Main();
        main.setIntent(new Intent().putExtra(Main.TAB_TAG, Main.Tab.ACTIVITY.tag));
        main.onCreate(null);
        expect(main.getTabHost().getCurrentTabTag()).toEqual(Main.Tab.ACTIVITY.tag);
    }
}
