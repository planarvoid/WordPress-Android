package com.soundcloud.android.model;

import static com.soundcloud.android.AndroidCloudAPI.CloudDateFormat.toTime;
import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.addPendingHttpResponse;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.SyncAdapterServiceTest;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class ScModelManagerTest {
    ScModelManager manager;
    ContentResolver resolver;
    final static long USER_ID = 1L;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        manager = new ScModelManager(Robolectric.application, TestHelper.getObjectMapper());
        resolver = Robolectric.application.getContentResolver();
    }

    @Test
    public void testShareTrack() {
        Track track = new Track();
        track.id = 1234;
        track.bpm = 120f;
        manager.cache(track, ScResource.CacheUpdateMode.FULL);

        Track track2 = new Track();
        track2.id = 1234;
        track2.duration = 9876;
        Track t = manager.cache(track2, ScResource.CacheUpdateMode.FULL);

        expect(t.bpm).toEqual(track.bpm);
        expect(t.duration).toEqual(track.duration);
    }

    // TODO, one instance of every user. deserialize post processing
    @Test
    public void testUniqueUserMultipleTracks() throws IOException {
        CollectionHolder<ScResource> holder = manager.getCollectionFromStream(SyncAdapterServiceTest.class.getResourceAsStream("tracks.json"));
        expect(holder.size()).toBe(3);

        Track t1 = (Track) holder.get(0);
        Track t2 = (Track) holder.get(1);
        Track t3 = (Track) holder.get(2);
        t2.user.country = "North Korea";
        t3.user.full_name = "Kim";

        expect(t1.user.country).toBe("North Korea");
        expect(t1.user.full_name).toBe("Kim");
    }


    @Test
    public void shouldGetUserById() throws Exception {
        User u = new User();
        u.id = 100L;
        u.permalink = "foo";

        Uri uri = manager.write(u);

        expect(uri).not.toBeNull();

        User u2 = manager.getUser(100);
        expect(u2).not.toBeNull();
        expect(u2.id).toEqual(u.id);
        expect(u2.permalink).toEqual(u.permalink);
    }

    @Test
    public void shouldNotGetUserByIdNegative() throws Exception {
        expect(manager.getUser(-1l)).toBeNull();
    }

    @Test
    public void shouldInsertTrack() throws Exception {
        Track t1 = new Track();
        t1.id = 100L;
        t1.title = "testing";
        t1.user = new User();
        t1.user.id = 200L;
        t1.user.username = "Testor";

        Uri uri = manager.write(t1);

        expect(uri).not.toBeNull();
        Track t2 = manager.getTrack(uri);

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

        Uri uri = manager.write(t1);

        expect(uri).not.toBeNull();
        Track t2 = manager.getTrack(uri);
        expect(t2).not.toBeNull();
        t2.title = "not interesting";

        manager.write(t2);

        Track t3 = manager.getTrack(uri);
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
        u.track_count = ++counter;
        u.followers_count = ++counter;
        u.followings_count = ++counter;
        u.public_likes_count = ++counter;
        u.private_tracks_count = ++counter;

        Uri uri = manager.write(u);

        expect(uri).not.toBeNull();

        User u2 = manager.getUser(uri);
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
        expect(u2.public_likes_count).toEqual(u.public_likes_count);
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

        Uri uri = manager.write(u);
        expect(uri).not.toBeNull();

        User u2 = manager.getUser(uri);
        expect(u2).not.toBeNull();
        expect(u2.description).toEqual("i make beatz");
    }

    @Test
    public void shouldUpsertUser() throws Exception {
        User u = new User();
        u.id = 100L;
        u.permalink = "foo";
        u.description = "baz";

        Uri uri = manager.write(u);

        expect(uri).not.toBeNull();

        User u2 = manager.getUser(uri);

        u2.permalink = "nomnom";

        manager.write(u2);

        User u3 = manager.getUser(uri);

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
        Uri uri = manager.write(track);
        expect(uri).not.toBeNull();

        final int PLAYS = 3;
        for (int i = 0; i < PLAYS; i++)
            expect(manager.markTrackAsPlayed(track)).toBeTrue();

        Cursor c = resolver.query(Content.TRACK_PLAYS.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToFirst()).toBeTrue();

        expect(c.getLong(c.getColumnIndex(DBHelper.TrackMetadata._ID))).toEqual(100L);
        expect(c.getLong(c.getColumnIndex(DBHelper.TrackMetadata.USER_ID))).toEqual(USER_ID);
        expect(c.getInt(c.getColumnIndex(DBHelper.TrackMetadata.PLAY_COUNT))).toEqual(PLAYS);

        Track played = manager.getTrack(100L);
        expect(played.local_user_playback_count).toEqual(PLAYS);
    }


    @Test
    public void shouldGetLocalIds() throws Exception {
        final int SIZE = 107;
        final long USER_ID = 1L;
        ContentValues[] cv = new ContentValues[SIZE];
        for (int i = 0; i < SIZE; i++) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.CollectionItems.POSITION, i);
            cv[i].put(DBHelper.CollectionItems.ITEM_ID, i);
            cv[i].put(DBHelper.CollectionItems.USER_ID, USER_ID);
        }

        resolver.bulkInsert(Content.ME_LIKES.uri, cv);

        expect(manager.getLocalIds(Content.ME_LIKES, USER_ID).size()).toEqual(107);
        List<Long> localIds = manager.getLocalIds(Content.ME_LIKES, USER_ID, 50, -1);

        expect(localIds.size()).toEqual(57);
        expect(localIds.get(0)).toEqual(50L);

        localIds = manager.getLocalIds(Content.ME_LIKES, USER_ID, 100, 50);
        expect(localIds.size()).toEqual(7);
        expect(localIds.get(0)).toEqual(100L);
    }

    @Test
    public void shouldBulkInsert() throws Exception {
        List<ScResource> items = createModels();
        expect(manager.writeCollection(items, ScResource.CacheUpdateMode.MINI)).toEqual(3);
    }

    @Test
    public void shouldBulkInsertWithCollections() throws Exception {
        List<Track> items = createTracks();
        expect(manager.writeCollection(items, Content.ME_LIKES.uri, USER_ID, ScResource.CacheUpdateMode.MINI)).toEqual(6);

        Cursor c = resolver.query(Content.ME_LIKES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotBulkInsertWithoutOwnerId() throws Exception {
        manager.writeCollection(createModels(), Content.ME_LIKES.uri, -1, ScResource.CacheUpdateMode.NONE);
    }

    private List<ScResource> createModels() {
        List<ScResource> items = new ArrayList<ScResource>();

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

    private List<Track> createTracks() {
        List<Track> items = new ArrayList<Track>();

        User u1 = new User();
        u1.permalink = "u1";
        u1.id = 100L;

        Track t = new Track();
        t.id = 200L;
        t.user = u1;

        User u2 = new User();
        u2.permalink = "u2";
        u2.id = 300L;


        Track t2 = new Track();
        t2.id = 400;
        t2.user = u2;

        items.add(t);
        items.add(t2);
        return items;
    }

    @Test
    public void shouldPersistPlaylistInDb() throws Exception {
        Playlist p = manager.getModelFromStream(SyncAdapterServiceTest.class.getResourceAsStream("playlist.json"));
        expect(p).not.toBeNull();
        expect(p.user.username).toEqual("Natalie");
        expect(p.tracks.size()).toEqual(41);

        Uri actualUri = p.insert(resolver);
        expect(actualUri)
                .toEqual(Uri.parse("content://com.soundcloud.android.provider.ScContentProvider/playlists/2524386"));

        Long id = Long.parseLong(actualUri.getLastPathSegment());
        Playlist p2 = manager.getPlaylistWithTracks(id);
        expect(p2).not.toBeNull();
        expect(p2.user.username).toEqual("Natalie");
        expect(p2.tracks.size()).toEqual(41);
        expect(p.tracks.get(0).id).toEqual(p2.tracks.get(0).id);

        p2.tracks.remove(0);
        expect(p2.insert(resolver)).not.toBeNull();

        Playlist p3 = manager.loadPlaylistFromUri(resolver, id, true);
        expect(p3).not.toBeNull();
        expect(p3.tracks.size()).toEqual(40);
        expect(p3.tracks.get(0).id).not.toEqual(p.tracks.get(0).id);

    }

    @Test
    public void shouldPersistActivitiesInDb() throws Exception {
        Activities a = manager.getActivitiesFromJson(
                SyncAdapterServiceTest.class.getResourceAsStream("e1_stream_1.json"));
        expect(a.insert(Content.ME_SOUND_STREAM, resolver)).toBe(22);

        expect(Content.ME_SOUND_STREAM).toHaveCount(22);
        expect(Activities.getSince(Content.ME_SOUND_STREAM,
                                resolver, -1).size()).toEqual(22);
    }


    @Test
    public void shouldGetActivitiesFromDBWithTimeFiltering() throws Exception {
        Activities a = manager.getActivitiesFromJson(
                SyncAdapterServiceTest.class.getResourceAsStream("e1_stream_1.json"));
        a.insert(Content.ME_SOUND_STREAM, resolver);
        expect(Content.ME_SOUND_STREAM).toHaveCount(22);

        expect(
                Activities.getSince(Content.ME_SOUND_STREAM,
                        resolver,
                        toTime("2012/09/27 14:08:01 +0000")).size()
        ).toEqual(2);
    }

    @Test
    public void shouldGetLastActivity() throws Exception {
        Activities a = manager.getActivitiesFromJson(
                SyncAdapterServiceTest.class.getResourceAsStream("e1_stream_1.json"));
        a.insert(Content.ME_SOUND_STREAM, resolver);
        expect(Content.ME_SOUND_STREAM).toHaveCount(22);

        expect(
                Activities.getLastActivity(Content.ME_SOUND_STREAM,
                        resolver).created_at.getTime()
        ).toEqual(toTime("2012/09/26 14:52:27 +0000"));
    }

    @Test
    public void shouldClearAllActivities() throws Exception {
        Activities a = manager.getActivitiesFromJson(
                SyncAdapterServiceTest.class.getResourceAsStream("e1_stream_1.json"));

        a.insert(Content.ME_SOUND_STREAM, resolver);
        expect(Content.ME_SOUND_STREAM).toHaveCount(22);

        LocalCollection.insertLocalCollection(Content.ME_SOUND_STREAM.uri,
                0, System.currentTimeMillis(), System.currentTimeMillis(), a.size(), a.future_href,
                resolver);

        Activities.clear(null, resolver);

        expect(Content.ME_SOUND_STREAM).toHaveCount(0);
        expect(Content.COLLECTIONS).toHaveCount(0);
    }

    @Test
    public void shouldWriteMissingCollectionItems() throws Exception {
        addPendingHttpResponse(getClass(), "5_users.json");

        List<ScResource> users = new ArrayList<ScResource>();
        for (int i = 0; i < 2; i++){
            users.add(createUserWithId(i));
        }

        ArrayList<Long> ids = new ArrayList<Long>();
        for (long i = 0; i < 10; i++){
            ids.add(i);
        }

        expect(manager.writeCollection(users, ScResource.CacheUpdateMode.MINI)).toEqual(2);
        expect(manager.fetchMissingCollectionItems((AndroidCloudAPI) Robolectric.application, ids, Content.USERS, false, 5)).toEqual(5);
    }

    private User createUserWithId(long id){
        User u = new User();
        u.id = id;
        return u;
    }
}
