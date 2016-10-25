package com.soundcloud.android.accounts;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.collection.CollectionOperations;
import com.soundcloud.android.collection.playhistory.PlayHistoryStorage;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedStorage;
import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.comments.CommentsStorage;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.PlanStorage;
import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.discovery.DiscoveryOperations;
import com.soundcloud.android.gcm.GcmStorage;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.PlaylistTagStorage;
import com.soundcloud.android.settings.notifications.NotificationPreferencesStorage;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.PersistentStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.stream.StreamOperations;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsStorage;
import com.soundcloud.android.sync.SyncCleanupAction;
import com.soundcloud.android.sync.playlists.RemoveLocalPlaylistsCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;
import android.content.SharedPreferences;

public class AccountCleanupActionTest extends AndroidUnitTest {

    private AccountCleanupAction action;

    @Mock private Context context;
    @Mock private PlaylistTagStorage tagStorage;
    @Mock private SoundRecorder soundRecorder;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor editor;
    @Mock private SoundCloudApplication soundCloudApplication;
    @Mock private UserAssociationStorage userAssociationStorage;
    @Mock private UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    @Mock private AccountOperations accountOperations;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;
    @Mock private FeatureStorage featureStorage;
    @Mock private RemoveLocalPlaylistsCommand removeLocalPlaylistsCommand;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private ClearTableCommand clearTableCommand;
    @Mock private SyncCleanupAction syncCleanupAction;
    @Mock private PlanStorage planStorage;
    @Mock private StationsOperations stationsOperations;
    @Mock private CollectionOperations collectionOperations;
    @Mock private StreamOperations streamOperations;
    @Mock private ConfigurationOperations configurationOperations;
    @Mock private NotificationPreferencesStorage notificationPreferencesStorage;
    @Mock private PlayHistoryStorage playHistoryStorage;
    @Mock private RecentlyPlayedStorage recentlyPlayedStorage;
    @Mock private GcmStorage gcmStorage;
    @Mock private PersistentStorage featureFlagsStorage;
    @Mock private CommentsStorage commentsStorage;
    @Mock private FeatureFlags featureFlags;
    @Mock private DatabaseManager databaseManager;
    @Mock private SuggestedCreatorsStorage suggestedCreatorsStorage;

    @Before
    public void setup() {
        action = new AccountCleanupAction(userAssociationStorage,
                                          tagStorage,
                                          soundRecorder,
                                          featureStorage,
                                          unauthorisedRequestRegistry,
                                          offlineSettingsStorage,
                                          syncCleanupAction,
                                          planStorage,
                                          removeLocalPlaylistsCommand,
                                          discoveryOperations,
                                          clearTableCommand,
                                          stationsOperations,
                                          collectionOperations,
                                          streamOperations,
                                          configurationOperations,
                                          notificationPreferencesStorage,
                                          playHistoryStorage,
                                          recentlyPlayedStorage,
                                          gcmStorage,
                                          featureFlagsStorage,
                                          commentsStorage,
                                          featureFlags,
                                          databaseManager,
                                          suggestedCreatorsStorage);

        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(context.getApplicationContext()).thenReturn(soundCloudApplication);
        when(soundCloudApplication.getAccountOperations()).thenReturn(accountOperations);
        when(featureFlags.isEnabled(Flag.CLEAR_TABLES_ON_SIGNOUT)).thenReturn(false);
    }

    @Test
    public void shouldClearSyncState() {
        action.call();
        verify(syncCleanupAction).clear();
    }

    @Test
    public void shouldResetSoundRecorder() {
        action.call();
        verify(soundRecorder).reset();
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
    public void shouldClearSuggestedCreatorsStorage() {
        action.call();
        verify(suggestedCreatorsStorage).clear();
    }

    @Test
    public void shouldClearFeatureStorage() {
        action.call();
        verify(featureStorage).clear();
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
    public void shouldClearActivitiesTable() throws PropellerWriteException {
        action.call();
        verify(clearTableCommand).call(Table.Activities);
    }

    @Test
    public void shouldClearCommentsTable() throws PropellerWriteException {
        action.call();

        verify(commentsStorage).clear();
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
    public void shouldClearPolicies() {
        action.call();
        verify(clearTableCommand).call(Table.TrackPolicies);
    }

    @Test
    public void shouldClearNotificationPreferences() {
        action.call();
        verify(notificationPreferencesStorage).clear();
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

    @Test
    public void shouldRemoveCollectionsStorage() {
        action.call();
        verify(collectionOperations).clearData();
    }

    @Test
    public void shouldRemoveUpsellPreferences() {
        action.call();
        verify(streamOperations).clearData();
    }

    @Test
    public void shouldRemovePlayHistory() {
        action.call();
        verify(playHistoryStorage).clear();
    }

    @Test
    public void shouldRemoveRecentlyPlayed() {
        action.call();
        verify(recentlyPlayedStorage).clear();
    }

    @Test
    public void shouldClearGcmTokenOnLogout() {
        action.call();
        verify(gcmStorage).clearTokenForRefresh();
    }

    @Test
    public void shouldClearFeatureFlagsOnLogout() {
        action.call();
        verify(featureFlagsStorage).clear();
    }

    @Test
    public void shouldNotClearAllTables() {
        action.call();

        verify(databaseManager, never()).clearTables();
    }

    @Test
    public void sholdClearAllTablesWhenFeatureFlagIsSet() {
        when(featureFlags.isEnabled(Flag.CLEAR_TABLES_ON_SIGNOUT)).thenReturn(true);

        action.call();

        verify(databaseManager).clearTables();
    }

}
