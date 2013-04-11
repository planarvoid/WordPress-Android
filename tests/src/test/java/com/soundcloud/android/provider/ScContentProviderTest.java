package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.provider.ScContentProvider.Parameter.*;
import static com.soundcloud.android.robolectric.TestHelper.getActivities;
import static com.soundcloud.android.robolectric.TestHelper.readJson;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.dao.TrackStorage;
import com.soundcloud.android.dao.UserDAO;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Shortcut;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.ApiSyncServiceTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.accounts.Account;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.PeriodicSync;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class ScContentProviderTest {
    static final long USER_ID = 100L;
    ContentResolver resolver;
    ActivitiesStorage activitiesStorage;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        activitiesStorage = new ActivitiesStorage(DefaultTestRunner.application);
    }

    @Test
    public void shouldInsertAndQueryRecordings() throws Exception {
        Recording r = Recording.create();
        r.user_id = USER_ID;

        Uri uri = resolver.insert(Content.RECORDINGS.uri, r.buildContentValues());
        expect(uri).not.toBeNull();

        Cursor c = resolver.query(Content.RECORDINGS.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
    }

    @Test
    public void shouldCleanupTracks() throws Exception {
        TrackHolder tracks = readJson(TrackHolder.class, "/com/soundcloud/android/provider/user_favorites.json");
        int i = 0;
        for (Track t : tracks) {
            TestHelper.insertAsSoundAssociation(t, i < tracks.size() / 2 ? SoundAssociation.Type.TRACK_LIKE : SoundAssociation.Type.TRACK_REPOST);
            i++;
        }

        expect(Content.TRACKS).toHaveCount(15);
        expect(Content.USERS).toHaveCount(14);

        Playlist playlist  = readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        TestHelper.insertAsSoundAssociation(playlist, SoundAssociation.Type.PLAYLIST_LIKE);

        expect(Content.TRACKS).toHaveCount(56); // added 41 from playlist
        expect(Content.PLAYLISTS).toHaveCount(1);
        expect(Content.USERS).toHaveCount(46); // added 32 from playlist

        // insert extra tracks that should be cleaned up
        tracks = readJson(TrackHolder.class, "/com/soundcloud/android/provider/tracks.json");
        TestHelper.bulkInsert(tracks.collection);

        expect(Content.TRACKS).toHaveCount(59);
        expect(Content.PLAYLISTS).toHaveCount(1);
        expect(Content.USERS).toHaveCount(47);

        resolver.update(Content.PLAYABLE_CLEANUP.uri, null, null, null);
        resolver.update(Content.USERS_CLEANUP.uri, null, null, null);

        expect(Content.TRACKS).toHaveCount(56);
        expect(Content.PLAYLISTS).toHaveCount(1);
        expect(Content.USERS).toHaveCount(46);
        expect(Content.PLAYLIST_ALL_TRACKS).toHaveCount(41); // playlist > track relational
    }

    @Test
    public void shouldCleanupPlaylist() throws Exception {
        TrackHolder tracks = readJson(TrackHolder.class, "/com/soundcloud/android/provider/user_favorites.json");
        int i = 0;
        for (Track t : tracks) {
            TestHelper.insertAsSoundAssociation(t, i < tracks.size() / 2 ? SoundAssociation.Type.TRACK_LIKE : SoundAssociation.Type.TRACK_REPOST);
            i++;
        }
        expect(Content.TRACKS).toHaveCount(15);
        expect(Content.USERS).toHaveCount(14);

        Playlist playlist = readJson(Playlist.class, "/com/soundcloud/android/service/sync/playlist.json");
        TestHelper.insertWithDependencies(playlist);

        expect(Content.TRACKS).toHaveCount(56); // added 41 from playlist
        expect(Content.PLAYLISTS).toHaveCount(1);
        expect(Content.USERS).toHaveCount(46); // added 32 from playlist

        expect(Content.PLAYLIST_ALL_TRACKS).toHaveCount(41); // playlist > track relational

        resolver.update(Content.PLAYABLE_CLEANUP.uri, null, null, null);
        resolver.update(Content.USERS_CLEANUP.uri, null, null, null);

        expect(Content.TRACKS).toHaveCount(15);
        expect(Content.PLAYLISTS).toHaveCount(0);
        expect(Content.USERS).toHaveCount(14);
        expect(Content.PLAYLIST_ALL_TRACKS).toHaveCount(0);
    }



    @Test
    public void shouldCleanupSoundStream() throws Exception {
        Activities a = getActivities("/com/soundcloud/android/service/sync/e1_stream.json");

        expect(activitiesStorage.insert(Content.ME_SOUND_STREAM, a)).toBe(50);
        expect(Content.ME_SOUND_STREAM).toHaveCount(50);
        expect(resolver.update(Uri.parse(Content.SOUND_STREAM_CLEANUP.uri.toString() + "?limit=10"), null, null, null)).toBe(40);
    }

    @Test
    public void shouldCleanupActivities() throws Exception {
        Activities a = getActivities("/com/soundcloud/android/service/sync/e1_activities.json");

        expect(activitiesStorage.insert(Content.ME_ACTIVITIES, a)).toBe(17);
        expect(Content.ME_ACTIVITIES).toHaveCount(17);
        expect(resolver.update(Uri.parse(Content.ACTIVITIES_CLEANUP.uri.toString() + "?limit=5"), null, null, null)).toBe(12);
    }

    @Test
    public void shouldIncludeUserPermalinkInTrackView() throws Exception {
        Activities activities = getActivities("/com/soundcloud/android/service/sync/e1_stream_1.json");

        for (Playable t : activities.getUniquePlayables()) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.TRACK.uri, t.buildContentValues())).not.toBeNull();
        }

        expect(Content.TRACK).toHaveCount(20);
        expect(Content.USERS).toHaveCount(11);
        Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(61350393l);

        expect(t).not.toBeNull();
        expect(t.user.permalink).toEqual("westafricademocracyradio");
        expect(t.permalink).toEqual("info-chez-vous-2012-27-09");
    }


    @Test
    public void shouldSupportAndroidGlobalSearch() throws Exception {
        Shortcut[] shortcuts = readJson(Shortcut[].class, "/com/soundcloud/android/service/sync/all_shortcuts.json");

        List<ContentValues> cvs = new ArrayList<ContentValues>();
        for (Shortcut shortcut : shortcuts) {
            ContentValues cv = shortcut.buildContentValues();
            if (cv != null) cvs.add(cv);
        }
        int inserted = resolver.bulkInsert(Content.ME_SHORTCUTS.uri, cvs.toArray(new ContentValues[cvs.size()]));
        expect(inserted).toEqual(cvs.size());
        expect(Content.ME_SHORTCUTS).toHaveCount(inserted);


        Cursor cursor = resolver.query(Content.ANDROID_SEARCH_SUGGEST.uri,
                null, null, new String[] { "blac" }, null);

        expect(cursor.getCount()).toEqual(4);  // 2 followings + 2 likes
        expect(cursor.moveToFirst()).toBeTrue();

        expect(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))).not.toEqual(0l);
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1))).toEqual("The Black Dog");
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA))).toEqual("content://com.soundcloud.android.provider.ScContentProvider/users/950");
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1))).toMatch("content://com.soundcloud.android.provider.ScContentProvider/me/shortcut_icon/(\\d+)");

        expect(cursor.moveToNext()).toBeTrue();
        expect(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))).not.toEqual(0l);
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1))).toEqual("Blackest Ever Black");
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA))).toEqual("content://com.soundcloud.android.provider.ScContentProvider/users/804339");
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1))).toMatch("content://com.soundcloud.android.provider.ScContentProvider/me/shortcut_icon/(\\d+)");

        expect(cursor.moveToNext()).toBeTrue();
        expect(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))).not.toEqual(0l);
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1))).toEqual("The Black Dog - Industrial Smokers Behind The Factory Wall");
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA))).toEqual("content://com.soundcloud.android.provider.ScContentProvider/tracks/25273712");
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1))).toMatch("content://com.soundcloud.android.provider.ScContentProvider/me/shortcut_icon/(\\d+)");

        expect(cursor.moveToNext()).toBeTrue();
        expect(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))).not.toEqual(0l);
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1))).toEqual("CBLS 119 - Compost Black Label Sessions Radio hosted by SHOW-B & Thomas Herb");
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA))).toEqual("content://com.soundcloud.android.provider.ScContentProvider/tracks/24336214");
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1))).toMatch("content://com.soundcloud.android.provider.ScContentProvider/me/shortcut_icon/(\\d+)");
    }

    @Test
    public void shouldGetCurrentUserId() throws Exception {
        Cursor c = resolver.query(Content.ME_USERID.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToFirst()).toBeTrue();
        expect(c.getLong(0)).toEqual(USER_ID);
    }

    @Test
    public void shouldCreateAndDeleteARecording() throws Exception {
        Recording r = Recording.create();
        r.user_id = USER_ID;
        Uri uri = resolver.insert(Content.RECORDINGS.uri, r.buildContentValues());
        expect(uri).not.toBeNull();

        expect(Content.RECORDINGS).toHaveCount(1);
        expect(resolver.delete(uri, null, null)).toEqual(1);
        expect(Content.RECORDINGS).toBeEmpty();
    }


    @Test
    public void shouldInsertTrackMetadata() throws Exception {
        ContentValues values = new ContentValues();
        values.put(DBHelper.TrackMetadata._ID, 20);
        values.put(DBHelper.TrackMetadata.ETAG, "123456");
        values.put(DBHelper.TrackMetadata.CACHED, 1);

        Uri result = resolver.insert(Content.TRACK_METADATA.uri, values);
        expect(result).toEqual("content://com.soundcloud.android.provider.ScContentProvider/track_metadata/20");
    }

    @Test
    public void shouldHaveATracksEndpointWithRandom() throws Exception {
        TrackHolder tracks  = readJson(TrackHolder.class, "/com/soundcloud/android/provider/user_favorites.json");

        for (Track t : tracks) {
            expect(TestHelper.insertAsSoundAssociation(t, SoundAssociation.Type.TRACK_LIKE)).not.toBeNull();
        }
        Cursor c = resolver.query(Content.TRACK.withQuery(RANDOM, "0"), null, null, null, null);
        expect(c.getCount()).toEqual(15);
        List<Long> sorted = new ArrayList<Long>();
        List<Long> random = new ArrayList<Long>();
        while (c.moveToNext()) {
            sorted.add(c.getLong(c.getColumnIndex(DBHelper.SoundView._ID)));
        }
        Cursor c2 = resolver.query(Content.TRACK.withQuery(RANDOM, "1"), null, null, null, null);
        expect(c2.getCount()).toEqual(15);
        while (c2.moveToNext()) {
            random.add(c2.getLong(c2.getColumnIndex(DBHelper.SoundView._ID)));
        }
        expect(sorted).not.toEqual(random);
    }

    @Test
    public void shouldHaveATracksEndpointWhichReturnsOnlyCachedItems() throws Exception {
        TrackHolder tracks  = readJson(TrackHolder.class, "/com/soundcloud/android/provider/user_favorites.json");
        for (Track t : tracks) {
            expect(TestHelper.insertAsSoundAssociation(t, SoundAssociation.Type.TRACK_LIKE)).not.toBeNull();
        }

        ContentValues cv = new ContentValues();
        final long cachedId = 27583938l;
        cv.put(DBHelper.TrackMetadata._ID, cachedId);
        cv.put(DBHelper.TrackMetadata.CACHED, 1);
        resolver.insert(Content.TRACK_METADATA.uri, cv);

        Uri uri = Content.TRACKS.withQuery(CACHED, "1");
        Cursor c = resolver.query(uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToNext()).toBeTrue();
        expect(c.getLong(c.getColumnIndex(DBHelper.SoundView._ID))).toEqual(cachedId);
    }

    @Test
    public void shouldHaveFavoriteEndpointWhichOnlyReturnsCachedItems() throws Exception {
        List<Track> tracks = TestHelper.readResourceList("/com/soundcloud/android/provider/user_favorites.json");
        for (Track t : tracks) {
            expect(TestHelper.insertAsSoundAssociation(t, SoundAssociation.Type.TRACK_LIKE)).not.toBeNull();
        }

        ContentValues cv = new ContentValues();
        final long cachedId = 27583938l;
        cv.put(DBHelper.TrackMetadata._ID, cachedId);
        cv.put(DBHelper.TrackMetadata.CACHED, 1);
        resolver.insert(Content.TRACK_METADATA.uri, cv);

        Uri uri = Content.ME_LIKES.withQuery(CACHED, "1");
        Cursor c = resolver.query(uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToNext()).toBeTrue();
        expect(c.getLong(c.getColumnIndex(DBHelper.SoundView._ID))).toEqual(cachedId);
    }

    @Test
    public void shouldHaveFavoriteEndpointWhichReturnsRandomItems() throws Exception {
        TrackHolder tracks  = readJson(TrackHolder.class, "/com/soundcloud/android/provider/user_favorites.json");

        for (Track t : tracks) {
            expect(TestHelper.insertAsSoundAssociation(t, SoundAssociation.Type.TRACK_LIKE)).not.toBeNull();
        }

        Uri uri = Content.ME_LIKES.withQuery(RANDOM, "1", LIMIT, "5");
        Cursor c = resolver.query(uri, null, null, null, null);
        expect(c.getCount()).toEqual(5);

        long[] sorted = new long[] { 13403434, 18071729, 18213041, 18571159, 19658312 };
        long[] result = new long[sorted.length];
        int i = 0;
        while(c.moveToNext()) {
            result[i++] = c.getLong(c.getColumnIndex(DBHelper.SoundView._ID));
        }
        expect(Arrays.equals(result, sorted)).toBeFalse();
    }

    @Test
    public void shouldHaveStreamEndpointWhichReturnsRandomItems() throws Exception {
        // TODO: find easier way to populate stream
        ApiSyncService svc = new ApiSyncService();
        TestHelper.addPendingHttpResponse(ApiSyncServiceTest.class, "e1_stream_2_oldest.json");
        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME_SOUND_STREAM.uri), 1);
        expect(Content.ME_SOUND_STREAM).toHaveCount(28);

        ContentValues cv = new ContentValues();
        final long firstId = 18508668l;
        cv.put(DBHelper.TrackMetadata._ID, firstId);
        cv.put(DBHelper.TrackMetadata.CACHED, 1);
        resolver.insert(Content.TRACK_METADATA.uri, cv);

        Uri uri = Content.ME_SOUND_STREAM.withQuery(RANDOM, "1", LIMIT, "5");
        Cursor c = resolver.query(uri, null, null, null, null);
        expect(c.getCount()).toEqual(5);
        long[] sorted = new long[] {61467451, 61465333, 61454101, 61451011, 61065502};
        long[] result = new long[sorted.length];
        int i=0;
        while (c.moveToNext()) {
            result[i++] = c.getLong(c.getColumnIndex(DBHelper.ActivityView.SOUND_ID));
        }
        expect(Arrays.equals(result, sorted)).toBeFalse();
    }

    @Test
    public void shouldHaveStreamEndpointWhichOnlyReturnsCachedItems() throws Exception {
        // TODO: find easier way to populate stream
        ApiSyncService svc = new ApiSyncService();
        TestHelper.addPendingHttpResponse(ApiSyncServiceTest.class, "e1_stream_2_oldest.json");
        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME_SOUND_STREAM.uri), 1);
        expect(Content.ME_SOUND_STREAM).toHaveCount(28);

        ContentValues cv = new ContentValues();
        final long cachedId = 61467451l;
        cv.put(DBHelper.TrackMetadata._ID, cachedId);
        cv.put(DBHelper.TrackMetadata.CACHED, 1);
        resolver.insert(Content.TRACK_METADATA.uri, cv);

        Uri uri = Content.ME_SOUND_STREAM.withQuery(CACHED, "1");
        Cursor c = resolver.query(uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToNext()).toBeTrue();
        expect(c.getLong(c.getColumnIndex(DBHelper.ActivityView.SOUND_ID))).toEqual(cachedId);
    }


    @Test
    public void shouldEnableSyncing() throws Exception {
        TestHelper.setSdkVersion(8);
        Account account = new Account("name", "type");
        ScContentProvider.enableSyncing(account, 3600);

        expect(ContentResolver.getSyncAutomatically(account, ScContentProvider.AUTHORITY)).toBeTrue();

        List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(account, ScContentProvider.AUTHORITY);
        expect(syncs.size()).toEqual(1);

        final PeriodicSync sync = syncs.get(0);
        expect(sync.account).toEqual(account);
        expect(sync.period).toEqual(3600l);
    }

    @Test
    public void shouldDisableSyncing() throws Exception {
        TestHelper.setSdkVersion(8);
        Account account = new Account("name", "type");
        ScContentProvider.enableSyncing(account, 3600);
        List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(account, ScContentProvider.AUTHORITY);
        expect(syncs.size()).toEqual(1);

        ScContentProvider.disableSyncing(account);

        syncs = ContentResolver.getPeriodicSyncs(account, ScContentProvider.AUTHORITY);
        expect(syncs.isEmpty()).toBeTrue();
        expect(ContentResolver.getSyncAutomatically(account, ScContentProvider.AUTHORITY)).toBeFalse();
    }

    @Test
    public void shouldBulkInsertSuggestions() throws Exception {
        Shortcut[] shortcuts = readJson(Shortcut[].class, "/com/soundcloud/android/service/sync/all_shortcuts.json");

        List<ContentValues> cvs = new ArrayList<ContentValues>();
        for (Shortcut shortcut : shortcuts) {
            ContentValues cv = shortcut.buildContentValues();
            if (cv != null) cvs.add(cv);
        }

        int inserted = resolver.bulkInsert(Content.ME_SHORTCUTS.uri, cvs.toArray(new ContentValues[cvs.size()]));
        expect(inserted).toEqual(cvs.size());
        expect(Content.ME_SHORTCUTS).toHaveCount(inserted);

        // reinsert same batch, make sure no dups
        inserted = resolver.bulkInsert(Content.ME_SHORTCUTS.uri, cvs.toArray(new ContentValues[cvs.size()]));
        expect(inserted).toEqual(cvs.size());
        expect(Content.ME_SHORTCUTS).toHaveCount(inserted);

        expect(Content.USERS).toHaveCount(318);
        expect(Content.TRACKS).toHaveCount(143);

        User u = new UserDAO(resolver).queryById(9);

        expect(u).not.toBeNull();
        expect(u.username).toEqual("Katharina");
        expect(u.avatar_url).toEqual("https://i1.sndcdn.com/avatars-000013690441-hohfv1-tiny.jpg?2479809");
        expect(u.permalink_url).toEqual("http://soundcloud.com/katharina");

        Track t = new TrackStorage(DefaultTestRunner.application).getTrack(64629168);
        expect(t).not.toBeNull();
        expect(t.title).toEqual("Halls - Roses For The Dead (Max Cooper remix)");
        expect(t.artwork_url).toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809");
        expect(t.permalink_url).toEqual("http://soundcloud.com/no-pain-in-pop/halls-roses-for-the-dead-max");
    }

    @Test
    public void shouldStoreAndFetchShortcut() throws Exception {
        Shortcut c = new Shortcut();
        c.kind = "like";
        c.id = 12;
        c.title = "Something";
        c.permalink_url = "http://soundcloud.com/foo";
        c.artwork_url   = "http://soundcloud.com/foo/artwork";


        Uri uri = resolver.insert(Content.ME_SHORTCUTS.uri, c.buildContentValues());
        expect(uri).not.toBeNull();

        Cursor cursor = resolver.query(uri, null, null, null, null);
        expect(cursor.getCount()).toEqual(1);
        expect(cursor.moveToFirst()).toBeTrue();

        expect(cursor.getString(cursor.getColumnIndex(DBHelper.Suggestions.ICON_URL))).toEqual("http://soundcloud.com/foo/artwork");
    }



}
