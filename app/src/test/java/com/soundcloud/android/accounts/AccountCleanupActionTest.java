package com.soundcloud.android.accounts;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.service.PlayQueueView;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.PlaylistTagStorage;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.sync.SyncStateManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;
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
    private EventBus eventBus;

    @Before
    public void setup() {
        action = new AccountCleanupAction(eventBus, context, syncStateManager,
                collectionStorage, activitiesStorage, userAssociationStorage, tagStorage, soundRecorder, c2DMReceiver, unauthorisedRequestRegistry);

        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(context.getApplicationContext()).thenReturn(soundCloudApplication);
    }

    @Test
    public void shouldClearSyncStateIfAccountRemovalSucceeds() {
        action.call();
        verify(syncStateManager).clear();
    }

    @Test
    public void shouldClearCollectionStorageIfAccountRemovalSucceeds() {
        action.call();
        verify(collectionStorage).clear();
    }

    @Test
    public void shouldClearActivitiesStorageIfAccountRemovalSucceeds() {
        action.call();
        verify(activitiesStorage).clear(null);
    }

    @Test
    public void shouldResetSoundRecorderIfAccountRemovalSucceeds() {
        action.call();
        verify(soundRecorder).reset();
    }

    @Test
    public void shouldNotClearPlayQueueManagersStateIfAccountRemovalSucceeds() {
        action.call();
        verifyZeroInteractions(playQueue);
    }

    @Test
    public void shouldPublishUserRemovalIfAccountRemovalSucceeds() {
        EventMonitor eventMonitor = EventMonitor.on(eventBus);

        action.call();

        CurrentUserChangedEvent event = eventMonitor.verifyEventOn(EventQueue.CURRENT_USER_CHANGED);
        assertEquals(event.getKind(), CurrentUserChangedEvent.USER_REMOVED);
    }

    @Test
    public void shouldBroadcastResetAllIntentIfAccountRemovalSucceeds() {
        action.call();
        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context, times(1)).sendBroadcast(argumentCaptor.capture());
        Intent broadcastIntent = argumentCaptor.getAllValues().get(0);
        assertThat(broadcastIntent.getAction(), is(PlaybackService.Actions.RESET_ALL));
    }

    @Test
    public void shouldUnregisterFromC2DMIfAccountRemovalSucceeds() {
        action.call();
        verify(c2DMReceiver).unregister(context);
    }

    @Test
    public void shouldClearLoggedInUserIfAccountRemovalSucceeds() {
        action.call();
        verify(soundCloudApplication).clearLoggedInUser();
    }

    @Test
    public void shouldClearUserAssociationStorageIfAccountRemovalSucceeds() {
        action.call();
        verify(userAssociationStorage).clear();
    }

    @Test
    public void shouldClearLastObservedTimestampIfAccountRemovalSucceeds() {
        action.call();
        verify(unauthorisedRequestRegistry).clearObservedUnauthorisedRequestTimestamp();
    }
}

