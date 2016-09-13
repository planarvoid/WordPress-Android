package com.soundcloud.android.sync;

import static com.soundcloud.android.sync.BackgroundSyncer.BACKOFF_MULTIPLIERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.sync.BackgroundSyncer.Result;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;

public class BackgroundSyncerTest extends AndroidUnitTest {

    private static final int STALE_TIME = 100;
    @Mock private AccountOperations accountOperations;
    @Mock private Context context;
    @Mock private SyncerRegistry syncerRegistry;
    @Mock private SyncStateStorage syncStateStorage;
    @Mock private BackgroundSyncResultReceiver resultReceiver;

    @Captor private ArgumentCaptor<Intent> intentArgumentCaptor;
    private BackgroundSyncer syncer;


    @Before
    public void setUp() throws Exception {
        syncer = new BackgroundSyncer(accountOperations, syncStateStorage, syncerRegistry, context, resultReceiver);

        when(syncerRegistry.get(any(Syncable.class))).thenReturn(TestSyncData.forStaleTime(Syncable.CHARTS, 0));
        when(syncStateStorage.hasSyncedWithin(any(Syncable.class), anyLong())).thenReturn(true);
    }

    @Test
    public void doesNotSyncWhenNotLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        assertThat(syncer.sync()).isEqualTo(Result.UNAUTHORIZED);

        verifyNoSyncInitiated();
    }

    @Test
    public void syncsWithUiRequestSetToFalse() {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);

        when(syncerRegistry.get(Syncable.CHARTS)).thenReturn(TestSyncData.forStaleTime(Syncable.CHARTS, STALE_TIME));
        when(syncStateStorage.hasSyncedWithin(Syncable.CHARTS, STALE_TIME)).thenReturn(false);

        assertThat(syncer.sync()).isEqualTo(Result.SYNCING);

        assertThat(getServiceIntent().getBooleanExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)).isFalse();
    }

    @Test
    public void syncsWithStaleSyncables() {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);

        when(syncerRegistry.get(Syncable.CHARTS)).thenReturn(TestSyncData.forStaleTime(Syncable.CHARTS, STALE_TIME));
        when(syncStateStorage.hasSyncedWithin(Syncable.CHARTS, STALE_TIME)).thenReturn(false);

        assertThat(syncer.sync()).isEqualTo(Result.SYNCING);

        verifySyncInitiated(Syncable.CHARTS);
    }

    @Test
    public void syncsWithStaleSyncablesIncludingBackoff() {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);

        final int misses = 2;
        when(syncerRegistry.get(Syncable.CHARTS)).thenReturn(TestSyncData.forStaleTime(Syncable.CHARTS, STALE_TIME));
        when(syncStateStorage.getSyncMisses(Syncable.CHARTS)).thenReturn(misses);
        when(syncStateStorage.hasSyncedWithin(Syncable.CHARTS, STALE_TIME)).thenReturn(false);
        when(syncStateStorage.hasSyncedWithin(Syncable.CHARTS, STALE_TIME * BACKOFF_MULTIPLIERS[misses])).thenReturn(
                false);

        assertThat(syncer.sync()).isEqualTo(Result.SYNCING);

        verifySyncInitiated(Syncable.CHARTS);
    }

    @Test
    public void doesNotSyncWithUnstaleSyncablesIncludingBackoff() {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);

        final int misses = 2;
        when(syncerRegistry.get(Syncable.CHARTS)).thenReturn(TestSyncData.forStaleTime(Syncable.CHARTS, STALE_TIME));
        when(syncStateStorage.getSyncMisses(Syncable.CHARTS)).thenReturn(misses);
        when(syncStateStorage.hasSyncedWithin(Syncable.CHARTS, STALE_TIME)).thenReturn(false);
        when(syncStateStorage.hasSyncedWithin(Syncable.CHARTS, STALE_TIME * BACKOFF_MULTIPLIERS[misses])).thenReturn(
                true);

        assertThat(syncer.sync()).isEqualTo(Result.NO_SYNC);

        verifyNoSyncInitiated();
    }

    @Test
    public void doesNotSyncWithoutStaleSyncables() {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(syncerRegistry.get(any(Syncable.class))).thenReturn(TestSyncData.forStaleTime(Syncable.CHARTS, 0));
        when(syncStateStorage.hasSyncedWithin(any(Syncable.class), anyLong())).thenReturn(true);

        assertThat(syncer.sync()).isEqualTo(Result.NO_SYNC);

        verifyNoSyncInitiated();
    }

    @Test
    public void syncsStaleSyncablesWhenForced() {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(syncerRegistry.get(any(Syncable.class))).thenReturn(TestSyncData.forStaleTime(Syncable.CHARTS, 0));
        when(syncStateStorage.hasSyncedWithin(any(Syncable.class), anyLong())).thenReturn(true);

        assertThat(syncer.sync(true)).isEqualTo(Result.SYNCING);

        final Intent intent = getServiceIntent();
        assertThat(SyncIntentHelper.getSyncables(intent)).isNotEmpty();
    }

    private void verifySyncInitiated(Syncable... syncables) {
        final Intent intent = getServiceIntent();
        assertThat(SyncIntentHelper.getSyncables(intent)).containsExactly(syncables);
    }

    private void verifyNoSyncInitiated() {
        verify(context, never()).startService(any(Intent.class));
    }

    private Intent getServiceIntent() {
        verify(context).startService(intentArgumentCaptor.capture());
        return intentArgumentCaptor.getValue();
    }
}
