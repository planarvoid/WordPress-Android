package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.List;

@RunWith(DefaultTestRunner.class)
public class SyncContentTest {
    ContentResolver resolver;

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();
    }

    @Test
    public void shouldSyncAll() throws Exception {
        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(3);
    }

    @Test
    public void shouldSyncAllExceptMySounds() throws Exception {
        LocalCollection c = LocalCollection.insertLocalCollection(
                SyncContent.MySounds.content.uri, // uri
                1, // sync state
                System.currentTimeMillis(), // last sync
                2, // size
                "some-extra", // extra
                resolver);

        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(2);
    }

    @Test
    public void shouldSyncAllExceptMySounds1Miss() throws Exception {
        LocalCollection c = LocalCollection.insertLocalCollection(
                SyncContent.MySounds.content.uri, // uri
                1, // sync state
                System.currentTimeMillis() - SyncConfig.TRACK_BACKOFF_MULTIPLIERS[1] * SyncConfig.TRACK_STALE_TIME + 5000, // last sync
                2, // size
                "1", // extra
                resolver);

        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(2);
    }

    @Test
    public void shouldSyncAllMySounds1Miss() throws Exception {
        LocalCollection c = LocalCollection.insertLocalCollection(
                SyncContent.MySounds.content.uri, // uri
                1, // sync state
                System.currentTimeMillis() - SyncConfig.TRACK_BACKOFF_MULTIPLIERS[1] * SyncConfig.TRACK_STALE_TIME, // last sync
                2, // size
                "1", // extra
                resolver);

        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(3);
    }

    @Test
    public void shouldSyncAllExceptMySoundsMaxMisses() throws Exception {
        LocalCollection c = LocalCollection.insertLocalCollection(
                SyncContent.MySounds.content.uri, // uri
                1, // sync state
                1, // last sync
                2, // size
                String.valueOf(SyncConfig.TRACK_BACKOFF_MULTIPLIERS.length), // extra
                resolver);

        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(2);
    }

    @Test
    public void shouldNotSyncAfterMiss() throws Exception {
        LocalCollection c = LocalCollection.insertLocalCollection(
                SyncContent.MySounds.content.uri,// uri
                1, // sync state
                System.currentTimeMillis() - SyncConfig.TRACK_STALE_TIME, // last sync
                2, // size
                "", // extra
                resolver);

        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(3);

        android.os.Bundle syncResult = new android.os.Bundle();
        syncResult.putBoolean(SyncContent.MySounds.content.uri.toString(),false);
        SyncContent.updateCollections(Robolectric.application, syncResult);

        urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(2);
    }
}
