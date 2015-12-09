package com.soundcloud.android.sync;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.soundcloud.android.storage.provider.Content;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;

@RunWith(DefaultTestRunner.class)
public class SyncStateManagerTest {

    SyncStateManager syncStateManager;
    ContentResolver resolver;
    @Mock LocalCollection.OnChangeListener onLocalCollectionChangeListener;

    @Before public void before() {
        MockitoAnnotations.initMocks(this);
        resolver = DefaultTestRunner.application.getContentResolver();
        syncStateManager = new SyncStateManager(resolver, new LocalCollectionDAO(resolver));
    }

    @Test
    public void shouldCreateSyncStateRecordLazily() {
        expect(Content.COLLECTIONS).toHaveCount(0);

        Uri contentUri = Content.PLAYLISTS.forId(123);
        LocalCollection syncState = syncStateManager.fromContent(contentUri);
        expect(Content.COLLECTIONS).toHaveCount(1);
        expect(syncState).not.toBeNull();
        expect(syncState.getUri()).toEqual(contentUri);
    }

    @Test
    public void shouldCreateSyncStateWithoutQueryparam() {
        final Uri uri = Content.PLAYLISTS.forId(123);
        Uri contentUri = uri.buildUpon().appendQueryParameter("foo","bar").build();
        expect(syncStateManager.fromContent(contentUri).getUri()).toEqual(uri);
    }

    @Test
    public void shouldInitializeNewLocalCollectionIfNotInDatabase() {
        final LocalCollection lc = new LocalCollection(Content.ME_LIKES.uri,
                System.currentTimeMillis(), System.currentTimeMillis(), LocalCollection.SyncState.SYNCING, 50, "extra");

        final LocalCollection initLc = new LocalCollection(Content.ME_LIKES.uri);
        initLc.setId(1);

        syncStateManager.onCollectionAsyncQueryReturn(null, lc, onLocalCollectionChangeListener);
        verify(onLocalCollectionChangeListener).onLocalCollectionChanged(initLc);
    }

    @Test
    public void shouldResolveContextToApplicationContextToPreventMemoryLeaks() {
        Activity activity = mock(Activity.class);
        when(activity.getApplicationContext()).thenReturn(activity);
        new SyncStateManager(activity);
        verify(activity, atLeastOnce()).getApplicationContext();
    }

}
