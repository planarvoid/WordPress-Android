package com.soundcloud.android.provider;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.provider.ScContentProvider.Parameter.CACHED;
import static com.soundcloud.android.provider.ScContentProvider.Parameter.LIMIT;
import static com.soundcloud.android.provider.ScContentProvider.Parameter.RANDOM;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.User;
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

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
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
    public void shouldInsertQueryAndDeleteFavorites() throws Exception {
        TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                TrackHolder.class);
        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_LIKES.uri, t.buildContentValues())).not.toBeNull();
        }

        Cursor c = resolver.query(Content.ME_LIKES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(15);

        resolver.delete(Content.ME_LIKES.uri, DBHelper.CollectionItems.ITEM_ID + " = ?",
                new String[]{String.valueOf(tracks.get(0).id)});

        c = resolver.query(Content.ME_LIKES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(14);
    }

    @Test
    public void shouldCleanup() throws Exception {
        TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_LIKES.uri, t.buildContentValues())).not.toBeNull();
        }

        expect(resolver.query(Content.TRACKS.uri, null, null, null, null).getCount()).toEqual(15);
        expect(resolver.query(Content.USERS.uri, null, null, null, null).getCount()).toEqual(14);

        tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("tracks.json"),
                TrackHolder.class);

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
        Activities activities = SoundCloudApplication.MODEL_MANAGER.getActivitiesFromJson(
                ApiSyncServiceTest.class.getResourceAsStream("e1_stream_1.json"));

        for (Track t : activities.getUniqueTracks()) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.TRACK.uri, t.buildContentValues())).not.toBeNull();
        }

        expect(Content.TRACK).toHaveCount(20);
        expect(Content.USERS).toHaveCount(9);
        Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(61350393l);

        expect(t).not.toBeNull();
        expect(t.user.permalink).toEqual("westafricademocracyradio");
        expect(t.permalink).toEqual("info-chez-vous-2012-27-09");
    }


    @Test
    public void shouldSupportAndroidGlobalSearch() throws Exception {
        TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                TrackHolder.class);

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

        TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                TrackHolder.class);

        for (Track t : tracks) {
            resolver.insert(Content.TRACKS.uri, t.buildContentValues());
            resolver.insert(Content.USERS.uri, t.user.buildContentValues());
        }

        Cursor cursor = resolver.query(Content.ANDROID_SEARCH_SUGGEST.uri,
                null, null, new String[] { "H" }, null);

        expect(cursor.getCount()).toEqual(7);

        expect(cursor.moveToFirst()).toBeTrue();
        Track first = SoundCloudApplication.MODEL_MANAGER.getTrack(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
        expect(cursor.moveToLast()).toBeTrue();
        Track last = SoundCloudApplication.MODEL_MANAGER.getTrack(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));

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
        CollectionHolder<Track> tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_LIKES.uri, t.buildContentValues())).not.toBeNull();
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
        CollectionHolder<Track> tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_LIKES.uri, t.buildContentValues())).not.toBeNull();
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
        CollectionHolder<Track> tracks = SoundCloudApplication.MODEL_MANAGER.getCollectionFromStream(getClass().getResourceAsStream("user_favorites.json"));

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_LIKES.uri, t.buildContentValues())).not.toBeNull();
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
        expect(c.getLong(c.getColumnIndex(DBHelper.TrackView._ID))).toEqual(cachedId);
    }

    @Test
    public void shouldHaveFavoriteEndpointWhichReturnsRandomItems() throws Exception {
        TrackHolder tracks  = AndroidCloudAPI.Mapper.readValue(
                getClass().getResourceAsStream("user_favorites.json"),
                TrackHolder.class);

        for (Track t : tracks) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.ME_LIKES.uri, t.buildContentValues())).not.toBeNull();
        }

        Uri uri = Content.ME_LIKES.withQuery(RANDOM, "1", LIMIT, "5");
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
        TestHelper.addCannedResponses(ApiSyncServiceTest.class,  "e1_stream_2_oldest.json");
        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME_SOUND_STREAM.uri), 1);
        expect(Content.ME_SOUND_STREAM).toHaveCount(26);

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
            result[i++] = c.getLong(c.getColumnIndex(DBHelper.ActivityView.TRACK_ID));
        }
        expect(Arrays.equals(result, sorted)).toBeFalse();
    }

    @Test
    public void shouldHaveStreamEndpointWhichOnlyReturnsCachedItems() throws Exception {
        // TODO: find easier way to populate stream
        ApiSyncService svc = new ApiSyncService();
        TestHelper.addCannedResponses(ApiSyncServiceTest.class,  "e1_stream_2_oldest.json");
        svc.onStart(new Intent(Intent.ACTION_SYNC, Content.ME_SOUND_STREAM.uri), 1);
        expect(Content.ME_SOUND_STREAM).toHaveCount(26);

        ContentValues cv = new ContentValues();
        final long cachedId = 61467451l;
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
    public void shouldDeleteRecordings() throws Exception {
        Recording r = Recording.create();
        expect(SoundCloudDB.upsertRecording(resolver, r, null)).not.toBeNull();
        resolver.delete(Content.RECORDINGS.uri, null, null);
        Cursor cursor = resolver.query(Content.RECORDINGS.uri, null, null, null, null);
        expect(cursor.getCount()).toEqual(0);
    }
}
