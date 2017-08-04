package com.soundcloud.android.main;

import static com.soundcloud.android.deeplinks.ShortcutController.Shortcut.PLAY_LIKES;
import static com.soundcloud.android.deeplinks.ShortcutController.Shortcut.SEARCH;

import com.soundcloud.android.Actions;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment;
import com.soundcloud.android.deeplinks.ShortcutController;
import com.soundcloud.android.discovery.DiscoveryConfiguration;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import javax.inject.Inject;

public class MainTabsPresenter extends ActivityLightCycleDispatcher<RootActivity> {

    private final BaseLayoutHelper layoutHelper;
    private final MainPagerAdapter.Factory pagerAdapterFactory;
    private final NavigationExecutor navigationExecutor;
    private final ShortcutController shortcutController;
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final GoOnboardingTooltipExperiment goOnboardingTooltipExperiment;
    private final DiscoveryConfiguration discoveryConfiguration;

    private RootActivity activity;

    private Disposable disposable = Disposables.empty();

    @LightCycle final MainTabsView mainTabsView;

    @Inject
    MainTabsPresenter(BaseLayoutHelper layoutHelper,
                      MainPagerAdapter.Factory pagerAdapterFactory,
                      NavigationExecutor navigationExecutor,
                      ShortcutController shortcutController,
                      FeatureOperations featureOperations,
                      OfflineContentOperations offlineContentOperations,
                      GoOnboardingTooltipExperiment goOnboardingTooltipExperiment,
                      DiscoveryConfiguration discoveryConfiguration,
                      MainTabsView mainTabsView) {
        this.layoutHelper = layoutHelper;
        this.pagerAdapterFactory = pagerAdapterFactory;
        this.navigationExecutor = navigationExecutor;
        this.shortcutController = shortcutController;
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.goOnboardingTooltipExperiment = goOnboardingTooltipExperiment;
        this.discoveryConfiguration = discoveryConfiguration;
        this.mainTabsView = mainTabsView;
    }

    public void setBaseLayout(RootActivity activity) {
        layoutHelper.setBaseTabsLayout(activity);
    }

    @Override
    public void onCreate(RootActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        this.activity = activity;

        mainTabsView.setupViews(pagerAdapterFactory.create(activity));

        if (bundle == null) {
            setTabFromIntent(activity.getIntent());
        }
        startDevelopmentMenuStream();
    }

    @Override
    public void onResume(RootActivity host) {
        super.onResume(host);
        if (offlineContentOperations.hasOfflineContent() && goOnboardingTooltipExperiment.isEnabled()) {
            mainTabsView.showOfflineSettingsIntroductoryOverlay();
        }
    }

    @Override
    public void onDestroy(RootActivity activity) {
        disposable.dispose();

        super.onDestroy(activity);
    }

    @Override
    public void onNewIntent(RootActivity activity, Intent intent) {
        super.onNewIntent(activity, intent);

        setTabFromIntent(intent);
    }

    public Observable<Long> enterScreenTimestamp() {
        return mainTabsView.enterScreenDispatcher.enterScreenTimestamp();
    }

    void hideToolbar() {
        mainTabsView.hideToolbar();
    }

    void showToolbar() {
        mainTabsView.showToolbar();
    }

    private void startDevelopmentMenuStream() {
        disposable = featureOperations.developmentMenuEnabled()
                           .startWith(featureOperations.isDevelopmentMenuEnabled())
                           .subscribeWith(new UpdateDevelopmentMenuAction());
    }

    private void setTabFromIntent(Intent intent) {
        final Uri data = intent.getData();
        final String action = intent.getAction();
        if (data != null) {
            resolveData(data);
        } else if (Strings.isNotBlank(action)) {
            resolveIntentFromAction(intent);
        }
    }

    private void resolveData(@NonNull Uri data) {
        if (NavigationIntentHelper.shouldGoToStream(data)) {
            mainTabsView.selectItem(Screen.STREAM);
        } else if (NavigationIntentHelper.shouldGoToSearch(data)) {
            selectDiscovery();
        }
    }

    private void resolveIntentFromAction(@NonNull final Intent intent) {
        switch (intent.getAction()) {
            case Actions.STREAM:
                mainTabsView.selectItem(Screen.STREAM);
                break;
            case Actions.COLLECTION:
                mainTabsView.selectItem(Screen.COLLECTIONS);
                break;
            case Actions.DISCOVERY:
                selectDiscovery();
                break;
            case Actions.SEARCH:
                selectDiscovery();
                navigationExecutor.openSearch(activity, intent);
                break;
            case Actions.MORE:
                mainTabsView.selectItem(Screen.MORE);
                break;
            case Actions.SHORTCUT_SEARCH:
                shortcutController.reportUsage(SEARCH);
                selectDiscovery();
                navigationExecutor.openSearchFromShortcut(activity);
                break;
            case Actions.SHORTCUT_PLAY_LIKES:
                shortcutController.reportUsage(PLAY_LIKES);
                mainTabsView.selectItem(Screen.COLLECTIONS);
                navigationExecutor.openTrackLikesFromShortcut(activity, intent);
                break;
            default:
                break;
        }
    }

    private void selectDiscovery() {
        if (discoveryConfiguration.shouldShowDiscoverBackendContent()) {
            mainTabsView.selectItem(Screen.DISCOVER);
        } else {
            mainTabsView.selectItem(Screen.SEARCH_MAIN);
        }
    }

    private class UpdateDevelopmentMenuAction extends DefaultObserver<Boolean> {
        @Override
        public void onNext(Boolean developmentModeEnabled) {
            if (developmentModeEnabled) {
                BaseLayoutHelper.addDevelopmentDrawer(activity);
            } else {
                BaseLayoutHelper.removeDevelopmentDrawer(activity);
            }
        }
    }
}
