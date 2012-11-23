package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.*;
import static com.soundcloud.android.service.sync.ApiSyncer.Result;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
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
import android.content.SyncResult;
import android.net.Uri;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class ApiSyncerTest {
    private static final long USER_ID = 133201L;
    private ContentResolver resolver;

    @Before
    public void before() {
        DefaultTestRunner.application.setCurrentUserId(USER_ID);
        resolver = DefaultTestRunner.application.getContentResolver();
    }

    @Test
    public void shouldSyncMe() throws Exception {
        addCannedResponses(getClass(), "me.json");
        expect(Content.ME).toBeEmpty();
        Result result = sync(Content.ME.uri);
        expect(Content.ME).toHaveCount(1);
        expect(Content.USERS).toHaveCount(1);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(Result.CHANGED);
    }

    @Test
    public void shouldSyncStream() throws Exception {
        sync(Content.ME_SOUND_STREAM.uri,
                "e1_stream.json",
                "e1_stream_oldest.json");

        expect(Content.ME_SOUND_STREAM).toHaveCount(112);
        expect(Content.ME_EXCLUSIVE_STREAM).toHaveCount(0);
        expect(Content.TRACKS).toHaveCount(111);
        expect(Content.USERS).toHaveCount(28);
        //expect(Content.PLAYLISTS).toHaveCount(8);

        Activities incoming = Activities.getSince(Content.ME_SOUND_STREAM, resolver, -1);

        expect(incoming.size()).toEqual(112);
        expect(incoming.getUniqueTracks().size()).toEqual(111); // currently excluding playlists
        assertResolverNotified(Content.ME_SOUND_STREAM.uri, Content.TRACKS.uri, Content.USERS.uri);
    }

    @Test
    public void shouldSyncActivities() throws Exception {
        sync(Content.ME_ACTIVITIES.uri,
                "e1_activities_1.json",
                "e1_activities_2.json");

        expect(Content.ME_ACTIVITIES).toHaveCount(17);
        expect(Content.COMMENTS).toHaveCount(5);

        Activities own = Activities.getSince(Content.ME_ACTIVITIES, Robolectric.application.getContentResolver(), -1);
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

        sync(Content.ME_FOLLOWERS.uri);

        // make sure tracks+users got written
        expect(Content.USERS).toHaveCount(3);
        expect(Content.ME_FOLLOWERS).toHaveCount(3);
        assertFirstIdToBe(Content.ME_FOLLOWERS, 308291);
    }

    @Test
    public void shouldSyncTracks() throws Exception {
        addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        addResourceResponse("/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        sync(Content.ME_TRACKS.uri);

        // make sure tracks+users got written
        expect(Content.TRACKS).toHaveCount(3);
        expect(Content.USERS).toHaveCount(1);
        assertResolverNotified(Content.ME_TRACKS.uri, Content.TRACKS.uri, Content.USERS.uri);
    }

    @Test
    public void shouldSyncSounds() throws Exception {
        addResourceResponse("/e1/me/sounds/mini?limit=200&linked_partitioning=1", "me_sounds_mini.json");

        Result result = sync(Content.ME_SOUNDS.uri);
        expect(result.success).toBeTrue();
    }

    @Test
    public void shouldDoTrackLookup() throws Exception {
        TestHelper.addCannedResponses(getClass(), "tracks.json");
        Result result = sync(Content.TRACK_LOOKUP.forQuery("10853436,10696200,10602324"));
        expect(result.success).toBe(true);
        expect(result.change).toEqual(Result.CHANGED);
        expect(Content.TRACKS).toHaveCount(3);
    }

    @Test
    public void shouldDoUserLookup() throws Exception {
        TestHelper.addCannedResponses(getClass(), "users.json");
        Result result = sync(Content.USER_LOOKUP.forQuery("308291,792584,1255758"));
        expect(result.success).toBe(true);
        expect(result.change).toEqual(Result.CHANGED);
        expect(Content.USERS).toHaveCount(3);
    }

    @Test
    public void shouldSetSyncResultData() throws Exception {
        TestHelper.addCannedResponses(getClass(), "e1_activities_1_oldest.json");
        Result result = sync(Content.ME_ACTIVITIES.uri);
        expect(result.change).toEqual(Result.CHANGED);
        expect(result.new_size).toEqual(7);
        expect(result.synced_at).not.toEqual(0l);
    }

    @Test
    public void shouldReturnUnchangedIfLocalStateEqualsRemote() throws Exception {
        addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        addCannedResponse(getClass(), "/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        Result result = sync(Content.ME_TRACKS.uri);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(Result.CHANGED);

        addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        addCannedResponse(getClass(), "/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        result = sync(Content.ME_TRACKS.uri);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(Result.UNCHANGED);
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
    public void shouldSyncDifferentEndoints() throws Exception {
        sync(Content.ME_ACTIVITIES.uri,
                "e1_activities_1.json",
                "e1_activities_2.json");

        sync(Content.ME_SOUND_STREAM.uri,
                "e1_stream.json",
                "e1_stream_oldest.json");

        expect(Content.ME_SOUND_STREAM).toHaveCount(112);
        expect(Content.ME_ACTIVITIES).toHaveCount(17);
        expect(Content.ME_EXCLUSIVE_STREAM).toHaveCount(0);
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(137);
    }

    @Test
    public void shouldNotProduceDuplicatesWhenSyncing() throws Exception {
        sync(Content.ME_SOUND_STREAM.uri, "e1_stream_1_oldest.json");
        sync(Content.ME_EXCLUSIVE_STREAM.uri, "e1_stream_1_oldest.json");

        expect(Content.ME_SOUND_STREAM).toHaveCount(20);
        expect(Content.ME_EXCLUSIVE_STREAM).toHaveCount(20);
        expect(Content.ME_ALL_ACTIVITIES).toHaveCount(44);
    }

    @Test
    public void shouldFilterOutDuplicateTrackAndSharingsAndKeepSharings() throws Exception {
        // TODO, removed duplicate handling. Figure out how to handle with reposts now
        //  1 unrelated track + 2 track-sharing/track with same id
        sync(Content.ME_SOUND_STREAM.uri, "track_and_track_sharing.json");

        expect(Content.ME_SOUND_STREAM).toHaveCount(3);
        Activities incoming = Activities.getSince(Content.ME_SOUND_STREAM, resolver, -1);

        expect(incoming.size()).toEqual(3); // this is with a duplicate
        Activity a1 = incoming.get(0);
        Activity a2 = incoming.get(1);

        expect(a1).toBeInstanceOf(TrackActivity.class);
        expect(a1.getTrack().permalink).toEqual("bastard-amo1-edit");
        expect(a2).toBeInstanceOf(TrackSharingActivity.class);
        expect(a2.getTrack().permalink).toEqual("leotrax06-leo-zero-boom-bam");
    }

    @Test(expected = IOException.class)
    public void shouldThrowIOException() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "error");
        sync(Content.ME_FOLLOWERS.uri);
    }


    private Result sync(Uri uri,  String... fixtures) throws IOException {
        addCannedResponses(getClass(), fixtures);
        ApiSyncer syncer = new ApiSyncer(Robolectric.application);
        return syncer.syncContent(uri, Intent.ACTION_SYNC);
    }

    private void addResourceResponse(String url, String resource) throws IOException {
        TestHelper.addCannedResponse(getClass(), url, resource);
    }
}
