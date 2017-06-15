package com.soundcloud.android.main;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Actions;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment;
import com.soundcloud.android.deeplinks.ShortcutController;
import com.soundcloud.android.deeplinks.ShortcutController.Shortcut;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import io.reactivex.Observable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class MainTabsPresenterTest extends AndroidUnitTest {

    @Mock private BaseLayoutHelper layoutHelper;
    @Mock private MainPagerAdapter.Factory pagerAdapterFactory;
    @Mock private NavigationExecutor navigationExecutor;
    @Mock private ShortcutController shortcutController;
    @Mock private FeatureOperations featureOperations;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private GoOnboardingTooltipExperiment goOnboardingTooltipExperiment;
    @Mock private RootActivity rootActivity;
    @Mock private MainTabsView mainTabsView;

    private MainTabsPresenter mainTabsPresenter;

    @Before
    public void setUp() {
        mainTabsPresenter = new MainTabsPresenter(layoutHelper,
                                                  pagerAdapterFactory,
                                                  navigationExecutor,
                                                  shortcutController,
                                                  featureOperations,
                                                  offlineContentOperations,
                                                  goOnboardingTooltipExperiment,
                                                  mainTabsView);

        when(featureOperations.developmentMenuEnabled()).thenReturn(Observable.just(false));
    }

    @Test
    public void shouldSetupMainTabsView() {
        mainTabsPresenter.onCreate(rootActivity, new Bundle());

        verify(mainTabsView).setupViews(pagerAdapterFactory.create(rootActivity));
    }

    @Test
    public void shouldResolveDataForStream() {
        Uri streamUri = Uri.parse("https://soundcloud.com/stream");
        when(rootActivity.getIntent()).thenReturn(createIntent(null, streamUri));

        mainTabsPresenter.onCreate(rootActivity, null);

        verify(mainTabsView).selectItem(Screen.STREAM);
    }

    @Test
    public void shouldResolveDataForSearch() {
        Uri searchUri = Uri.parse("https://soundcloud.com/search");
        when(rootActivity.getIntent()).thenReturn(createIntent(null, searchUri));

        mainTabsPresenter.onCreate(rootActivity, null);

        verify(mainTabsView).selectItem(Screen.SEARCH_MAIN);
    }

    @Test
    public void shouldResolveIntentFromStreamAction() {
        when(rootActivity.getIntent()).thenReturn(createIntent(Actions.STREAM, null));

        mainTabsPresenter.onCreate(rootActivity, null);

        verify(mainTabsView).selectItem(Screen.STREAM);
    }

    @Test
    public void shouldResolveIntentFromCollectionAction() {
        when(rootActivity.getIntent()).thenReturn(createIntent(Actions.COLLECTION, null));

        mainTabsPresenter.onCreate(rootActivity, null);

        verify(mainTabsView).selectItem(Screen.COLLECTIONS);
    }

    @Test
    public void shouldResolveIntentFromDiscoveryAction() {
        when(rootActivity.getIntent()).thenReturn(createIntent(Actions.DISCOVERY, null));

        mainTabsPresenter.onCreate(rootActivity, null);

        verify(mainTabsView).selectItem(Screen.SEARCH_MAIN);
    }

    @Test
    public void shouldResolveIntentFromSearchAction() {
        final Intent intent = createIntent(Actions.SEARCH, null);
        when(rootActivity.getIntent()).thenReturn(intent);

        mainTabsPresenter.onCreate(rootActivity, null);

        verify(mainTabsView).selectItem(Screen.SEARCH_MAIN);
        verify(navigationExecutor).openSearch(rootActivity, intent);
    }

    @Test
    public void shouldResolveIntentFromMoreAction() {
        when(rootActivity.getIntent()).thenReturn(createIntent(Actions.MORE, null));

        mainTabsPresenter.onCreate(rootActivity, null);

        verify(mainTabsView).selectItem(Screen.MORE);
    }

    @Test
    public void shouldResolveIntentFromSearchShortcut() {
        when(rootActivity.getIntent()).thenReturn(createIntent(Actions.SHORTCUT_SEARCH, null));

        mainTabsPresenter.onCreate(rootActivity, null);

        verify(shortcutController).reportUsage(Shortcut.SEARCH);
        verify(mainTabsView).selectItem(Screen.SEARCH_MAIN);
        verify(navigationExecutor).openSearchFromShortcut(rootActivity);
    }

    @Test
    public void shouldResolveIntentFromPlayLikesShortcut() {
        final Intent intent = createIntent(Actions.SHORTCUT_PLAY_LIKES, null);
        when(rootActivity.getIntent()).thenReturn(intent);

        mainTabsPresenter.onCreate(rootActivity, null);

        verify(shortcutController).reportUsage(Shortcut.PLAY_LIKES);
        verify(mainTabsView).selectItem(Screen.COLLECTIONS);
        verify(navigationExecutor).openTrackLikesFromShortcut(rootActivity, intent);
    }

    @Test
    public void shouldNotShowOfflineSettingsIntroductoryOverlay() {
        when(offlineContentOperations.hasOfflineContent()).thenReturn(false);
        when(goOnboardingTooltipExperiment.isEnabled()).thenReturn(false);

        mainTabsPresenter.onResume(rootActivity);

        verify(mainTabsView, never()).showOfflineSettingsIntroductoryOverlay();
    }

    @Test
    public void shouldNotShowOfflineSettingsIntroductoryOverlayIfNoOfflineContent() {
        when(offlineContentOperations.hasOfflineContent()).thenReturn(false);
        when(goOnboardingTooltipExperiment.isEnabled()).thenReturn(true);

        mainTabsPresenter.onResume(rootActivity);

        verify(mainTabsView, never()).showOfflineSettingsIntroductoryOverlay();
    }

    @Test
    public void shouldNotShowOfflineSettingsIntroductoryOverlayIfExperimentNotEnabled() {
        when(offlineContentOperations.hasOfflineContent()).thenReturn(true);
        when(goOnboardingTooltipExperiment.isEnabled()).thenReturn(false);

        mainTabsPresenter.onResume(rootActivity);

        verify(mainTabsView, never()).showOfflineSettingsIntroductoryOverlay();
    }

    @Test
    public void shouldShowOfflineSettingsIntroductoryOverlayOnResume() {
        when(offlineContentOperations.hasOfflineContent()).thenReturn(true);
        when(goOnboardingTooltipExperiment.isEnabled()).thenReturn(true);

        mainTabsPresenter.onResume(rootActivity);

        verify(mainTabsView).showOfflineSettingsIntroductoryOverlay();
    }

    private Intent createIntent(String action, Uri uri) {
        return new Intent(action, uri);
    }
}
