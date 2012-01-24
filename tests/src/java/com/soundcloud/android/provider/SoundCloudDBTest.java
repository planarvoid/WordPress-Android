package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.SearchHistoryItem;
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

        expect(c.getLong(c.getColumnIndex(DBHelper.TrackPlays.TRACK_ID))).toEqual(100L);
        expect(c.getLong(c.getColumnIndex(DBHelper.TrackPlays.USER_ID))).toEqual(USER_ID);
        expect(c.getInt(c.getColumnIndex(DBHelper.TrackPlays.PLAY_COUNT))).toEqual(PLAYS);
    }


    @Test
    public void shouldAddSearches() throws Exception {
        Uri uri = SoundCloudDB.addSearch(resolver, 0, "A Query");
        expect(uri.toString()).toEqual("content://com.soundcloud.android.provider.ScContentProvider/searches/1");

        List<SearchHistoryItem> searches = SoundCloudDB.getSearches(resolver);
        expect(searches.size()).toEqual(1);

        SearchHistoryItem item = searches.get(0);
        expect(item.search_type).toEqual(0);
        expect(item.query).toEqual("A Query");
        expect(item.created_at).not.toEqual(0L);
    }

    @Test
    public void shouldNotAddDuplicateSearches() throws Exception {
        for (int i=0; i<5; i++) {
            SoundCloudDB.addSearch(resolver, 0, "A Query");
        }
        SoundCloudDB.addSearch(resolver, 0, "A different query");
        expect(SoundCloudDB.getSearches(resolver).size()).toEqual(2);
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

        Friend f = new Friend();
        f.user = u2;

        items.add(u1);
        items.add(f);
        items.add(t);
        items.add(u2_);
        return items;
    }
}
