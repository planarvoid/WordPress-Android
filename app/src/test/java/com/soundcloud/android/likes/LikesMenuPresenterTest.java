package com.soundcloud.android.likes;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineLikesDialog;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Provider;

public class LikesMenuPresenterTest extends AndroidUnitTest {

    private LikesMenuPresenter likesMenuPresenter;

    @Mock private PopupMenuWrapper.Factory popupMenuWrapperFactory;
    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflineLikesDialog offlineLikesDialog;
    @Mock private Navigator navigator;
    @Mock private FragmentManager fragmentManager;
    @Mock private FragmentActivity context;
    @Mock private MenuItem menuItem;
    @Mock private View button;
    @Mock private PopupMenuWrapper popupMenuWrapper;
    @Mock private EventBus eventBus;
    @Captor private ArgumentCaptor<PopupMenuWrapper.PopupMenuWrapperListener> listenerCaptor;

    @Before
    public void setUp() {
        Provider<OfflineLikesDialog> syncLikesDialogProvider = new Provider<OfflineLikesDialog>() {
            @Override
            public OfflineLikesDialog get() {
                return offlineLikesDialog;
            }
        };
        when(popupMenuWrapperFactory.build(context, button)).thenReturn(popupMenuWrapper);
        when(button.getContext()).thenReturn(context);
        likesMenuPresenter = new LikesMenuPresenter(
                popupMenuWrapperFactory,
                featureOperations,
                offlineContentOperations,
                syncLikesDialogProvider,
                navigator,
                eventBus);
    }

    @Test
    public void showSyncLikesDialogWhenClickOnMenuAndOfflineContentIsEnabled() {
        MenuItem makeOfflineAvailable = mockMenuItem(R.id.action_make_offline_available);
        showMenu(true, false);

        verify(popupMenuWrapper).setOnMenuItemClickListener(listenerCaptor.capture());
        listenerCaptor.getValue().onMenuItemClick(makeOfflineAvailable, context);

        verify(offlineLikesDialog).show(any(FragmentManager.class));
    }

    @Test
    public void showDisableLikesSyncingWhenClickOnMakeOfflineUnavailableMenu() {
        when(offlineContentOperations.disableOfflineLikedTracks()).thenReturn(Observable.just(true));
        MenuItem makeOfflineUnavailable = mockMenuItem(R.id.action_make_offline_unavailable);
        showMenu(true, true);

        verify(popupMenuWrapper).setOnMenuItemClickListener(listenerCaptor.capture());
        listenerCaptor.getValue().onMenuItemClick(makeOfflineUnavailable, context);

        verify(offlineContentOperations).disableOfflineLikedTracks();
        verifyZeroInteractions(offlineLikesDialog);
    }

    @Test
    public void showSubscribeActivityWhenLikesSyncEnabledAndOfflineContentDisabled() {
        MenuItem makeOfflineAvailable = mockMenuItem(R.id.action_make_offline_available);
        showMenu(false, false);

        verify(popupMenuWrapper).setOnMenuItemClickListener(listenerCaptor.capture());
        listenerCaptor.getValue().onMenuItemClick(makeOfflineAvailable, context);

        verify(navigator).openUpgrade(context);
        verifyZeroInteractions(offlineLikesDialog);
    }

    @Test
    public void showOfflineDownloadOptionWhenOfflineTracksDisabled() {
        PopupMenuWrapper popupMenuWrapper = mock(PopupMenuWrapper.class);

        when(button.getContext()).thenReturn(context);
        when(popupMenuWrapperFactory.build(context, button)).thenReturn(popupMenuWrapper);
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(false));

        likesMenuPresenter.show(button, fragmentManager);

        verify(popupMenuWrapper).setItemVisible(R.id.action_make_offline_available, true);
        verify(popupMenuWrapper).setItemVisible(R.id.action_make_offline_unavailable, false);
        verify(popupMenuWrapper).show();
    }

    @Test
    public void showOfflineRemovalOptionWhenOfflineTracksEnabled() {
        PopupMenuWrapper popupMenuWrapper = mock(PopupMenuWrapper.class);

        when(button.getContext()).thenReturn(context);
        when(popupMenuWrapperFactory.build(context, button)).thenReturn(popupMenuWrapper);
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        likesMenuPresenter.show(button, fragmentManager);

        verify(popupMenuWrapper).setItemVisible(R.id.action_make_offline_available, false);
        verify(popupMenuWrapper).setItemVisible(R.id.action_make_offline_unavailable, true);
        verify(popupMenuWrapper).show();
    }

    private void showMenu(boolean offlineFeatureEnabled, boolean offlineLikesEnabled) {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(offlineFeatureEnabled);
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(offlineLikesEnabled));
        likesMenuPresenter.show(button, fragmentManager);
    }

    private MenuItem mockMenuItem(int menuItemId) {
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(menuItemId);
        return menuItem;
    }
}
