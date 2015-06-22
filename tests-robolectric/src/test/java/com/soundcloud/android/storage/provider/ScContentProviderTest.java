package com.soundcloud.android.storage.provider;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.storage.provider.ScContentProvider.Parameter.CACHED;
import static com.soundcloud.android.testsupport.TestHelper.getActivities;
import static com.soundcloud.android.testsupport.TestHelper.readJson;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.Shortcut;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.DatabaseFixtures;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.accounts.Account;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.PeriodicSync;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class ScContentProviderTest {
    static final long USER_ID = 100L;
    ContentResolver resolver;
    ActivitiesStorage activitiesStorage;
    private DatabaseFixtures testFixtures;
    private SQLiteDatabase writableDatabase;

    @Before
    public void before() {
        TestHelper.setUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        activitiesStorage = new ActivitiesStorage(Robolectric.application);
        writableDatabase = DatabaseManager.getInstance(Robolectric.application).getWritableDatabase();
        testFixtures = new DatabaseFixtures(writableDatabase);
    }

    @Test
    public void shouldIncludeUserPermalinkInTrackView() throws Exception {
        Activities activities = getActivities("/com/soundcloud/android/sync/e1_stream_1.json");

        for (Playable t : activities.getUniquePlayables()) {
            expect(resolver.insert(Content.USERS.uri, t.user.buildContentValues())).not.toBeNull();
            expect(resolver.insert(Content.TRACK.uri, t.buildContentValues())).not.toBeNull();
        }

        expect(Content.TRACK).toHaveCount(20);
        expect(Content.USERS).toHaveCount(11);
        PublicApiTrack t = SoundCloudApplication.sModelManager.getTrack(61350393l);

        expect(t).not.toBeNull();
        expect(t.user.permalink).toEqual("westafricademocracyradio");
        expect(t.permalink).toEqual("info-chez-vous-2012-27-09");
    }


    @Test
    public void shouldSupportAndroidGlobalSearch() throws Exception {
        Shortcut[] shortcuts = readJson(Shortcut[].class, "/com/soundcloud/android/sync/all_shortcuts.json");

        List<ContentValues> cvs = new ArrayList<>();
        for (Shortcut shortcut : shortcuts) {
            ContentValues cv = shortcut.buildContentValues();
            if (cv != null) {
                cvs.add(cv);
            }
        }
        int inserted = resolver.bulkInsert(Content.ME_SHORTCUTS.uri, cvs.toArray(new ContentValues[cvs.size()]));
        expect(inserted).toEqual(cvs.size());
        expect(Content.ME_SHORTCUTS).toHaveCount(inserted);


        Cursor cursor = resolver.query(Content.ANDROID_SEARCH_SUGGEST.uri,
                null, null, new String[]{"blac"}, null);

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
    public void shouldInsertTrackMetadata() throws Exception {
        ContentValues values = new ContentValues();
        values.put(TableColumns.TrackMetadata._ID, 20);
        values.put(TableColumns.TrackMetadata.ETAG, "123456");
        values.put(TableColumns.TrackMetadata.CACHED, 1);

        Uri result = resolver.insert(Content.TRACK_METADATA.uri, values);
        expect(result).toEqual("content://com.soundcloud.android.provider.ScContentProvider/track_metadata/20");
    }

    @Test
    public void shouldHaveFavoriteEndpointWhichOnlyReturnsCachedItems() throws Exception {
        ApiTrack track = testFixtures.insertLikedTrack(new Date());

        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackMetadata._ID, track.getUrn().getNumericId());
        cv.put(TableColumns.TrackMetadata.CACHED, 1);
        resolver.insert(Content.TRACK_METADATA.uri, cv);

        Uri uri = Content.ME_LIKES.withQuery(CACHED, "1");
        Cursor c = resolver.query(uri, null, null, null, null);
        expect(c.getCount()).toEqual(1);
        expect(c.moveToNext()).toBeTrue();
        expect(c.getLong(c.getColumnIndex(TableColumns.SoundView._ID))).toEqual(track.getUrn().getNumericId());
    }

    @Test
    public void shouldQueryLikesWithDescendingOrder() throws Exception {
        ApiTrack track1 = testFixtures.insertLikedTrack(new Date(1000));
        ApiTrack track2 = testFixtures.insertLikedTrack(new Date(2000));

        Cursor c = resolver.query(Content.ME_LIKES.uri, null, null, null, null);
        expect(c.getCount()).toEqual(2);

        long[] result = new long[2];
        int i = 0;
        while (c.moveToNext()) {
            result[i++] = c.getLong(c.getColumnIndex(TableColumns.SoundView._ID));
        }
        expect(result[0]).toEqual(track2.getId());
        expect(result[1]).toEqual(track1.getId());
    }

    @Test
    public void shouldEnableSyncing() throws Exception {
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
        Shortcut[] shortcuts = readJson(Shortcut[].class, "/com/soundcloud/android/sync/all_shortcuts.json");

        List<ContentValues> cvs = new ArrayList<>();
        for (Shortcut shortcut : shortcuts) {
            ContentValues cv = shortcut.buildContentValues();
            if (cv != null) {
                cvs.add(cv);
            }
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

        PublicApiUser u = TestHelper.loadLocalContentItem(Content.USERS.uri, PublicApiUser.class, "_id = 9");

        expect(u).not.toBeNull();
        expect(u.username).toEqual("Katharina");
        expect(u.avatar_url).toEqual("https://i1.sndcdn.com/avatars-000013690441-hohfv1-tiny.jpg?2479809");
        expect(u.permalink_url).toEqual("http://soundcloud.com/katharina");

        PublicApiTrack t = TestHelper.loadLocalContent(new PublicApiTrack(64629168).toUri(), PublicApiTrack.class).get(0);
        expect(t).not.toBeNull();
        expect(t.title).toEqual("Halls - Roses For The Dead (Max Cooper remix)");
        expect(t.artwork_url).toEqual("https://i1.sndcdn.com/artworks-000032795722-aaqx24-tiny.jpg?2479809");
        expect(t.permalink_url).toEqual("http://soundcloud.com/no-pain-in-pop/halls-roses-for-the-dead-max");
    }

    @Test
    public void shouldStoreAndFetchShortcut() throws Exception {
        Shortcut c = new Shortcut();
        c.kind = "like";
        c.setId(12);
        c.title = "Something";
        c.permalink_url = "http://soundcloud.com/foo";
        c.artwork_url = "http://soundcloud.com/foo/artwork";


        Uri uri = resolver.insert(Content.ME_SHORTCUTS.uri, c.buildContentValues());
        expect(uri).not.toBeNull();

        Cursor cursor = resolver.query(uri, null, null, null, null);
        expect(cursor.getCount()).toEqual(1);
        expect(cursor.moveToFirst()).toBeTrue();

        expect(cursor.getString(cursor.getColumnIndex(TableColumns.Suggestions.ICON_URL))).toEqual("http://soundcloud.com/foo/artwork");
    }

    @Test
    public void shouldSupportLimitParameter() {
        TestHelper.insertWithDependencies(Content.TRACKS.uri, new PublicApiTrack(1));
        TestHelper.insertWithDependencies(Content.TRACKS.uri, new PublicApiTrack(2));

        expect(Content.TRACKS).toHaveCount(2);

        Uri limitedUri = Content.TRACKS.uri.buildUpon()
                .appendQueryParameter("limit", "1").build();
        Cursor cursor = resolver.query(limitedUri, null, null, null, null);
        expect(cursor).toHaveCount(1);
        cursor.moveToFirst();
        expect(cursor).toHaveColumn(BaseColumns._ID, 1L);
    }

    @Test
    public void shouldSupportOffsetParameter() {
        TestHelper.insertWithDependencies(Content.TRACKS.uri, new PublicApiTrack(1));
        TestHelper.insertWithDependencies(Content.TRACKS.uri, new PublicApiTrack(2));
        TestHelper.insertWithDependencies(Content.TRACKS.uri, new PublicApiTrack(3));

        expect(Content.TRACKS).toHaveCount(3);

        Uri limitedUri = Content.TRACKS.uri.buildUpon()
                .appendQueryParameter("limit", "1")
                .appendQueryParameter("offset", "2").build();
        Cursor cursor = resolver.query(limitedUri, null, null, null, null);
        expect(cursor).toHaveCount(1);
        cursor.moveToFirst();
        expect(cursor).toHaveColumn(BaseColumns._ID, 3L);
    }

}
