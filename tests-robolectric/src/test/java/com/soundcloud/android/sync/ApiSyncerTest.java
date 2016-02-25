package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.TestHelper.addPendingHttpResponse;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.EventBus;
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
    long startTime;

    @Mock private EventBus eventBus;
    @Mock private ApiClient apiClient;
    @Mock private AccountOperations accountOperations;
    @Mock private StoreTracksCommand storeTracksCommand;

    @Before
    public void before() {
        final PublicApiUser value = ModelFixtures.create(PublicApiUser.class);
        when(accountOperations.getLoggedInUserId()).thenReturn(value.getId());
        when(accountOperations.getLoggedInUser()).thenReturn(value);

        resolver = DefaultTestRunner.application.getContentResolver();
        syncStateManager = new SyncStateManager(resolver, new LocalCollectionDAO(resolver));
        startTime = System.currentTimeMillis();
    }

    @Test
    public void shouldSyncMe() throws Exception {
        when(apiClient.fetchMappedResponse(
                argThat(isPublicApiRequestTo("GET", "/me")), eq(PublicApiUser.class)))
                .thenReturn(new PublicApiUser(123L));
        expect(Content.ME).toBeEmpty();
        ApiSyncResult result = sync(Content.ME.uri);
        expect(result.success).toBeTrue();
        expect(result.synced_at).toBeGreaterThan(startTime);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
    }

    private ApiSyncResult sync(Uri uri, String... fixtures) throws IOException {
        addPendingHttpResponse(getClass(), fixtures);
        ApiSyncer syncer = new ApiSyncer(
                Robolectric.application, Robolectric.application.getContentResolver(), eventBus,
                apiClient, accountOperations, storeTracksCommand);
        return syncer.syncContent(uri, Intent.ACTION_SYNC);
    }
}
