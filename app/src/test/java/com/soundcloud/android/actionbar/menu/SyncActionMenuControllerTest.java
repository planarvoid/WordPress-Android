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
import com.soundcloud.android.offline.OfflineLikesDialog;
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
    @Mock private OfflineLikesDialog offlineLikesDialog;

    @Before
    public void setUp() throws Exception {
        controller = new SyncActionMenuController(castConnectionHelper,
                featureOperations,
                offlineOperations,
                new Provider<OfflineLikesDialog>() {
                    @Override
                    public OfflineLikesDialog get() {
                        return offlineLikesDialog;
                    }
                });
        fragment = new Fragment();
        fragmentActivity = new FragmentActivity();
        Robolectric.shadowOf(fragment).setActivity(fragmentActivity);
    }

    @Test
    public void clickStartSyncNameOnOfflineSyncAvailableShowsSyncLikesDialog() {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(R.id.action_start_offline_update);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        controller.onOptionsItemSelected(fragment, item);

        verify(offlineLikesDialog).show(any(FragmentManager.class));
    }

    @Test
    public void clickStartSyncNameOnOfflineSyncUnavailableShowsUpsell() {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(R.id.action_start_offline_update);
        when(featureOperations.isOfflineContentUpsellEnabled()).thenReturn(false);

        controller.onOptionsItemSelected(fragment, item);

        Intent nextStartedActivity = Robolectric.shadowOf(fragmentActivity).getNextStartedActivity();
        expect(nextStartedActivity).not.toBeNull();
        expect(nextStartedActivity.getComponent().getClassName()).toEqual(SubscribeActivity.class.getCanonicalName());
    }

    @Test
    public void removeSyncClickUnsetsLikesOfflineSync() {
        MenuItem item = mock(MenuItem.class);
        when(item.getItemId()).thenReturn(R.id.action_remove_offline_likes);

        controller.onOptionsItemSelected(null, item);

        verify(offlineOperations).setOfflineLikesEnabled(false);
    }
}