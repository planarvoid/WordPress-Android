package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineLikesDialog;
import com.soundcloud.android.payments.SubscribeActivity;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Provider;

@RunWith(SoundCloudTestRunner.class)
public class LikesMenuPresenterTest {

    private LikesMenuPresenter likesMenuPresenter;

    @Mock private PopupMenuWrapper.Factory popupMenuWrapperFactory;
    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflineLikesDialog offlineLikesDialog;

    @Mock private FragmentActivity context;
    @Mock private MenuItem menuItem;
    @Mock private View button;

    private Provider<OfflineLikesDialog> syncLikesDialogProvider;

    @Before
    public void setUp() {
        syncLikesDialogProvider = new Provider<OfflineLikesDialog>() {
            @Override
            public OfflineLikesDialog get() {
                return offlineLikesDialog;
            }
        };

        likesMenuPresenter = new LikesMenuPresenter(
                popupMenuWrapperFactory,
                featureOperations,
                offlineContentOperations,
                syncLikesDialogProvider);
    }

    @Test
    public void showSyncLikesDialogWhenClickOnMenuAndOfflineContentIsEnabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);

        MenuItem makeOfflineAvailable = mockMenuItem(R.id.action_make_offline_available);
        likesMenuPresenter.onMenuItemClick(makeOfflineAvailable, context);

        verify(offlineLikesDialog).show(any(FragmentManager.class));
    }

    @Test
    public void showDisableLikesSyncingWhenClickOnMakeOfflineUnavailableMenu() {
        MenuItem makeOfflineUnavailable = mockMenuItem(R.id.action_make_offline_unavailable);
        likesMenuPresenter.onMenuItemClick(makeOfflineUnavailable, context);

        verify(offlineContentOperations).setOfflineLikesEnabled(false);
        verifyZeroInteractions(offlineLikesDialog);
    }

    @Test
    public void showSubscribeActivityWhenLikesSyncEnabledAndOfflineContentDisabled() {
        MenuItem makeOfflineAvailable = mockMenuItem(R.id.action_make_offline_available);
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);

        likesMenuPresenter.onMenuItemClick(makeOfflineAvailable, Robolectric.application);
        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();

        expect(intent).not.toBeNull();
        expect(intent.getComponent().getClassName()).toEqual(SubscribeActivity.class.getCanonicalName());
        verifyZeroInteractions(offlineLikesDialog);
    }

    @Test
    public void showOfflineDownloadOptionWhenOfflineTracksDisabled() {
        PopupMenuWrapper popupMenuWrapper = mock(PopupMenuWrapper.class);

        when(button.getContext()).thenReturn(context);
        when(popupMenuWrapperFactory.build(context, button)).thenReturn(popupMenuWrapper);
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(false);

        likesMenuPresenter.show(button);

        verify(popupMenuWrapper).setItemVisible(R.id.action_make_offline_available, true);
        verify(popupMenuWrapper).setItemVisible(R.id.action_make_offline_unavailable, false);
        verify(popupMenuWrapper).show();
    }

    @Test
    public void showOfflineRemovalOptionWhenOfflineTracksEnabled() {
        PopupMenuWrapper popupMenuWrapper = mock(PopupMenuWrapper.class);

        when(button.getContext()).thenReturn(context);
        when(popupMenuWrapperFactory.build(context, button)).thenReturn(popupMenuWrapper);
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(true);

        likesMenuPresenter.show(button);

        verify(popupMenuWrapper).setItemVisible(R.id.action_make_offline_available, false);
        verify(popupMenuWrapper).setItemVisible(R.id.action_make_offline_unavailable, true);
        verify(popupMenuWrapper).show();
    }

    private MenuItem mockMenuItem(int menuItemId) {
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(menuItemId);
        return menuItem;
    }
}
