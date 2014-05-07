package com.soundcloud.android.accounts;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.playback.service.PlayQueueView;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.PlaylistTagStorage;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.sync.SyncStateManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class AccountCleanupActionTest {

    private AccountCleanupAction action;

    @Mock
    private Context context;
    @Mock
    private SyncStateManager syncStateManager;
    @Mock
    private CollectionStorage collectionStorage;
    @Mock
    private ActivitiesStorage activitiesStorage;
    @Mock
    private PlaylistTagStorage tagStorage;
    @Mock
    private SoundRecorder soundRecorder;
    @Mock
    private PlayQueueView playQueue;
    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor editor;
    @Mock
    private SoundCloudApplication soundCloudApplication;
    @Mock
    private C2DMReceiver c2DMReceiver;
    @Mock
    private UserAssociationStorage userAssociationStorage;
    @Mock
    private UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    @Mock
    private AccountOperations accountOperations;

    @Before
    public void setup() {
        action = new AccountCleanupAction(context, syncStateManager,
                collectionStorage, activitiesStorage, userAssociationStorage, tagStorage, soundRecorder, c2DMReceiver, unauthorisedRequestRegistry);

        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(context.getApplicationContext()).thenReturn(soundCloudApplication);
        when(soundCloudApplication.getAccountOperations()).thenReturn(accountOperations);
    }

    @Test
    public void shouldClearSyncState() {
        action.call();
        verify(syncStateManager).clear();
    }

    @Test
    public void shouldClearCollectionStorage() {
        action.call();
        verify(collectionStorage).clear();
    }

    @Test
    public void shouldClearActivitiesStorage() {
        action.call();
        verify(activitiesStorage).clear(null);
    }

    @Test
    public void shouldResetSoundRecorder() {
        action.call();
        verify(soundRecorder).reset();
    }

    @Test
    public void shouldNotClearPlayQueueManagersState() {
        action.call();
        verifyZeroInteractions(playQueue);
    }

    @Test
    public void shouldUnregisterFromC2DM() {
        action.call();
        verify(c2DMReceiver).unregister(context);
    }

    @Test
    public void shouldClearUserAssociationStorage() {
        action.call();
        verify(userAssociationStorage).clear();
    }

    @Test
    public void shouldClearLastObservedTimestamp() {
        action.call();
        verify(unauthorisedRequestRegistry).clearObservedUnauthorisedRequestTimestamp();
    }

    @Test
    public void shouldClearPlaylistTagStorage() {
        action.call();
        verify(tagStorage).clear();
    }
}

