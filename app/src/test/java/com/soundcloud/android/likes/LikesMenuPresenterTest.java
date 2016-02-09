package com.soundcloud.android.likes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflineLikesDialog;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;

import javax.inject.Provider;

public class LikesMenuPresenterTest extends AndroidUnitTest {

    private LikesMenuPresenter likesMenuPresenter;
    private TestEventBus eventBus;

    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private OfflineLikesDialog offlineLikesDialog;
    @Mock private Navigator navigator;
    @Mock private FragmentManager fragmentManager;
    @Mock private FragmentActivity context;
    @Mock private MenuItem menuItem;
    @Mock private ScreenProvider screenProvider;
    @Mock private Menu menu;

    private MenuItem makeOfflineAvailableItem;
    private MenuItem makeOfflineUnavailableItem;

    @Captor private ArgumentCaptor<PopupMenuWrapper.PopupMenuWrapperListener> listenerCaptor;

    @Before
    public void setUp() {
        Provider<OfflineLikesDialog> syncLikesDialogProvider = new Provider<OfflineLikesDialog>() {
            @Override
            public OfflineLikesDialog get() {
                return offlineLikesDialog;
            }
        };
        eventBus = new TestEventBus();

        makeOfflineAvailableItem = mockMenuItem(R.id.action_make_offline_available);
        makeOfflineUnavailableItem = mockMenuItem(R.id.action_make_offline_unavailable);

        when(featureOperations.isOfflineContentOrUpsellEnabled()).thenReturn(true);
        when(menu.findItem(R.id.action_make_offline_available)).thenReturn(makeOfflineAvailableItem);
        when(menu.findItem(R.id.action_make_offline_unavailable)).thenReturn(makeOfflineUnavailableItem);
        when(screenProvider.getLastScreenTag()).thenReturn("page_name");
        likesMenuPresenter = new LikesMenuPresenter(
                featureOperations,
                offlineContentOperations,
                syncLikesDialogProvider,
                screenProvider,
                navigator,
                eventBus);
    }

    @Test
    public void showSyncLikesDialogWhenClickOnMenuAndOfflineContentIsEnabled() {
        showMenu(true, false);

        likesMenuPresenter.onOptionsItemSelected(makeOfflineAvailableItem, context, fragmentManager);

        verify(offlineLikesDialog).show(any(FragmentManager.class));
    }

    @Test
    public void showDisableLikesSyncingWhenClickOnMakeOfflineUnavailableMenu() {
        when(offlineContentOperations.disableOfflineLikedTracks()).thenReturn(Observable.<Void>just(null));
        showMenu(true, true);

        likesMenuPresenter.onOptionsItemSelected(makeOfflineUnavailableItem, context, fragmentManager);

        verify(offlineContentOperations).disableOfflineLikedTracks();
        verifyZeroInteractions(offlineLikesDialog);
    }

    @Test
    public void showSubscribeActivityWhenLikesSyncEnabledAndOfflineContentDisabled() {
        showMenu(false, false);

        likesMenuPresenter.onOptionsItemSelected(makeOfflineAvailableItem, context, fragmentManager);

        verify(navigator).openUpgrade(context);
        verifyZeroInteractions(offlineLikesDialog);
    }

    @Test
    public void showOfflineDownloadOptionWhenOfflineTracksDisabled() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(false));

        likesMenuPresenter.onPrepareOptionsMenu(menu);

        verify(makeOfflineAvailableItem).setVisible(true);
        verify(makeOfflineUnavailableItem).setVisible(false);
    }

    @Test
    public void showOfflineRemovalOptionWhenOfflineTracksEnabled() {
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(true));

        likesMenuPresenter.onPrepareOptionsMenu(menu);

        verify(makeOfflineAvailableItem).setVisible(false);
        verify(makeOfflineUnavailableItem).setVisible(true);
    }

    @Test
    public void sendsTrackingEventWhenRemovingOfflineLikes() {
        when(offlineContentOperations.disableOfflineLikedTracks()).thenReturn(Observable.<Void>empty());
        showMenu(true, false);

        likesMenuPresenter.onOptionsItemSelected(makeOfflineUnavailableItem, context, fragmentManager);

        OfflineInteractionEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING, OfflineInteractionEvent.class);
        assertThat(trackingEvent.getKind()).isEqualTo(OfflineInteractionEvent.KIND_OFFLINE_LIKES_REMOVE);
        assertThat(trackingEvent.getAttributes()
                .containsValue("page_name")).isTrue();
    }

    private void showMenu(boolean offlineFeatureEnabled, boolean offlineLikesEnabled) {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(offlineFeatureEnabled);
        when(offlineContentOperations.isOfflineLikedTracksEnabled()).thenReturn(Observable.just(offlineLikesEnabled));
        likesMenuPresenter.onPrepareOptionsMenu(menu);
    }

    private MenuItem mockMenuItem(int menuItemId) {
        MenuItem menuItem = mock(MenuItem.class);
        when(menuItem.getItemId()).thenReturn(menuItemId);
        return menuItem;
    }
}
