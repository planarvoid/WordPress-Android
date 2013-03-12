package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.dao.LocalCollectionDAO;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.List;
import java.util.Set;

@RunWith(DefaultTestRunner.class)
public class SyncContentTest {
    ContentResolver resolver;

    private static final int ACTIVE_SYNC_ENDPOINTS = SyncContent.values().length - 1; /* follower disabled */

    @Before
    public void before() {
        resolver = Robolectric.application.getContentResolver();
        SyncContent.setAllSyncEnabledPrefs(Robolectric.application,true);
    }

    @Test
    public void shouldSyncAll() throws Exception {
        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS);
    }

    @Test
    public void shouldSyncAllExceptMySounds() throws Exception {
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(
                SyncContent.MySounds.content.uri, // uri
                1, // sync state
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis(), // last sync
                2, // size
                "some-extra", // extra
                resolver);

        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS - 1);
    }

    @Test
    public void shouldSyncAllExceptMySounds1Miss() throws Exception {
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(
                SyncContent.MySounds.content.uri, // uri
                1, // sync state
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.TRACK_BACKOFF_MULTIPLIERS[1] * SyncConfig.TRACK_STALE_TIME + 5000, // last sync
                2, // size
                "1", // extra
                resolver);

        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS -1);
    }

    @Test
    public void shouldSyncAllMySounds1Miss() throws Exception {
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(
                SyncContent.MySounds.content.uri, // uri
                1, // sync state
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.TRACK_BACKOFF_MULTIPLIERS[1] * SyncConfig.TRACK_STALE_TIME, // last sync
                2, // size
                "1", // extra
                resolver);

        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS );
    }

    @Test
    public void shouldSyncAllExceptMySoundsMaxMisses() throws Exception {
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(
                SyncContent.MySounds.content.uri, // uri
                1, // sync state
                -1l, // last sync attempt, ignored in the sync adapter
                1, // last sync
                2, // size
                String.valueOf(SyncConfig.TRACK_BACKOFF_MULTIPLIERS.length), // extra
                resolver);

        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS -1);
        expect(urisToSync).not.toContain(SyncContent.MySounds.content.uri);
    }

    @Test
    public void shouldNotSyncAfterMiss() throws Exception {
        LocalCollection c = LocalCollectionDAO.insertLocalCollection(
                SyncContent.MySounds.content.uri,// uri
                1, // sync state
                -1l, // last sync attempt, ignored in the sync adapter
                System.currentTimeMillis() - SyncConfig.TRACK_STALE_TIME, // last sync
                2, // size
                "", // extra
                resolver);

        List<Uri> urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS);

        android.os.Bundle syncResult = new android.os.Bundle();
        syncResult.putBoolean(SyncContent.MySounds.content.uri.toString(),false);
        SyncContent.updateCollections(Robolectric.application, syncResult);

        urisToSync = SyncContent.getCollectionsDueForSync(Robolectric.application, false);
        expect(urisToSync.size()).toEqual(ACTIVE_SYNC_ENDPOINTS-1);
    }

    @Test
    public void shouldSyncChangesToExistingPlaylists() throws Exception {
        final Playlist playlist = new Playlist(12345);
        expect(Playlist.addTrackToPlaylist(resolver, playlist, 10696200, System.currentTimeMillis())).not.toBeNull();
        // local unpushed playlists (those with a negative timestamp) are not part of this sync step
        expect(Playlist.addTrackToPlaylist(resolver, new Playlist(-34243), 10696200, System.currentTimeMillis())).not.toBeNull();

        Set<Uri> urisToSync = SyncContent.getPlaylistsDueForSync(Robolectric.application.getContentResolver());
        expect(urisToSync.size()).toEqual(1);
        expect(urisToSync.contains(Content.PLAYLIST.forId(playlist.id))).toBeTrue();
    }

    @Test
    public void shouldSyncMultiplePlaylists() throws Exception {
        final Playlist playlist1 = new Playlist(12345);
        final Playlist playlist2 = new Playlist(544321);

        expect(Playlist.addTrackToPlaylist(resolver, playlist1, 10696200, System.currentTimeMillis())).not.toBeNull();
        expect(Playlist.addTrackToPlaylist(resolver, playlist2, 10696200, System.currentTimeMillis())).not.toBeNull();

        Set<Uri> urisToSync = SyncContent.getPlaylistsDueForSync(Robolectric.application.getContentResolver());
        expect(urisToSync.size()).toEqual(2);
        expect(urisToSync.contains(Content.PLAYLIST.forId(playlist1.id))).toBeTrue();
        expect(urisToSync.contains(Content.PLAYLIST.forId(playlist2.id))).toBeTrue();


    }
}
