package com.soundcloud.android.accounts;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.playback.service.PlayQueueView;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.search.PlaylistTagStorage;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.sync.likes.RemoveAllLikesCommand;
import com.soundcloud.android.sync.playlists.RemoveLocalPlaylistsCommand;
import com.soundcloud.android.sync.posts.RemoveAllPostsCommand;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class AccountCleanupActionTest {

    private AccountCleanupAction action;

    @Mock private Context context;
    @Mock private SyncStateManager syncStateManager;
    @Mock private ActivitiesStorage activitiesStorage;
    @Mock private PlaylistTagStorage tagStorage;
    @Mock private SoundRecorder soundRecorder;
    @Mock private PlayQueueView playQueue;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor editor;
    @Mock private SoundCloudApplication soundCloudApplication;
    @Mock private UserAssociationStorage userAssociationStorage;
    @Mock private UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    @Mock private AccountOperations accountOperations;
    @Mock private ClearSoundStreamCommand clearSoundStreamCommand;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;
    @Mock private FeatureStorage featureStorage;
    @Mock private RemoveAllLikesCommand removeAllLikes;
    @Mock private RemoveAllPostsCommand removeAllPostsCommand;
    @Mock private RemoveLocalPlaylistsCommand removeLocalPlaylistsCommand;

    @Before
    public void setup() {
        action = new AccountCleanupAction(syncStateManager,
                activitiesStorage, userAssociationStorage, tagStorage, soundRecorder,
                featureStorage, unauthorisedRequestRegistry, clearSoundStreamCommand, offlineSettingsStorage, removeAllLikes, removeAllPostsCommand, removeLocalPlaylistsCommand);

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

    @Test
    public void shouldClearOfflineSettingsStorage() {
        action.call();
        verify(offlineSettingsStorage).clear();
    }

    @Test
    public void shouldClearFeatureStorage() {
        action.call();
        verify(featureStorage).clear();
    }

    @Test
    public void shouldClearSoundStreamStorage() throws PropellerWriteException {
        action.call();
        verify(clearSoundStreamCommand).call();
    }

    @Test
    public void shouldClearLikes() throws PropellerWriteException {
        action.call();
        verify(removeAllLikes).call();
    }

    @Test
    public void shouldClearPosts() throws PropellerWriteException {
        action.call();
        verify(removeAllPostsCommand).call(null);
    }

    @Test
    public void shouldRemoveLocalPlaylists() throws PropellerWriteException {
        action.call();
        verify(removeLocalPlaylistsCommand).call(null);
    }
}