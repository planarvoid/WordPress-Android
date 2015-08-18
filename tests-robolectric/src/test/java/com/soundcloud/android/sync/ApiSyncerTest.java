package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.addPendingHttpResponse;
import static com.soundcloud.android.testsupport.TestHelper.assertResolverNotified;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.TestHelper;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.matchers.RequestMatchers;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class ApiSyncerTest {
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
    public void shouldSetSyncResultData() throws Exception {
        TestHelper.addPendingHttpResponse(getClass(), "e1_activities_1_oldest.json");
        ApiSyncResult result = sync(Content.ME_ACTIVITIES.uri);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(result.new_size).toEqual(7);
        expect(result.synced_at).toBeGreaterThan(startTime);
    }

    private ApiSyncResult sync(Uri uri, String... fixtures) throws IOException {
        addPendingHttpResponse(getClass(), fixtures);
        ApiSyncer syncer = new ApiSyncer(
                Robolectric.application, Robolectric.application.getContentResolver(), eventBus, apiClient, accountOperations);
        return syncer.syncContent(uri, Intent.ACTION_SYNC);
    }
}
