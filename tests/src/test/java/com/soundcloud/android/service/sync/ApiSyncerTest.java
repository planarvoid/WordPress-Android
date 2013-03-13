package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.*;
import static com.soundcloud.android.service.sync.ApiSyncer.Result;

import com.soundcloud.android.dao.ActivitiesDAO;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.LocalCollectionDAO;
import com.soundcloud.android.dao.PlaylistDAO;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManagerTest;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
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

    private ContentResolver resolver;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
    }

    @Test
    public void shouldSyncMe() throws Exception {
        addPendingHttpResponse(getClass(), "me.json");
        expect(Content.ME).toBeEmpty();
        Result result = sync(Content.ME.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        expect(Content.ME).toHaveCount(1);
        expect(Content.USERS).toHaveCount(1);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(Result.CHANGED);
    }

    @Test
    public void shouldSyncStream() throws Exception {
        Result result = sync(Content.ME_SOUND_STREAM.uri,
                "e1_stream.json",
                "e1_stream_oldest.json");
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        expect(Content.ME_SOUND_STREAM).toHaveCount(TOTAL_STREAM_SIZE);
        expect(Content.TRACKS).toHaveCount(111);
        expect(Content.USERS).toHaveCount(28);
        expect(Content.PLAYLISTS).toHaveCount(8);

        Activities incoming = ActivitiesDAO.getSince(Content.ME_SOUND_STREAM, resolver, -1);

        expect(incoming.size()).toEqual(TOTAL_STREAM_SIZE);
        expect(incoming.getUniquePlayables().size()).toEqual(TOTAL_STREAM_SIZE);
        assertResolverNotified(Content.ME_SOUND_STREAM.uri, Content.TRACKS.uri, Content.USERS.uri);
    }
    @Test
    public void shouldSyncStreamWithTrackWithoutStats() throws Exception {
        // special case: track in stream doesn't contain some of the stats (per track basis):
        // playback_count, download_count, favoritings_count, comment_count, likes_count, reposts_count
        // we need to make sure we preserve this information and not write 0 to the local storage
        Result result = sync(Content.ME_SOUND_STREAM.uri,
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
        Result result = sync(Content.ME_ACTIVITIES.uri,
                "e1_activities_1.json",
                "e1_activities_2.json");
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);


        expect(Content.ME_ACTIVITIES).toHaveCount(17);
        expect(Content.COMMENTS).toHaveCount(5);

        Activities own = ActivitiesDAO.getSince(Content.ME_ACTIVITIES, Robolectric.application.getContentResolver(), -1);
        expect(own.size()).toEqual(17);

        assertResolverNotified(Content.TRACKS.uri,
                Content.USERS.uri,
                Content.COMMENTS.uri,
                Content.ME_ACTIVITIES.uri);
    }

    @Test
    public void shouldSyncFollowers() throws Exception {
        addIdResponse("/me/followers/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addCannedResponse(getClass(), "/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE, "users.json");

        Result result = sync(Content.ME_FOLLOWERS.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        // make sure tracks+users got written
        expect(Content.USERS).toHaveCount(3);
        expect(Content.ME_FOLLOWERS).toHaveCount(3);
        assertFirstIdToBe(Content.ME_FOLLOWERS, 308291);
    }

    @Test
    public void shouldSyncSounds() throws Exception {
        Result result = populateMeSounds();
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        expect(Content.TRACKS).toHaveCount(48);
        expect(Content.ME_SOUNDS).toHaveCount(50);
    }

    @Test
    public void shouldSyncLikes() throws Exception {
        addResourceResponse("/e1/users/" + String.valueOf(USER_ID)
                + "/likes?limit=200&representation=mini&linked_partitioning=1", "e1_likes_mini.json");

        Result result = sync(Content.ME_LIKES.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        expect(Content.TRACKS).toHaveCount(1);
        expect(Content.PLAYLISTS).toHaveCount(1);
        expect(Content.ME_LIKES).toHaveCount(2);
    }

    @Test
    public void shouldSyncPlaylists() throws Exception {
        addResourceResponse("/me/playlists?representation=compact&limit=200&linked_partitioning=1", "me_playlists_compact.json");
        addResourceResponse("/playlists/3250812/tracks", "playlist_3250812_tracks.json");
        addResourceResponse("/playlists/3250804/tracks", "playlist_3250804_tracks.json");

        Result result = sync(Content.ME_PLAYLISTS.uri);
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
        Result result = populateMeSounds();
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        addResourceResponse("/e1/users/" + String.valueOf(USER_ID)
                + "/likes?limit=200&representation=mini&linked_partitioning=1", "e1_likes_mini.json");

        result = sync(Content.ME_LIKES.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(0l);

        expect(Content.TRACKS).toHaveCount(49); // 48 tracks + from /me/sounds + 1 track from /me/likes
        expect(Content.ME_SOUNDS).toHaveCount(50); // 48 tracks + 2 playlists from /me/sounds
        expect(Content.ME_LIKES).toHaveCount(2); // 1 track + 1 playlist like
    }

    @Test
    public void shouldSyncFriends() throws Exception {
        addIdResponse("/me/connections/friends/ids?linked_partitioning=1", 792584, 1255758, 308291);
        addResourceResponse("/users?linked_partitioning=1&limit=200&ids=792584%2C1255758%2C308291", "users.json");

        sync(Content.ME_FRIENDS.uri);

        // make sure tracks+users got written
        expect(Content.USERS).toHaveCount(3);
        expect(Content.ME_FRIENDS).toHaveCount(3);
        assertFirstIdToBe(Content.ME_FRIENDS, 308291);
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
        expect(sync(Content.ME_CONNECTIONS.uri).change).toEqual(Result.CHANGED);
        expect(Content.ME_CONNECTIONS).toHaveCount(4);

        TestHelper.addPendingHttpResponse(getClass(), "connections.json");
        expect(sync(Content.ME_CONNECTIONS.uri).change).toEqual(Result.UNCHANGED);
        expect(Content.ME_CONNECTIONS).toHaveCount(4);

        TestHelper.addPendingHttpResponse(getClass(), "connections_add.json");
        expect(sync(Content.ME_CONNECTIONS.uri).change).toEqual(Result.CHANGED);
        expect(Content.ME_CONNECTIONS).toHaveCount(6);

        TestHelper.addPendingHttpResponse(getClass(), "connections_delete.json");
        expect(sync(Content.ME_CONNECTIONS.uri).change).toEqual(Result.CHANGED);
        expect(Content.ME_CONNECTIONS).toHaveCount(3);
    }

    @Test
    public void shouldPushNewPlaylist() throws Exception {
        populateMeSounds();

        TestHelper.addPendingHttpResponse(getClass(), "playlist.json");

        final ContentResolver contentResolver = DefaultTestRunner.application.getContentResolver();
        Playlist p = ScModelManagerTest.createNewPlaylist(contentResolver,
                SoundCloudApplication.MODEL_MANAGER);

        expect(p).not.toBeNull();

        expect(PlaylistDAO.insertAsMyPlaylist(contentResolver, p)).not.toBeNull();
        expect(Content.TRACKS).toHaveCount(50);
        expect(Content.ME_SOUNDS).toHaveCount(51);

        expect(new ApiSyncer(Robolectric.application).pushLocalPlaylists()).toBe(1);
        expect(Content.ME_SOUNDS).toHaveCount(51);

        expect(LocalCollectionDAO.fromContentUri(p.toUri(), contentResolver, true).shouldAutoRefresh()).toBeFalse();
    }

    private Result populateMeSounds() throws IOException {
        addResourceResponse("/e1/me/sounds/mini?limit=200&representation=mini&linked_partitioning=1", "me_sounds_mini.json");
        return sync(Content.ME_SOUNDS.uri);
    }

    @Test
    public void shouldSyncAPlaylist() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "playlist.json");
        Result result = sync(Content.PLAYLIST.forId(2524386l));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(result.change).toEqual(Result.CHANGED);
        expect(Content.PLAYLISTS).toHaveCount(1);

        Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylistWithTracks(2524386l);
        expect(p.title).toEqual("fall into fall");
        expect(p.getTrackCount()).toEqual(41);
        expect(p.tracks).not.toBeNull();

        final Track track = p.tracks.get(10);
        expect(track.title).toEqual("Mozart Parties - Where Has Everybody Gone (Regal Safari Remix)");
        expect(track.user).not.toBeNull();
        expect(track.user.username).toEqual("Regal Safari");
    }

    @Test
    public void shouldSyncPlaylistTracks() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "playlist_tracks.json");
        final Uri localUri = Content.PLAYLIST_TRACKS.forId(2524386l);
        Result result = sync(localUri);
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(result.change).toEqual(Result.CHANGED);
        expect(Content.TRACKS).toHaveCount(41);

        CollectionHolder<Track> trackHolder = SoundCloudApplication.MODEL_MANAGER.loadLocalContent(resolver,Track.class, localUri);
        expect(trackHolder.collection.size()).toBe(41);
        expect(trackHolder.collection.get(1).title).toEqual("Keaton Henson - All Things Must Pass");
    }

    @Test
    public void shouldSyncPlaylistWithAdditions() throws Exception {

        TestHelper.addPendingHttpResponse(getClass(), "tracks.json");
        Result result = sync(Content.TRACK_LOOKUP.forQuery("10853436,10696200,10602324"));
        expect(result.success).toBe(true);

        final Playlist playlist = new Playlist(2524386);

        expect(PlaylistDAO.addTrackToPlaylist(resolver, playlist, 10696200, System.currentTimeMillis())).not.toBeNull();
        expect(PlaylistDAO.addTrackToPlaylist(resolver, playlist, 10853436, System.currentTimeMillis() + 100)).not.toBeNull();

        TestHelper.addPendingHttpResponse(getClass(), "playlist.json", "playlist_added.json");

        result = sync(Content.PLAYLIST.forId(10696200));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(0L);
        expect(result.change).toEqual(Result.CHANGED);
        expect(Content.TRACKS).toHaveCount(44);

        Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylistWithTracks(playlist.id);
        expect(p.tracks.size()).toBe(43);
        expect(p.tracks.get(1).title).toEqual("recording on thursday afternoon");
    }

    @Test
    public void shouldDoTrackLookup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "tracks.json");
        Result result = sync(Content.TRACK_LOOKUP.forQuery("10853436,10696200,10602324"));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(result.change).toEqual(Result.CHANGED);
        expect(Content.TRACKS).toHaveCount(3);
    }

    @Test
    public void shouldDoUserLookup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "users.json");
        Result result = sync(Content.USER_LOOKUP.forQuery("308291,792584,1255758"));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(result.change).toEqual(Result.CHANGED);
        expect(Content.USERS).toHaveCount(3);
    }

    @Test
    public void shouldDoPlaylistLookup() throws Exception {
        TestHelper.addCannedResponse(getClass(), "/playlists?ids=3761799%2C1&representation=compact&linked_partitioning=1",
                "playlists_compact.json");
        Result result = sync(Content.PLAYLIST_LOOKUP.forQuery("3761799,1"));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(result.change).toEqual(Result.CHANGED);
        expect(Content.PLAYLISTS).toHaveCount(2);
    }

    @Test
    public void shouldSetSyncResultData() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "e1_activities_1_oldest.json");
        Result result = sync(Content.ME_ACTIVITIES.uri);
        expect(result.change).toEqual(Result.CHANGED);
        expect(result.new_size).toEqual(7);
        expect(result.synced_at).toBeGreaterThan(0l);
    }

    @Test
    public void shouldReturnUnchangedIfLocalStateEqualsRemote() throws Exception {
        addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        addCannedResponse(getClass(), "/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        Result result = sync(Content.ME_TRACKS.uri);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(Result.CHANGED);
        expect(result.synced_at).toBeGreaterThan(0l);

        addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        addCannedResponse(getClass(), "/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        result = sync(Content.ME_TRACKS.uri);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(Result.UNCHANGED);
        expect(result.synced_at).toBeGreaterThan(0l);
        expect(result.extra).toBeNull();
    }

    @Test
    public void shouldNotOverwriteDataFromPreviousSyncRuns() throws Exception {
        addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        addResourceResponse("/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        sync(Content.ME_TRACKS.uri);
        expect(Content.TRACKS).toHaveCount(3);

        // sync activities concerning these tracks
        sync(Content.ME_ACTIVITIES.uri, "tracks_activities.json");

        Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(10853436L);
        expect(t).not.toBeNull();
        expect(t.duration).toEqual(782);
        // title should get changed from the activity json
        expect(t.title).toEqual("recording on sunday night (edit)");

        User u = SoundCloudApplication.MODEL_MANAGER.getUser(3135930L);
        expect(u).not.toBeNull();
        expect(u.username).toEqual("I'm your father");
        // permalink was set in first sync run, not present in second
        expect(u.permalink).toEqual("soundcloud-android-mwc");
    }

    @Test
    public void shouldNotOverwriteDataFromPreviousSyncRuns2() throws Exception {
        sync(Content.ME_ACTIVITIES.uri, "tracks_activities.json");

        addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        addResourceResponse("/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        sync(Content.ME_TRACKS.uri);

        expect(Content.TRACKS).toHaveCount(3);

        Track t = SoundCloudApplication.MODEL_MANAGER.getTrack(10853436L);
        expect(t).not.toBeNull();
        expect(t.duration).toEqual(782);
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
        Activities incoming = ActivitiesDAO.getSince(Content.ME_SOUND_STREAM, resolver, -1);

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
        Activities incoming = ActivitiesDAO.getSince(Content.ME_SOUND_STREAM, resolver, -1);

        expect(incoming.size()).toEqual(1);
        Activity a1 = incoming.get(0);

        expect(a1).toBeInstanceOf(PlaylistActivity.class);
        expect(a1.getPlayable().permalink).toEqual("private-share-test");
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOException() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "error");
        sync(Content.ME_FOLLOWERS.uri);
    }

    private Result sync(Uri uri,  String... fixtures) throws IOException {
        addPendingHttpResponse(getClass(), fixtures);
        ApiSyncer syncer = new ApiSyncer(Robolectric.application);
        return syncer.syncContent(uri, Intent.ACTION_SYNC);
    }

    private void addResourceResponse(String url, String resource) throws IOException {
        TestHelper.addCannedResponse(getClass(), url, resource);
    }
}
