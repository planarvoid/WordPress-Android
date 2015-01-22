package com.soundcloud.android.likes;

import static com.soundcloud.android.actionbar.menu.ActionMenuController.STATE_REMOVE_SYNC;
import static com.soundcloud.android.actionbar.menu.ActionMenuController.STATE_START_SYNC;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.actionbar.menu.DefaultActionMenuController;
import com.soundcloud.android.actionbar.menu.SyncActionMenuController;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class TrackLikesActionMenuControllerTest {

    @Mock private SyncActionMenuController syncActionMenuController;
    @Mock private DefaultActionMenuController defaultActionMenuControllerProvider;
    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineOperations;

    private TrackLikesActionMenuController controller;

    @Before
    public void setUp() throws Exception {
        controller = new TrackLikesActionMenuController(syncActionMenuController,
                defaultActionMenuControllerProvider,
                featureOperations,
                offlineOperations);
    }

    @Test
    public void onCreateMenuWhenLikesOfflineSyncIsEnabledSetsSyncActionMenuToRemoveSyncState() {
        when(offlineOperations.getSettingsStatus()).thenReturn(Observable.<Boolean>empty());
        when(featureOperations.isEnabled(FeatureOperations.OFFLINE_SYNC, false)).thenReturn(true);
        when(offlineOperations.isLikesOfflineSyncEnabled()).thenReturn(true);

        controller.onResume();
        controller.onCreateOptionsMenu(null, null);

        verify(syncActionMenuController).setState(STATE_REMOVE_SYNC);
    }

    @Test
    public void onCreateMenuWhenLikesOfflineSyncIsDisabledSetsSyncActionMenuToStartSyncState() {
        when(offlineOperations.getSettingsStatus()).thenReturn(Observable.<Boolean>empty());
        when(featureOperations.isEnabled(FeatureOperations.OFFLINE_SYNC, false)).thenReturn(true);
        when(offlineOperations.isLikesOfflineSyncEnabled()).thenReturn(false);

        controller.onResume();
        controller.onCreateOptionsMenu(null, null);

        verify(syncActionMenuController).setState(STATE_START_SYNC);
    }

    @Test
    public void likeSettingsEnableEventSetsSyncActionMenuToRemoveSyncState() {
        when(offlineOperations.getSettingsStatus()).thenReturn(Observable.just(true));
        when(featureOperations.isEnabled(FeatureOperations.OFFLINE_SYNC, false)).thenReturn(true);

        controller.onResume();

        verify(syncActionMenuController).setState(STATE_REMOVE_SYNC);
    }

    @Test
    public void likeSettingsDisableEventSetsSyncActionMenuToStartSyncState() {
        when(offlineOperations.getSettingsStatus()).thenReturn(Observable.just(false));
        when(featureOperations.isEnabled(FeatureOperations.OFFLINE_SYNC, false)).thenReturn(true);

        controller.onResume();

        verify(syncActionMenuController).setState(STATE_START_SYNC);
    }
}