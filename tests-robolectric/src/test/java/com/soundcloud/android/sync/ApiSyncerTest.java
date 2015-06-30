package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.addPendingHttpResponse;
import static com.soundcloud.android.testsupport.TestHelper.assertResolverNotified;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.api.legacy.model.activities.PlaylistActivity;
import com.soundcloud.android.api.legacy.model.activities.TrackActivity;
import com.soundcloud.android.api.legacy.model.activities.TrackSharingActivity;
import com.soundcloud.android.testsupport.matchers.RequestMatchers;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.api.legacy.InvalidTokenException;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class ApiSyncerTest {
    private static final int TOTAL_STREAM_SIZE = 119; // 120 - 1 dup

    ContentResolver resolver;
    SyncStateManager syncStateManager;
    ActivitiesStorage activitiesStorage;
    long startTime;

    @Mock private EventBus eventBus;
    @Mock private ApiClient apiClient;
    @Mock private AccountOperations accountOperations;

    @Before
    public void before() {
        final PublicApiUser value = ModelFixtures.create(PublicApiUser.class);
        when(accountOperations.getLoggedInUserId()).thenReturn(value.getId());
        when(accountOperations.getLoggedInUser()).thenReturn(value);

        resolver = DefaultTestRunner.application.getContentResolver();
        syncStateManager = new SyncStateManager(resolver, new LocalCollectionDAO(resolver));
        activitiesStorage = new ActivitiesStorage();
        startTime = System.currentTimeMillis();
    }

    @Test
    public void shouldSyncMe() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(RequestMatchers.isPublicApiRequestTo("GET", "/me")), eq(PublicApiUser.class)))
                .thenReturn(new PublicApiUser(123L));
        expect(Content.ME).toBeEmpty();
        ApiSyncResult result = sync(Content.ME.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(startTime);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
    }

    @Test(expected = InvalidTokenException.class)
    public void shouldThrowAuthExceptionFrom404FromSyncStream() throws Exception {
        Robolectric.getFakeHttpLayer().addPendingHttpResponse(new TestHttpResponse(HttpStatus.SC_NOT_FOUND, ""));
        sync(Content.ME_SOUND_STREAM.uri);
    }

    @Test
    public void shouldSyncStream() throws Exception {
        ApiSyncResult result = sync(Content.ME_SOUND_STREAM.uri,
                "e1_stream.json",
                "e1_stream_oldest.json");
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(startTime);

        expect(Content.ME_SOUND_STREAM).toHaveCount(TOTAL_STREAM_SIZE);
        expect(Content.TRACKS).toHaveCount(111);
        expect(Content.USERS).toHaveCount(28);
        expect(Content.PLAYLISTS).toHaveCount(8);

        Activities incoming = activitiesStorage.getCollectionSince(Content.ME_SOUND_STREAM.uri, -1);

        expect(incoming.size()).toEqual(TOTAL_STREAM_SIZE);
        expect(incoming.getUniquePlayables().size()).toEqual(TOTAL_STREAM_SIZE);
        assertResolverNotified(Content.ME_SOUND_STREAM.uri, Content.TRACKS.uri, Content.USERS.uri);
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOExceptionOnInvalidStreamResponse() throws Exception {
        sync(Content.ME_SOUND_STREAM.uri,"e1_stream_missing_track.json");
    }

    @Test
    public void syncStreamWithHardRefreshReplacesExistingActivities() throws Exception {
        // initial sync
        ApiSyncResult initialResult = sync(Content.ME_SOUND_STREAM.uri, "e1_stream.json", "e1_stream_oldest.json");

        // hard refresh
        addPendingHttpResponse(getClass(), "e1_stream_oldest.json");
        ApiSyncer syncer = new ApiSyncer(Robolectric.application, Robolectric.application.getContentResolver(),
                eventBus, apiClient, accountOperations);
        ApiSyncResult result = syncer.syncContent(Content.ME_SOUND_STREAM.uri, ApiSyncService.ACTION_HARD_REFRESH);

        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(initialResult.synced_at);
        expect(result.change).toBe(ApiSyncResult.CHANGED);
        expect(Content.ME_SOUND_STREAM).toHaveCount(69);

        Activities incoming = activitiesStorage.getCollectionSince(Content.ME_SOUND_STREAM.uri, -1);
        expect(incoming.size()).toEqual(69);
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
        PublicApiTrack t = new PublicApiTrack(c);
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
        expect(result.synced_at).toBeGreaterThan(startTime);


        expect(Content.ME_ACTIVITIES).toHaveCount(17);
        expect(Content.COMMENTS).toHaveCount(5);

        Activities own = activitiesStorage.getCollectionSince(Content.ME_ACTIVITIES.uri, -1);
        expect(own.size()).toEqual(17);

        assertResolverNotified(Content.TRACKS.uri,
                Content.USERS.uri,
                Content.COMMENTS.uri,
                Content.ME_ACTIVITIES.uri);
    }

    @Test
    public void shouldSyncSecondTimeWithCorrectRequest() throws Exception {
        ApiSyncResult result = sync(Content.ME_ACTIVITIES.uri,
                "e1_activities_1.json",
                "e1_activities_2.json");
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(startTime);

        TestHelper.addResourceResponse(getClass(),
                "/e1/me/activities?uuid%5Bto%5D=3d22f400-0699-11e2-919a-b494be7979e7&limit=100", "empty_collection.json");

        result = sync(Content.ME_ACTIVITIES.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(startTime);
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
    public void shouldDoTrackLookup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "tracks.json");
        ApiSyncResult result = sync(Content.TRACK_LOOKUP.forQuery("10853436,10696200,10602324"));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(startTime);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(Content.TRACKS).toHaveCount(3);
    }

    @Test
    public void shouldDoUserLookup() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "users.json");
        ApiSyncResult result = sync(Content.USER_LOOKUP.forQuery("308291,792584,1255758"));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(startTime);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(Content.USERS).toHaveCount(3);
    }

    @Test
    public void shouldDoPlaylistLookup() throws Exception {
        TestHelper.addCannedResponse(getClass(), "/playlists?ids=3761799%2C1&representation=compact&linked_partitioning=1",
                "playlists_compact.json");
        ApiSyncResult result = sync(Content.PLAYLIST_LOOKUP.forQuery("3761799,1"));
        expect(result.success).toBe(true);
        expect(result.synced_at).toBeGreaterThan(startTime);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(Content.PLAYLISTS).toHaveCount(2);
    }

    @Test
    public void shouldSetSyncResultData() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "e1_activities_1_oldest.json");
        ApiSyncResult result = sync(Content.ME_ACTIVITIES.uri);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(result.new_size).toEqual(7);
        expect(result.synced_at).toBeGreaterThan(startTime);
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
        Activities incoming = activitiesStorage.getCollectionSince(Content.ME_SOUND_STREAM.uri, -1);

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
        Activities incoming = activitiesStorage.getCollectionSince(Content.ME_SOUND_STREAM.uri, -1);

        expect(incoming.size()).toEqual(1);
        Activity a1 = incoming.get(0);

        expect(a1).toBeInstanceOf(PlaylistActivity.class);
        expect(a1.getPlayable().permalink).toEqual("private-share-test");
    }

    private ApiSyncResult sync(Uri uri, String... fixtures) throws IOException {
        addPendingHttpResponse(getClass(), fixtures);
        ApiSyncer syncer = new ApiSyncer(
                Robolectric.application, Robolectric.application.getContentResolver(), eventBus, apiClient, accountOperations);
        return syncer.syncContent(uri, Intent.ACTION_SYNC);
    }
}
