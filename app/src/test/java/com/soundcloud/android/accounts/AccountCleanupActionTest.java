package com.soundcloud.android.accounts;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.configuration.PlanStorage;
import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.discovery.DiscoveryOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.search.PlaylistTagStorage;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.LegacyUserAssociationStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.sync.playlists.RemoveLocalPlaylistsCommand;
import com.soundcloud.android.sync.stream.StreamSyncStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.content.SharedPreferences;

public class AccountCleanupActionTest extends AndroidUnitTest {

    private AccountCleanupAction action;

    @Mock private Context context;
    @Mock private SyncStateManager syncStateManager;
    @Mock private ActivitiesStorage activitiesStorage;
    @Mock private PlaylistTagStorage tagStorage;
    @Mock private SoundRecorder soundRecorder;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor editor;
    @Mock private SoundCloudApplication soundCloudApplication;
    @Mock private LegacyUserAssociationStorage legacyUserAssociationStorage;
    @Mock private UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    @Mock private AccountOperations accountOperations;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;
    @Mock private FeatureStorage featureStorage;
    @Mock private RemoveLocalPlaylistsCommand removeLocalPlaylistsCommand;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private ClearTableCommand clearTableCommand;
    @Mock private StreamSyncStorage streamSyncStorage;
    @Mock private PlanStorage planStorage;
    @Mock private StationsOperations stationsOperations;

    @Before
    public void setup() {
        action = new AccountCleanupAction(syncStateManager,
                activitiesStorage, legacyUserAssociationStorage, tagStorage, soundRecorder,
                featureStorage, unauthorisedRequestRegistry, offlineSettingsStorage, streamSyncStorage, planStorage,
                removeLocalPlaylistsCommand, discoveryOperations, clearTableCommand, stationsOperations);

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
    public void shouldClearUserAssociationStorage() {
        action.call();
        verify(legacyUserAssociationStorage).clear();
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
    public void shouldClearStreamSyncStorage() {
        action.call();
        verify(streamSyncStorage).clear();
    }

    @Test
    public void shouldClearPlanStorage() {
        action.call();
        verify(planStorage).clear();
    }

    @Test
    public void shouldClearSoundStreamStorage() throws PropellerWriteException {
        action.call();
        verify(clearTableCommand).call(Table.SoundStream);
    }

    @Test
    public void shouldClearPromotedTracks() throws PropellerWriteException {
        action.call();
        verify(clearTableCommand).call(Table.PromotedTracks);
    }

    @Test
    public void shouldClearLikes() throws PropellerWriteException {
        action.call();
        verify(clearTableCommand).call(Table.Likes);
    }

    @Test
    public void shouldClearPosts() throws PropellerWriteException {
        action.call();
        verify(clearTableCommand).call(Table.Posts);
    }

    @Test
    public void shouldClearWaveforms() {
        action.call();
        verify(clearTableCommand).call(Table.Waveforms);
    }

    @Test
    public void shouldRemoveLocalPlaylists() throws PropellerWriteException {
        action.call();
        verify(removeLocalPlaylistsCommand).call(null);
    }

    @Test
    public void shouldRemoveRecommendations() throws PropellerWriteException {
        action.call();
        verify(discoveryOperations).clearData();
    }

    @Test
    public void shouldRemoveStationsStorage() {
        action.call();
        verify(stationsOperations).clearData();
    }
}
