package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class SoundCloudDBTest {
    ContentResolver resolver;
    final static long USER_ID = 1L;

    @Before
    public void before() {
        resolver = DefaultTestRunner.application.getContentResolver();
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
    }

    @Test
    public void shouldGetUserById() throws Exception {
        User u = new User();
        u.id = 100L;
        u.permalink = "foo";

        Uri uri = SoundCloudDB.insertUser(resolver, u);

        expect(uri).not.toBeNull();

        User u2 = SoundCloudDB.getUserById(resolver, 100);
        expect(u2).not.toBeNull();
        expect(u2.id).toEqual(u.id);
        expect(u2.permalink).toEqual(u.permalink);
    }

    @Test
    public void shouldNotGetUserByIdNegative() throws Exception {
        expect(SoundCloudDB.getUserById(resolver, -1)).toBeNull();
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
    public void shouldInsertUserAndReadBackUser() throws Exception {
        User u = new User();
        u.id = 100L;
        u.full_name = "Bobby Fuller";
        u.permalink = "foo";
        u.description = "baz";
        u.city = "Somewhere";
        u.plan = "plan";
        u.website = "http://foo.com";
        u.website_title = "Site";
        u.setPrimaryEmailConfirmed(true);
        u.myspace_name = "myspace";
        u.discogs_name = "discogs";

        int counter = 0;
        u.track_count     = ++counter;
        u.followers_count = ++counter;
        u.followings_count = ++counter;
        u.public_favorites_count = ++counter;
        u.private_tracks_count = ++counter;

        Uri uri = SoundCloudDB.insertUser(resolver, u);

        expect(uri).not.toBeNull();

        User u2 = SoundCloudDB.getUserByUri(resolver, uri);
        expect(u2).not.toBeNull();
        expect(u2.full_name).toEqual(u.full_name);
        expect(u2.permalink).toEqual(u.permalink);
        expect(u2.city).toEqual(u.city);
        expect(u2.plan).toEqual(u.plan);
        expect(u2.website).toEqual(u.website);
        expect(u2.website_title).toEqual(u.website_title);
        expect(u2.isPrimaryEmailConfirmed()).toEqual(u.isPrimaryEmailConfirmed());
        expect(u2.myspace_name).toEqual(u.myspace_name);
        expect(u2.discogs_name).toEqual(u.discogs_name);

        expect(u2.track_count).toEqual(u.track_count);
        expect(u2.followers_count).toEqual(u.followers_count);
        expect(u2.followings_count).toEqual(u.followings_count);
        expect(u2.public_favorites_count).toEqual(u.public_favorites_count);
        expect(u2.private_tracks_count).toEqual(u.private_tracks_count);

        expect(u2.last_updated).not.toEqual(u.last_updated);

        // description is not store
        expect(u2.description).toBeNull();
    }

    @Test
    public void shouldInsertUserWithDescriptionIfCurrentUser() throws Exception {
        User u = new User();
        u.id = USER_ID;
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

    @Test
    public void shouldMarkTrackAsPlayed() throws Exception {
        Track track = new Track();
        track.id = 100L;
        track.title = "testing";
        track.user = new User();
        track.user.id = 200L;
        Uri uri = SoundCloudDB.insertTrack(resolver, track);
        expect(uri).not.toBeNull();

        final int PLAYS = 3;
        for (int i=0; i<PLAYS; i++)
            expect(SoundCloudDB.markTrackAsPlayed(resolver, track)).toBeTrue();

        Cursor c = resolver.query(Content.TRACK_PLAYS.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToFirst()).toBeTrue();

        expect(c.getLong(c.getColumnIndex(DBHelper.TrackMetadata._ID))).toEqual(100L);
        expect(c.getLong(c.getColumnIndex(DBHelper.TrackMetadata.USER_ID))).toEqual(USER_ID);
        expect(c.getInt(c.getColumnIndex(DBHelper.TrackMetadata.PLAY_COUNT))).toEqual(PLAYS);

        Track played = SoundCloudDB.getTrackById(resolver, 100L);
        expect(played.local_user_playback_count).toEqual(PLAYS);
    }


    @Test
    public void shouldBulkInsert() throws Exception {
        List<Parcelable> items = createParcelables();
        expect(SoundCloudDB.bulkInsertParcelables(resolver, items)).toEqual(3);
    }

    @Test
    public void shouldBulkInsertWithCollections() throws Exception {
        List<Parcelable> items = createParcelables();
        expect(SoundCloudDB.bulkInsertParcelables(resolver, items, Content.ME_FAVORITES.uri, USER_ID)).toEqual(3);

        Cursor c = resolver.query(Content.ME_FAVORITES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBulkInsertWithoutOwnerId() throws Exception {
        SoundCloudDB.bulkInsertParcelables(resolver, createParcelables(), Content.ME_FAVORITES.uri, -1);
    }

    private List<Parcelable> createParcelables() {
        List<Parcelable> items = new ArrayList<Parcelable>();

        User u1 = new User();
        u1.permalink = "u1";
        u1.id = 100L;

        Track t = new Track();
        t.id = 200L;
        t.user = u1;

        User u2 = new User();
        u2.permalink = "u2";
        u2.id = 300L;

        User u2_ = new User();
        u2_.permalink = "u2";
        u2_.id = 300L;

        items.add(u1);
        items.add(t);
        items.add(u2_);
        return items;
    }
}
