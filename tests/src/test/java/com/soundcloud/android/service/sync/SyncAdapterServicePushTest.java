package com.soundcloud.android.service.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.service.sync.CollectionSyncRequestTest.NON_INTERACTIVE;
import static com.soundcloud.android.utils.IOUtils.readInputStream;
import static com.xtremelabs.robolectric.Robolectric.addHttpResponseRule;

import com.soundcloud.android.Consts;
import com.soundcloud.android.c2dm.PushEvent;
import com.soundcloud.android.dao.LocalCollectionDAO;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.junit.Test;

import android.os.Bundle;


/**
 * Push related sync tests.
 */
public class SyncAdapterServicePushTest extends SyncAdapterServiceTestBase {

    @Test
    public void shouldOnlySyncActivitiesFromPushEventLike() throws Exception {
        shouldOnlySyncActivitiesFromPushEvent(PushEvent.LIKE.type);
    }

    @Test
    public void shouldOnlySyncActivitiesFromPushEventComment() throws Exception {
        shouldOnlySyncActivitiesFromPushEvent(PushEvent.COMMENT.type);
    }

    @Test
    public void shouldShowNewFetchedFollower() throws Exception {
        TestHelper.addIdResponse("/me/followers/ids?linked_partitioning=1" + NON_INTERACTIVE, 792584, 1255758, 308291);
        addResourceResponse("/me/followers?linked_partitioning=1&limit=" + Consts.COLLECTION_PAGE_SIZE + NON_INTERACTIVE, "users.json");

        addHttpResponseRule("GET", "/users/12345",
                new TestHttpResponse(200, readInputStream(getClass().getResourceAsStream("user.json"))));

        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, PushEvent.FOLLOWER.type);
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT_URI, "soundcloud:users:12345");

        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);
        expect(result.notifications.size()).toEqual(1);

        expect(result.getTicker()).toEqual("New follower");
        expect(result.getInfo().getContentTitle().toString()).toEqual("You have a new follower");
        expect(result.getInfo().getContentText().toString()).toEqual("SoundCloud Android @ MWC is now following you. Follow back?");
    }

    @Test
    public void shouldCheckPushEventExtraParameterLike() throws Exception {
        addCannedActivities("e1_activities_1_oldest.json");

        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, PushEvent.LIKE.type);
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);

        expect(result.notifications.size()).toEqual(1);
    }

    @Test
    public void shouldCheckPushEventExtraParameterComment() throws Exception {
        addCannedActivities("e1_activities_2.json");

        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, PushEvent.COMMENT.type);
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);

        expect(result.notifications.size()).toEqual(1);
    }


    @Test
    public void shouldCheckPushEventExtraParameterUnknown() throws Exception {
        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, "alien-sync");
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);
        expect(result.notifications.size()).toEqual(0);
    }


    private void shouldOnlySyncActivitiesFromPushEvent(String pushType) throws Exception {
        addCannedActivities("e1_activities_2.json");

        // add my sounds should sync
        SyncContent.MySounds.setEnabled(Robolectric.application, true);
        TestHelper.addIdResponse("/me/tracks/ids?linked_partitioning=1", 1, 2, 3);
        TestHelper.addCannedResponse(getClass(), "/tracks?linked_partitioning=1&limit=200&ids=1%2C2%2C3", "tracks.json");


        Bundle extras = new Bundle();
        extras.putString(SyncAdapterService.EXTRA_PUSH_EVENT, pushType);
        SyncOutcome result = doPerformSync(DefaultTestRunner.application, false, extras);

        LocalCollection lc = LocalCollectionDAO.fromContent(Content.ME_TRACKS, Robolectric.application.getContentResolver(), false);
        expect(lc).toBeNull();
        expect(result.notifications.size()).toEqual(1);
    }
}
