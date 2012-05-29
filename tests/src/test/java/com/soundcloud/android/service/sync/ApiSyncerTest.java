package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Intent;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class ApiSyncerTest {

    @Test(expected = IOException.class)
    public void shouldThrowIOException() throws Exception {
        Robolectric.setDefaultHttpResponse(500, "error");
        ApiSyncer syncer = new ApiSyncer(Robolectric.application);
        syncer.syncContent(Content.ME_FOLLOWERS.uri, Intent.ACTION_SYNC);
    }

    @Test
    public void shouldUpdateLocalUser() throws Exception {
        TestHelper.addCannedResponses(getClass(), "me.json");
        ApiSyncer syncer = new ApiSyncer(Robolectric.application);
        ApiSyncer.Result result = syncer.syncContent(Content.ME.uri, Intent.ACTION_SYNC);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(ApiSyncer.Result.CHANGED);
    }

    @Test
    public void shouldSyncActivities() throws Exception {
        TestHelper.addCannedResponses(getClass(), "own_2.json");
        ApiSyncer syncer = new ApiSyncer(Robolectric.application);
        ApiSyncer.Result result = syncer.syncContent(Content.ME_ACTIVITIES.uri, Intent.ACTION_SYNC);
        expect(result.success).toBe(true);
    }

    @Test
    public void shouldSetSyncResultData() throws Exception {
        TestHelper.addCannedResponses(getClass(), "own_2.json");
        ApiSyncer syncer = new ApiSyncer(Robolectric.application);
        ApiSyncer.Result result = syncer.syncContent(Content.ME_ACTIVITIES.uri, Intent.ACTION_SYNC);
        expect(result.change).toEqual(ApiSyncer.Result.CHANGED);
        expect(result.new_size).toEqual(32);
        expect(result.synced_at).not.toEqual(0l);
    }

    @Test
    public void shouldSyncContent() throws Exception {
        TestHelper.addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        TestHelper.addCannedResponse(getClass(), "/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");
        ApiSyncer syncer = new ApiSyncer(Robolectric.application);
        ApiSyncer.Result result = syncer.syncContent(Content.ME_TRACKS.uri, Intent.ACTION_SYNC);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(ApiSyncer.Result.CHANGED);
        expect(result.extra).toEqual("0");
    }

    @Test
    public void shouldReturnUnchangedIfLocalStateEqualsRemote() throws Exception {
        TestHelper.addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        TestHelper.addCannedResponse(getClass(), "/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        ApiSyncer syncer = new ApiSyncer(Robolectric.application);
        ApiSyncer.Result result = syncer.syncContent(Content.ME_TRACKS.uri, Intent.ACTION_SYNC);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(ApiSyncer.Result.CHANGED);

        TestHelper.addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        TestHelper.addCannedResponse(getClass(), "/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");

        result = syncer.syncContent(Content.ME_TRACKS.uri, Intent.ACTION_SYNC);
        expect(result.success).toBe(true);
        expect(result.change).toEqual(ApiSyncer.Result.UNCHANGED);
        expect(result.extra).toBeNull();
    }
}
