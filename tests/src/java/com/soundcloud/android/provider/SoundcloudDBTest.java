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
public class SoundcloudDBTest {
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

        Uri uri = SoundCloudDB.insertTrack(resolver, t1, 0);

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

        Uri uri = SoundCloudDB.insertTrack(resolver, t1, 0);

        expect(uri).not.toBeNull();
        Track t2 = SoundCloudDB.getTrackByUri(resolver, uri);
        expect(t2).not.toBeNull();
        t2.title = "not interesting";

        SoundCloudDB.upsertTrack(resolver, t2, 0);

        Track t3 = SoundCloudDB.getTrackByUri(resolver, uri);
        expect(t3.title).toEqual("not interesting");
    }
}
