package com.soundcloud.android.accounts;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

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
import com.soundcloud.android.deeplinks.ShortcutController;
import com.soundcloud.android.discovery.DiscoveryWritableStorage;
import com.soundcloud.android.gcm.GcmStorage;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.olddiscovery.OldDiscoveryOperations;
import com.soundcloud.android.olddiscovery.recommendedplaylists.RecommendedPlaylistsStorage;
import com.soundcloud.android.search.PlaylistTagStorage;
import com.soundcloud.android.settings.notifications.NotificationPreferencesStorage;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.PersistentStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.stream.StreamOperations;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsStorage;
import com.soundcloud.android.sync.SyncCleanupAction;
import com.soundcloud.android.sync.playlists.RemoveLocalPlaylistsCommand;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.propeller.PropellerWriteException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.content.Context;
import android.content.SharedPreferences;

@RunWith(MockitoJUnitRunner.class)
public class AccountCleanupActionTest {

    private AccountCleanupAction action;

    @Mock private Context context;
    @Mock private PlaylistTagStorage tagStorage;
    @Mock private SoundRecorder soundRecorder;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor editor;
    @Mock private SoundCloudApplication soundCloudApplication;
    @Mock private UserAssociationStorage userAssociationStorage;
    @Mock private UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    @Mock private OfflineSettingsStorage offlineSettingsStorage;
    @Mock private FeatureStorage featureStorage;
    @Mock private RemoveLocalPlaylistsCommand removeLocalPlaylistsCommand;
    @Mock private OldDiscoveryOperations oldDiscoveryOperations;
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
    @Mock private RecommendedPlaylistsStorage recommendedPlaylistStorage;
    @Mock private GcmStorage gcmStorage;
    @Mock private PersistentStorage featureFlagsStorage;
    @Mock private CommentsStorage commentsStorage;
    @Mock private DatabaseManager databaseManager;
    @Mock private SuggestedCreatorsStorage suggestedCreatorsStorage;
    @Mock private ShortcutController shortcutController;
    @Mock private SecureFileStorage secureFileStorage;
    @Mock private DiscoveryWritableStorage discoveryWritableStorage;
    @Mock private WaveformOperations waveformOperations;

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
                                          oldDiscoveryOperations,
                                          clearTableCommand,
                                          stationsOperations,
                                          collectionOperations,
                                          streamOperations,
                                          configurationOperations,
                                          notificationPreferencesStorage,
                                          playHistoryStorage,
                                          recentlyPlayedStorage,
                                          recommendedPlaylistStorage,
                                          gcmStorage,
                                          featureFlagsStorage,
                                          commentsStorage,
                                          databaseManager,
                                          suggestedCreatorsStorage,
                                          shortcutController,
                                          secureFileStorage,
                                          discoveryWritableStorage,
                                          waveformOperations);
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
    public void shouldClearOfflineSettingsStorageAndResetSecureFileStorageInOrder() {
        action.call();

        InOrder inOrder = inOrder(offlineSettingsStorage, secureFileStorage);
        inOrder.verify(offlineSettingsStorage).clear();
        inOrder.verify(secureFileStorage).reset();
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
        verify(clearTableCommand).call(Tables.Likes.TABLE);
    }

    @Test
    public void shouldClearPosts() throws PropellerWriteException {
        action.call();
        verify(clearTableCommand).call(Tables.Posts.TABLE);
    }

    @Test
    public void shouldClearWaveforms() {
        action.call();
        verify(waveformOperations).clearWaveforms();
    }

    @Test
    public void shouldClearPolicies() {
        action.call();
        verify(clearTableCommand).call(Tables.TrackPolicies.TABLE);
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
        verify(oldDiscoveryOperations).clearData();
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
    public void shouldRemoveShortcuts() {
        action.call();

        verify(shortcutController).removeShortcuts();
    }

    @Test
    public void shouldClearDiscoveryDatabase() {
        action.call();

        verify(discoveryWritableStorage).clearData();
    }

    @Test
    public void shouldClearAllTables() {
        action.call();

        verify(databaseManager).clearTables();
    }

}
