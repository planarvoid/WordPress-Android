package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.provider.ScContentProvider.Parameter.*;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.SyncAdapterService;
import com.soundcloud.android.service.sync.SyncAdapterServiceTest;
import com.xtremelabs.robolectric.RobolectricTestRunner;
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
import android.os.Build;
import android.provider.BaseColumns;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class ScContentProviderTest {
    static final long USER_ID = 100L;
    ContentResolver resolver;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
    }

    @Test
    public void shouldInsertAndQueryRecordings() throws Exception {
        Recording r = new Recording(new File("/tmp/test"));
        r.user_id = USER_ID;

        Uri uri = resolver.insert(Content.RECORDINGS.uri, r.buildContentValues());
        expect(uri).not.toBeNull();

        Cursor c = resolver.query(Content.RECORDINGS.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
    }

    @Test
    public void shouldInsertQueryAndDeleteFavorites() throws Exception {
        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);
        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_FAVORITES.uri, t.buildContentValues())).not.toBeNull();
        }

        Cursor c = resolver.query(Content.ME_FAVORITES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(15);

        resolver.delete(Content.ME_FAVORITES.uri, DBHelper.CollectionItems.ITEM_ID + " = ?",
                new String[]{String.valueOf(tracks.get(0).id)});

        c = resolver.query(Content.ME_FAVORITES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(14);
    }

    @Test
    public void shouldCleanup() throws Exception {
        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_FAVORITES.uri, t.buildContentValues())).not.toBeNull();
        }

        expect(resolver.query(Content.TRACKS.uri, null, null, null, null).getCount()).toEqual(15);
        expect(resolver.query(Content.USERS.uri, null, null, null, null).getCount()).toEqual(14);

        tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("tracks.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.TRACKS.uri, t.buildContentValues())).not.toBeNull();
        }

        expect(resolver.query(Content.TRACKS.uri, null, null, null, null).getCount()).toEqual(18);
        expect(resolver.query(Content.USERS.uri, null, null, null, null).getCount()).toEqual(15);

        resolver.update(Content.TRACK_CLEANUP.uri, null, null, null);
        expect(resolver.query(Content.TRACKS.uri, null, null, null, null).getCount()).toEqual(15);

        resolver.update(Content.USERS_CLEANUP.uri, null, null, null);
        expect(resolver.query(Content.USERS.uri, null, null, null, null).getCount()).toEqual(14);
    }


    @Test
    public void shouldIncludeUserPermalinkInTrackView() throws Exception {
        Activities activities = Activities.fromJSON(
                SyncAdapterService.class.getResourceAsStream("incoming_1.json"));

        for (Track t : activities.getUniqueTracks()) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.TRACK.uri, t.buildContentValues())).not.toBeNull();
        }

        expect(Content.TRACK).toHaveCount(50);
        expect(Content.USERS).toHaveCount(32);

        Track t = SoundCloudDB.getTrackById(resolver, 18876167l); // jwagener/grand-piano-keys
        expect(t).not.toBeNull();
        expect(t.user.permalink).toEqual("jwagener");
        expect(t.permalink).toEqual("grand-piano-keys");
    }

    @Test
    public void shouldIncludeUsernameForPrivateRecordings() throws Exception {
        User user = new User();
        user.id = USER_ID;
        user.username = "current user";

        Recording r = new Recording(new File("/tmp/test"), user);
        r.user_id = USER_ID;
        expect(resolver.insert(Content.USERS.uri, user.buildContentValues())).not.toBeNull();
        Uri uri = resolver.insert(Content.RECORDINGS.uri, r.buildContentValues());

        expect(uri).not.toBeNull();

        Cursor c = resolver.query(Content.RECORDINGS.uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToFirst()).toBeTrue();

        expect(c.getString(c.getColumnIndex("username"))).toEqual("current user");
    }

    @Test
    public void shouldSupportAndroidGlobalSearch() throws Exception {
        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            resolver.insert(Content.TRACKS.uri, t.buildContentValues());
            resolver.insert(Content.USERS.uri, t.user.buildContentValues());
        }

        Cursor cursor = resolver.query(Content.ANDROID_SEARCH_SUGGEST.uri,
                null, null, new String[] { "plaid"}, null);

        expect(cursor.getCount()).toEqual(1);
        expect(cursor.moveToFirst()).toBeTrue();
        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)))
                .toEqual("Plaid - missing (taken from new album Scintilli)");

        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_2)))
                .toEqual("Warp Records");

        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID)))
                .toEqual(SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT);

        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_INTENT_DATA)))
                .toEqual("soundcloud:tracks:22365800");

        expect(cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1)))
                .toEqual(Content.TRACK_ARTWORK.forId(22365800L).toString());

        expect(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)))
                .toEqual(22365800L);
    }

    @Test
    public void shouldSuggestSoundsSortedByCreatedAt() throws Exception {

        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            resolver.insert(Content.TRACKS.uri, t.buildContentValues());
            resolver.insert(Content.USERS.uri, t.user.buildContentValues());
        }

        Cursor cursor = resolver.query(Content.ANDROID_SEARCH_SUGGEST.uri,
                null, null, new String[] { "H" }, null);

        expect(cursor.getCount()).toEqual(7);

        expect(cursor.moveToFirst()).toBeTrue();
        Track first = SoundCloudDB.getTrackById(resolver,
                cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
        expect(cursor.moveToLast()).toBeTrue();
        Track last = SoundCloudDB.getTrackById(resolver,
                cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));

        expect(first.created_at.after(last.created_at)).toBeTrue();
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
        Recording r = new Recording(new File("/tmp"));
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
        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_FAVORITES.uri, t.buildContentValues())).not.toBeNull();
        }
        Cursor c = resolver.query(Content.TRACK.withQuery(RANDOM, "0"), null, null, null, null);
        expect(c.getCount()).toEqual(15);
        List<Long> sorted = new ArrayList<Long>();
        List<Long> random = new ArrayList<Long>();
        while (c.moveToNext()) {
            sorted.add(c.getLong(c.getColumnIndex(DBHelper.TrackView._ID)));
        }
        Cursor c2 = resolver.query(Content.TRACK.withQuery(RANDOM, "1"), null, null, null, null);
        expect(c2.getCount()).toEqual(15);
        while (c2.moveToNext()) {
            random.add(c2.getLong(c2.getColumnIndex(DBHelper.TrackView._ID)));
        }
        expect(sorted).not.toEqual(random);
    }

    @Test
    public void shouldHaveATracksEndpointWhichReturnsOnlyCachedItems() throws Exception {
        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_FAVORITES.uri, t.buildContentValues())).not.toBeNull();
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
        expect(c.getLong(c.getColumnIndex(DBHelper.TrackView._ID))).toEqual(cachedId);
    }

    @Test
    public void shouldHaveFavoriteEndpointWhichOnlyReturnsCachedItems() throws Exception {
        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_FAVORITES.uri, t.buildContentValues())).not.toBeNull();
        }

        ContentValues cv = new ContentValues();
        final long cachedId = 27583938l;
        cv.put(DBHelper.TrackMetadata._ID, cachedId);
        cv.put(DBHelper.TrackMetadata.CACHED, 1);
        resolver.insert(Content.TRACK_METADATA.uri, cv);

        Uri uri = Content.ME_FAVORITES.withQuery(CACHED, "1");
        Cursor c = resolver.query(uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToNext()).toBeTrue();
        expect(c.getLong(c.getColumnIndex(DBHelper.TrackView._ID))).toEqual(cachedId);
    }

    @Test
    public void shouldHaveFavoriteEndpointWhichReturnsRandomItems() throws Exception {
        Track.TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                Track.TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_FAVORITES.uri, t.buildContentValues())).not.toBeNull();
        }

        Uri uri = Content.ME_FAVORITES.withQuery(RANDOM, "1", LIMIT, "5");
        Cursor c = resolver.query(uri, null, null, null, null);
        expect(c.getCount()).toEqual(5);

        long[] sorted = new long[] { 13403434, 18071729, 18213041, 18571159, 19658312 };
        long[] result = new long[sorted.length];
        int i = 0;
        while(c.moveToNext()) {
            result[i++] = c.getLong(c.getColumnIndex(DBHelper.TrackView._ID));
        }
        expect(Arrays.equals(result, sorted)).toBeFalse();
    }

    @Test
    public void shouldHaveStreamEndpointWhichReturnsRandomItems() throws Exception {
        // TODO: find easier way to populate stream
        ApiSyncService svc = new ApiSyncService();
        TestHelper.addCannedResponses(SyncAdapterServiceTest.class,  "incoming_2.json");
        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME_SOUND_STREAM.uri), 1);
        expect(Content.ME_SOUND_STREAM).toHaveCount(49);

        ContentValues cv = new ContentValues();
        final long firstId = 18508668l;
        cv.put(DBHelper.TrackMetadata._ID, firstId);
        cv.put(DBHelper.TrackMetadata.CACHED, 1);
        resolver.insert(Content.TRACK_METADATA.uri, cv);

        Uri uri = Content.ME_SOUND_STREAM.withQuery(RANDOM, "1", LIMIT, "5");
        Cursor c = resolver.query(uri, null, null, null, null);
        expect(c.getCount()).toEqual(5);
        long sorted[] = new long[] {18508668, 18508600, 18508493, 18028217, 18223729};
        long result[] = new long[sorted.length];
        int i=0;
        while (c.moveToNext()) {
            result[i++] = c.getLong(c.getColumnIndex(DBHelper.ActivityView.TRACK_ID));
        }
        expect(Arrays.equals(result, sorted)).toBeFalse();
    }

    @Test
    public void shouldHaveStreamEndpointWhichOnlyReturnsCachedItems() throws Exception {
        // TODO: find easier way to populate stream
        ApiSyncService svc = new ApiSyncService();
        TestHelper.addCannedResponses(SyncAdapterServiceTest.class,  "incoming_2.json");
        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME_SOUND_STREAM.uri), 1);
        expect(Content.ME_SOUND_STREAM).toHaveCount(49);

        ContentValues cv = new ContentValues();
        final long cachedId = 18508668l;
        cv.put(DBHelper.TrackMetadata._ID, cachedId);
        cv.put(DBHelper.TrackMetadata.CACHED, 1);
        resolver.insert(Content.TRACK_METADATA.uri, cv);

        Uri uri = Content.ME_SOUND_STREAM.withQuery(CACHED, "1");
        Cursor c = resolver.query(uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToNext()).toBeTrue();
        expect(c.getLong(c.getColumnIndex(DBHelper.ActivityView.TRACK_ID))).toEqual(cachedId);
    }


    @Test
    public void shouldEnableSyncing() throws Exception {
        RobolectricTestRunner.setStaticValue(Build.VERSION.class, "SDK_INT", 8);
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
        RobolectricTestRunner.setStaticValue(Build.VERSION.class, "SDK_INT", 8);
        Account account = new Account("name", "type");
        ScContentProvider.enableSyncing(account, 3600);
        List<PeriodicSync> syncs = ContentResolver.getPeriodicSyncs(account, ScContentProvider.AUTHORITY);
        expect(syncs.size()).toEqual(1);

        ScContentProvider.disableSyncing(account);

        syncs = ContentResolver.getPeriodicSyncs(account, ScContentProvider.AUTHORITY);
        expect(syncs.isEmpty()).toBeTrue();
        expect(ContentResolver.getSyncAutomatically(account, ScContentProvider.AUTHORITY)).toBeFalse();
    }
}
