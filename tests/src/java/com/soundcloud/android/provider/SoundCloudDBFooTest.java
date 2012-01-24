package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class SoundCloudDBFooTest {
    ContentResolver resolver;
    final static long USER_ID = 1L;

    @Before
    public void before() {
        resolver = DefaultTestRunner.application.getContentResolver();
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldInsertTrack() throws Exception {
        Track t1 = new Track();
        t1.id = 100L;
        t1.title = "testing";
        t1.user = new User();
        t1.user.id = 200L;
        t1.user.username = "Testor";

        Uri uri = SoundCloudDB.insertTrack(resolver, t1);

        expect(uri).not.toBeNull();
        Track t2 = SoundCloudDB.getTrackByUri(resolver, uri);

        expect(t2).not.toBeNull();
        expect(t2.user).not.toBeNull();
        expect(t2.user.username).toEqual("Testor");
        expect(t1.title).toEqual(t2.title);
    }

    @Test
    public void shouldUpsertTrack() throws Exception {
        Track t1 = new Track();
        t1.id = 100L;
        t1.title = "testing";
        t1.user = new User();
        t1.user.id = 200L;
        t1.user.username = "Testor";

        Uri uri = SoundCloudDB.insertTrack(resolver, t1);

        expect(uri).not.toBeNull();
        Track t2 = SoundCloudDB.getTrackByUri(resolver, uri);
        expect(t2).not.toBeNull();
        t2.title = "not interesting";

        SoundCloudDB.upsertTrack(resolver, t2);

        Track t3 = SoundCloudDB.getTrackByUri(resolver, uri);
        expect(t3.title).toEqual("not interesting");
    }

    @Test
    public void shouldInsertUser() throws Exception {
        User u = new User();
        u.id = 100L;
        u.permalink = "foo";
        u.description = "baz";

        Uri uri = SoundCloudDB.insertUser(resolver, u);

        expect(uri).not.toBeNull();

        User u2 = SoundCloudDB.getUserByUri(resolver, uri);
        expect(u2).not.toBeNull();
        expect(u2.permalink).toEqual(u.permalink);
        expect(u2.description).toBeNull();
    }

    @Test
    public void shouldInsertUserWithDescriptionIfCurrentUser() throws Exception {
        User u = new User();
        u.id = USER_ID;
        u.permalink = "foo";
        u.description = "i make beatz";

        Uri uri = SoundCloudDB.insertUser(resolver, u);
        expect(uri).not.toBeNull();

        User u2 = SoundCloudDB.getUserByUri(resolver, uri);
        expect(u2).not.toBeNull();
        expect(u2.description).toEqual("i make beatz");
    }

    @Test
    public void shouldUpsertUser() throws Exception {
        User u = new User();
        u.id = 100L;
        u.permalink = "foo";
        u.description = "baz";

        Uri uri = SoundCloudDB.insertUser(resolver, u);

        expect(uri).not.toBeNull();

        User u2 = SoundCloudDB.getUserByUri(resolver, uri);

        u2.permalink = "nomnom";

        SoundCloudDB.upsertUser(resolver, u2);

        User u3 = SoundCloudDB.getUserByUri(resolver, uri);

        expect(u3).not.toBeNull();
        expect(u3.permalink).toEqual("nomnom");
        expect(u3.id).toEqual(100L);
    }
}
