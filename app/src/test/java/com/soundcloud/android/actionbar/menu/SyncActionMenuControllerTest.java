package com.soundcloud.android.actionbar.menu;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.SyncLikesDialog;
import com.soundcloud.android.payments.SubscribeActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import javax.inject.Provider;

@RunWith(SoundCloudTestRunner.class)
public class SyncActionMenuControllerTest {

    private SyncActionMenuController controller;
    private Fragment fragment;
    private FragmentActivity fragmentActivity;

    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineOperations;
    @Mock private SyncLikesDialog syncLikesDialog;

    @Before
    public void setUp() throws Exception {
        controller = new SyncActionMenuController(castConnectionHelper,
                featureOperations,
                offlineOperations,
                new Provider<SyncLikesDialog>() {
                    @Override
                    public SyncLikesDialog get() {
                        return syncLikesDialog;
                    }
                });
        fragment = new Fragment();
        fragmentActivity = new FragmentActivity();
        Robolectric.shadowOf(fragment).setActivity(fragmentActivity);
    }

    @Test
    public void clickStartSyncNameOnOfflineSyncAvailableShowsSyncLikesDialog() {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(R.id.action_start_sync);
        when(featureOperations.isOfflineSyncEnabled()).thenReturn(true);

        controller.onOptionsItemSelected(fragment, item);

        verify(syncLikesDialog).show(any(FragmentManager.class));
    }

    @Test
    public void clickStartSyncNameOnOfflineSyncUnavailableShowsUpsell() {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(R.id.action_start_sync);
        when(featureOperations.isOfflineSyncUpsellEnabled()).thenReturn(false);

        controller.onOptionsItemSelected(fragment, item);

        Intent nextStartedActivity = Robolectric.shadowOf(fragmentActivity).getNextStartedActivity();
        expect(nextStartedActivity).not.toBeNull();
        expect(nextStartedActivity.getComponent().getClassName()).toEqual(SubscribeActivity.class.getCanonicalName());
    }

    @Test
    public void removeSyncClickUnsetsLikesOfflineSync() {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(R.id.action_remove_sync);

        controller.onOptionsItemSelected(null, item);

        verify(offlineOperations).setLikesOfflineSync(false);
    }
}