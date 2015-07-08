package com.soundcloud.android.likes;

import static com.soundcloud.android.actionbar.menu.ActionMenuController.STATE_REMOVE_SYNC;
import static com.soundcloud.android.actionbar.menu.ActionMenuController.STATE_START_SYNC;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.actionbar.menu.DefaultActionMenuController;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

public class TrackLikesActionMenuControllerTest extends AndroidUnitTest {

    @Mock private DefaultActionMenuController actionMenuController;
    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineOperations;
    @Mock private Fragment fragment;
    @Mock private FragmentActivity activity;

    private TrackLikesActionMenuController controller;

    @Before
    public void setUp() throws Exception {
        controller = new TrackLikesActionMenuController(actionMenuController, offlineOperations);
        when(fragment.getActivity()).thenReturn(activity);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
    }

    @Test
    public void onCreateMenuWhenLikesOfflineSyncIsEnabledSetsActionMenuToRemoveSyncState() {
        when(offlineOperations.getOfflineLikesSettingsStatus()).thenReturn(Observable.<Boolean>empty());
        when(offlineOperations.isOfflineLikedTracksEnabled()).thenReturn(true);

        controller.onResume(fragment);
        controller.onCreateOptionsMenu(null, null);

        verify(actionMenuController).setState(STATE_REMOVE_SYNC);
    }

    @Test
    public void onCreateMenuWhenLikesOfflineSyncIsDisabledSetsActionMenuToStartSyncState() {
        when(offlineOperations.getOfflineLikesSettingsStatus()).thenReturn(Observable.<Boolean>empty());
        when(offlineOperations.isOfflineLikedTracksEnabled()).thenReturn(false);

        controller.onResume(fragment);
        controller.onCreateOptionsMenu(null, null);

        verify(actionMenuController).setState(STATE_START_SYNC);
    }

    @Test
    public void likeSettingsEnableEventSetsActionMenuToRemoveSyncState() {
        when(offlineOperations.getOfflineLikesSettingsStatus()).thenReturn(Observable.just(true));

        controller.onResume(fragment);

        verify(actionMenuController).setState(STATE_REMOVE_SYNC);
    }

    @Test
    public void likeSettingsDisableEventSetsActionMenuToStartSyncState() {
        when(offlineOperations.getOfflineLikesSettingsStatus()).thenReturn(Observable.just(false));

        controller.onResume(fragment);

        verify(actionMenuController).setState(STATE_START_SYNC);
    }

    @Test
    public void onResumeInvalidatesOptionsMenu() {
        when(offlineOperations.getOfflineLikesSettingsStatus()).thenReturn(Observable.<Boolean>empty());

        controller.onResume(fragment);

        verify(activity).supportInvalidateOptionsMenu();
    }
}