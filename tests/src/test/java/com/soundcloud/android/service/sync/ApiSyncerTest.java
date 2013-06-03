package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.addPendingHttpResponse;
import static com.soundcloud.android.robolectric.TestHelper.assertResolverNotified;

import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.dao.PlaylistStorage;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.model.act.PlaylistActivity;
import com.soundcloud.android.model.act.TrackActivity;
import com.soundcloud.android.model.act.TrackSharingActivity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class ApiSyncerTest {
    private static final long USER_ID = 133201L;
    private static final int TOTAL_STREAM_SIZE = 119; // 120 - 1 dup

    ContentResolver resolver;
    SyncStateManager syncStateManager;
    ActivitiesStorage activitiesStorage;
    PlaylistStorage playlistStorage;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
        syncStateManager = new SyncStateManager();
        activitiesStorage = new ActivitiesStorage();
        playlistStorage = new PlaylistStorage();
    }

    @Test
    public void shouldSyncMe() throws Exception {
        addPendingHttpResponse(getClass(), "me.json");
        expect(Content.ME).toBeEmpty();
        ApiSyncResult result = sync(Content.ME.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        expect(Content.ME).toHaveCount(1);
        expect(Content.USERS).toHaveCount(1);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
    }

    @Test
    public void shouldSyncStream() throws Exception {
        ApiSyncResult result = sync(Content.ME_SOUND_STREAM.uri,
                "e1_stream.json",
                "e1_stream_oldest.json");
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        expect(Content.ME_SOUND_STREAM).toHaveCount(TOTAL_STREAM_SIZE);
        expect(Content.TRACKS).toHaveCount(111);
        expect(Content.USERS).toHaveCount(28);
        expect(Content.PLAYLISTS).toHaveCount(8);

        Activities incoming = activitiesStorage.getCollectionSince(Content.ME_SOUND_STREAM.uri, -1)
                .toBlockingObservable().lastOrDefault(Activities.EMPTY);

        expect(incoming.size()).toEqual(TOTAL_STREAM_SIZE);
        expect(incoming.getUniquePlayables().size()).toEqual(TOTAL_STREAM_SIZE);
        assertResolverNotified(Content.ME_SOUND_STREAM.uri, Content.TRACKS.uri, Content.USERS.uri);
    }
    @Test
    public void shouldSyncStreamWithTrackWithoutStats() throws Exception {
        // special case: track in stream doesn't contain some of the stats (per track basis):
        // playback_count, download_count, favoritings_count, comment_count, likes_count, reposts_count
        // we need to make sure we preserve this information and not write 0 to the local storage
        ApiSyncResult result = sync(Content.ME_SOUND_STREAM.uri,
                "e1_stream_track_without_stats.json",
                "e1_stream_oldest.json");
        expect(result.success).toBeTrue();
        expect(Content.TRACKS).toHaveCount(1);

        Cursor c = resolver.query(Content.TRACK.forId(61467451), null, null, null, null);
        expect(c).not.toBeNull();
        expect(c.moveToNext()).toBeTrue();
        Track t = new Track(c);
        expect(t.likes_count   ).toEqual(ScModel.NOT_SET);
        expect(t.download_count).toEqual(ScModel.NOT_SET);
        expect(t.reposts_count ).toEqual(ScModel.NOT_SET);
        expect(t.comment_count ).toEqual(ScModel.NOT_SET);
        expect(t.playback_count).toEqual(ScModel.NOT_SET);
    }

    @Test
    public void shouldSyncActivities() throws Exception {
        ApiSyncResult result = sync(Content.ME_ACTIVITIES.uri,
                "e1_activities_1.json",
                "e1_activities_2.json");
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);


        expect(Content.ME_ACTIVITIES).toHaveCount(17);
        expect(Content.COMMENTS).toHaveCount(5);

        Activities own = activitiesStorage.getCollectionSince(Content.ME_ACTIVITIES.uri, -1)
                .toBlockingObservable().lastOrDefault(Activities.EMPTY);
        expect(own.size()).toEqual(17);

        assertResolverNotified(Content.TRACKS.uri,
                Content.USERS.uri,
                Content.COMMENTS.uri,
                Content.ME_ACTIVITIES.uri);
    }

    @Test
    public void shouldSyncSounds() throws Exception {
        ApiSyncResult result = syncMeSounds();
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        expect(Content.TRACKS).toHaveCount(48);
        expect(Content.ME_SOUNDS).toHaveCount(50);
    }

    @Test
    public void shouldSyncLikes() throws Exception {
        TestHelper.addResourceResponse(getClass(), "/e1/users/" + String.valueOf(USER_ID)
                + "/likes?limit=200&representation=mini&linked_partitioning=1", "e1_likes_mini.json");

        ApiSyncResult result = sync(Content.ME_LIKES.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        expect(Content.TRACKS).toHaveCount(1);
        expect(Content.PLAYLISTS).toHaveCount(1);
        expect(Content.ME_LIKES).toHaveCount(2);
    }

    @Test
    public void shouldSyncPlaylists() throws Exception {
        TestHelper.addResourceResponse(getClass(), "/me/playlists?representation=compact&limit=200&linked_partitioning=1", "me_playlists_compact.json");
        TestHelper.addResourceResponse(getClass(), "/playlists/3250812/tracks", "playlist_3250812_tracks.json");
        TestHelper.addResourceResponse(getClass(), "/playlists/3250804/tracks", "playlist_3250804_tracks.json");

        ApiSyncResult result = sync(Content.ME_PLAYLISTS.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        expect(Content.TRACKS).toHaveCount(7);
        expect(Content.PLAYLISTS).toHaveCount(3);
        expect(Content.ME_PLAYLISTS).toHaveCount(3);

        expect(Content.PLAYLIST_TRACKS.forQuery("3250812")).toHaveCount(4);
        expect(Content.PLAYLIST_TRACKS.forQuery("3250804")).toHaveCount(4);
        expect(Content.PLAYLIST_TRACKS.forQuery("4042968")).toHaveCount(0); // this one is too big for the sync (101 sounds)
    }

    @Test
    public void shouldSyncSoundsAndLikes() throws Exception {
        ApiSyncResult result = syncMeSounds();
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        TestHelper.addResourceResponse(getClass(), "/e1/users/" + String.valueOf(USER_ID)
                + "/likes?limit=200&representation=mini&linked_partitioning=1", "e1_likes_mini.json");

        result = sync(Content.ME_LIKES.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        expect(Content.TRACKS).toHaveCount(49); // 48 tracks + from /me/sounds + 1 track from /me/likes
        expect(Content.ME_SOUNDS).toHaveCount(50); // 48 tracks + 2 playlists from /me/sounds
        expect(Content.ME_LIKES).toHaveCount(2); // 1 track + 1 playlist like
    }

    @Test
    public void shouldSyncMyShortcuts() throws Exception {

        TestHelper.addPendingHttpResponse(getClass(), "all_shortcuts.json");
        sync(Content.ME_SHORTCUTS.uri);
        expect(Content.ME_SHORTCUTS).toHaveCount(461);

        // make sure tracks+users got written
        expect(Content.USERS).toHaveCount(318);
        expect(Content.TRACKS).toHaveCount(143);
    }

    @Test
    public void shouldSyncConnections() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "connections.json");
        expect(sync(Content.ME_CONNECTIONS.uri).change).toEqual(ApiSyncResult.CHANGED);
        expect(Content.ME_CONNECTIONS).toHaveCount(4);

        TestHelper.addPendingHttpResponse(getClass(), "connections.json");
        expect(sync(Content.ME_CONNECTIONS.uri).change).toEqual(ApiSyncResult.UNCHANGED);
        expect(Content.ME_CONNECTIONS).toHaveCount(4);

        TestHelper.addPendingHttpResponse(getClass(), "connections_add.json");
        expect(sync(Content.ME_CONNECTIONS.uri).change).toEqual(ApiSyncResult.CHANGED);
        expect(Content.ME_CONNECTIONS).toHaveCount(6);

        TestHelper.addPendingHttpResponse(getClass(), "connections_delete.json");
        expect(sync(Content.ME_CONNECTIONS.uri).change).toEqual(ApiSyncResult.CHANGED);
        expect(Content.ME_CONNECTIONS).toHaveCount(3);
    }

    @Test
    public void shouldPushNewPlaylist() throws Exception {
        syncMeSounds();

        Playlist playlist = TestHelper.readResource("/com/soundcloud/android/service/sync/playlist.json");
        TestHelper.addPendingHttpResponse(getClass(), "playlist.json");

        Playlist p = TestHelper.createNewUserPlaylist(playlist.user, false, playlist.tracks);
        TestHelper.insertAsSoundAssociation(p, SoundAssociation.Type.PLAYLIST);

        expect(Content.ME_SOUNDS).toHaveCount(51);
        expect(Content.COLLECTIONS).toHaveCount(0);
        expect(new ApiSyncer(Robolectric.application, resolver).pushLocalPlaylists()).toBe(1);
        expect(Content.ME_SOUNDS).toHaveCount(51);
        expect(Content.COLLECTIONS).toHaveCount(1);

        expect(syncStateManager.fromContent(playlist.toUri()).shouldAutoRefresh()).toBeFalse();
    }

    private ApiSyncResult syncMeSounds() throws IOException {
        TestHelper.addResourceResponse(getClass(), "/e1/me/sounds/mini?limit=200&representation=mini&linked_partitioning=1", "me_sounds_mini.json");
        return sync(Content.ME_SOUNDS.uri);
    }

    @Test
    public void shouldSyncAPlaylist() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "playlist.json");
        ApiSyncResult result = sync(Content.PLAYLIST.forId(2524386l));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(Content.PLAYLISTS).toHaveCount(1);

        Playlist p = playlistStorage.loadPlaylistWithTracks(2524386l).toBlockingObservable().lastOrDefault(null);


        expect(p.title).toEqual("fall into fall");
        expect(p.getTrackCount()).toEqual(41);
        expect(p.tracks).not.toBeNull();

        final Track track = p.tracks.get(10);
        expect(track.title).toEqual("Mozart Parties - Where Has Everybody Gone (Regal Safari Remix)");
        expect(track.user).not.toBeNull();
        expect(track.user.username).toEqual("Regal Safari");
    }

    @Test
    public void shouldSyncPlaylistWithAdditions() throws Exception {

        TestHelper.addPendingHttpResponse(getClass(), "tracks.json");
        ApiSyncResult result = sync(Content.TRACK_LOOKUP.forQuery("10853436,10696200,10602324"));
        expect(result.success).toBe(true);

        final Playlist playlist = new Playlist(2524386);

        expect(playlistStorage.addTrackToPlaylist(playlist, 10696200, System.currentTimeMillis())).not.toBeNull();
        expect(playlistStorage.addTrackToPlaylist(playlist, 10853436, System.currentTimeMillis() + 100)).not.toBeNull();

        TestHelper.addPendingHttpResponse(getClass(), "playlist.json", "playlist_added.json");

        result = sync(Content.PLAYLIST.forId(10696200));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(0L);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(Content.TRACKS).toHaveCount(44);

        Playlist p = playlistStorage.loadPlaylistWithTracks(playlist.id).toBlockingObservable().lastOrDefault(null);
        expect(p.tracks.size()).toBe(43);
        expect(p.tracks.get(1).title).toEqual("recording on thursday afternoon");
    }

    @Test
    public void shouldDoTrackLookup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "tracks.json");
        ApiSyncResult result = sync(Content.TRACK_LOOKUP.forQuery("10853436,10696200,10602324"));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(Content.TRACKS).toHaveCount(3);
    }

    @Test
    public void shouldDoUserLookup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "users.json");
        ApiSyncResult result = sync(Content.USER_LOOKUP.forQuery("308291,792584,1255758"));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(Content.USERS).toHaveCount(3);
    }

    @Test
    public void shouldDoPlaylistLookup() throws Exception {
        TestHelper.addCannedResponse(getClass(), "/playlists?ids=3761799%2C1&representation=compact&linked_partitioning=1",
                "playlists_compact.json");
        ApiSyncResult result = sync(Content.PLAYLIST_LOOKUP.forQuery("3761799,1"));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(Content.PLAYLISTS).toHaveCount(2);
    }

    @Test
    public void shouldSetSyncResultData() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "e1_activities_1_oldest.json");
        ApiSyncResult result = sync(Content.ME_ACTIVITIES.uri);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(result.new_size).toEqual(7);
        expect(result.synced_at).toBeGreaterThan(0l);
    }

    @Test
    public void shouldSyncDifferentEndpoints() throws Exception {
        sync(Content.ME_ACTIVITIES.uri,
                "e1_activities_1.json",
                "e1_activities_2.json");

        sync(Content.ME_SOUND_STREAM.uri,
                "e1_stream.json",
                "e1_stream_oldest.json");

        expect(Content.ME_SOUND_STREAM).toHaveCount(TOTAL_STREAM_SIZE);
        expect(Content.ME_ACTIVITIES).toHaveCount(17);
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(136);
    }

    @Test
    public void shouldNotProduceDuplicatesWhenSyncing() throws Exception {
        sync(Content.ME_SOUND_STREAM.uri, "e1_stream_1_oldest.json");

        expect(Content.ME_SOUND_STREAM).toHaveCount(22);
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(22);
    }

    @Test
    public void shouldFilterOutDuplicateTrackAndSharingsAndKeepSharings() throws Exception {
        sync(Content.ME_SOUND_STREAM.uri, "track_and_track_sharing.json");

        expect(Content.ME_SOUND_STREAM).toHaveCount(2);
        Activities incoming = activitiesStorage.getCollectionSince(Content.ME_SOUND_STREAM.uri, -1)
                .toBlockingObservable().lastOrDefault(Activities.EMPTY);

        expect(incoming.size()).toEqual(2);
        Activity a1 = incoming.get(0);
        Activity a2 = incoming.get(1);

        expect(a1).toBeInstanceOf(TrackActivity.class);
        expect(a1.getPlayable().permalink).toEqual("bastard-amo1-edit");
        expect(a2).toBeInstanceOf(TrackSharingActivity.class);
        expect(a2.getPlayable().permalink).toEqual("leotrax06-leo-zero-boom-bam");
    }

    @Test
    public void shouldFilterOutDuplicateTrackAndSharingsAndKeepSharingsWithRealData() throws Exception {
        sync(Content.ME_SOUND_STREAM.uri, "stream_with_duplicates.json");
        // 2 track dups: take-you-home-ruff-cut-preview, b-b-fuller-7-minutes-preview
        // 1 set dup: repost-your-favorite
        expect(Content.ME_SOUND_STREAM).toHaveCount(47);
    }

    @Test
    public void shouldFilterOutDuplicatePlaylistAndSharingsAndKeepSharings() throws Exception {
        sync(Content.ME_SOUND_STREAM.uri, "playlist_and_playlist_sharing.json");

        expect(Content.ME_SOUND_STREAM).toHaveCount(1);
        Activities incoming = activitiesStorage.getCollectionSince(Content.ME_SOUND_STREAM.uri, -1)
                .toBlockingObservable().lastOrDefault(Activities.EMPTY);

        expect(incoming.size()).toEqual(1);
        Activity a1 = incoming.get(0);

        expect(a1).toBeInstanceOf(PlaylistActivity.class);
        expect(a1.getPlayable().permalink).toEqual("private-share-test");
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOException() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "error");
        sync(Content.ME_LIKES.uri);
    }

    private ApiSyncResult sync(Uri uri, String... fixtures) throws IOException {
        addPendingHttpResponse(getClass(), fixtures);
        ApiSyncer syncer = new ApiSyncer(Robolectric.application, Robolectric.application.getContentResolver());
        return syncer.syncContent(uri, Intent.ACTION_SYNC);
    }
}
