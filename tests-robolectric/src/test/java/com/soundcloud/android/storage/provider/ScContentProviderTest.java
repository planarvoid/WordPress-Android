package com.soundcloud.android.storage.provider;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.storage.provider.ScContentProvider.Parameter.CACHED;
import static com.soundcloud.android.testsupport.TestHelper.getActivities;
import static com.soundcloud.android.testsupport.TestHelper.readJson;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
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
