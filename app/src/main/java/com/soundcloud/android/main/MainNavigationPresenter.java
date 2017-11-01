package com.soundcloud.android.main;

import static com.soundcloud.android.deeplinks.ShortcutController.Shortcut.PLAY_LIKES;
import static com.soundcloud.android.deeplinks.ShortcutController.Shortcut.SEARCH;

import com.soundcloud.android.Actions;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment;
import com.soundcloud.android.deeplinks.ShortcutController;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.collections.Pair;
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

public class MainNavigationPresenter extends ActivityLightCycleDispatcher<RootActivity> implements SlidingPlayerController.SlideListener {

    private final BaseLayoutHelper layoutHelper;
    private final MainPagerAdapter.Factory pagerAdapterFactory;
    private final NavigationExecutor navigationExecutor;
    private final ShortcutController shortcutController;
    private final FeatureOperations featureOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final GoOnboardingTooltipExperiment goOnboardingTooltipExperiment;

    private RootActivity activity;

    private Disposable disposable = Disposables.empty();

    @LightCycle final MainNavigationView mainNavigationView;

    @Inject
    MainNavigationPresenter(BaseLayoutHelper layoutHelper,
                      MainPagerAdapter.Factory pagerAdapterFactory,
                      NavigationExecutor navigationExecutor,
                      ShortcutController shortcutController,
                      FeatureOperations featureOperations,
                      OfflineContentOperations offlineContentOperations,
                      GoOnboardingTooltipExperiment goOnboardingTooltipExperiment,
                      MainNavigationView mainNavigationView) {
        this.layoutHelper = layoutHelper;
        this.pagerAdapterFactory = pagerAdapterFactory;
        this.navigationExecutor = navigationExecutor;
        this.shortcutController = shortcutController;
        this.featureOperations = featureOperations;
        this.offlineContentOperations = offlineContentOperations;
        this.goOnboardingTooltipExperiment = goOnboardingTooltipExperiment;
        this.mainNavigationView = mainNavigationView;
    }

    public void setBaseLayout(RootActivity activity) {
        layoutHelper.setMainLayout(activity);
    }

    @Override
    public void onCreate(RootActivity activity, Bundle savedInstanceState) {
        super.onCreate(activity, savedInstanceState);
        this.activity = activity;

        mainNavigationView.setupViews(activity, savedInstanceState, pagerAdapterFactory.create(activity));

        if (savedInstanceState == null) {
            setTabFromIntent(activity.getIntent());
        }
        startDevelopmentMenuStream();
    }

    @Override
    public void onResume(RootActivity host) {
        super.onResume(host);
        if (offlineContentOperations.hasOfflineContent() && goOnboardingTooltipExperiment.isEnabled()) {
            mainNavigationView.showOfflineSettingsIntroductoryOverlay();
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
        return mainNavigationView.enterScreenDispatcher.enterScreenTimestamp();
    }

    Observable<Pair<Long, Screen>> pageSelectedTimestamp() {
        return mainNavigationView.enterScreenDispatcher.pageSelectedTimestamp().map(timestamp -> Pair.of(timestamp, mainNavigationView.getScreen()));
    }

    void hideToolbar() {
        mainNavigationView.hideToolbar();
    }

    void showToolbar() {
        mainNavigationView.showToolbar();
    }

    @Override
    public void onPlayerSlide(float slideOffset) {
        mainNavigationView.onPlayerSlide(slideOffset);
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
            mainNavigationView.selectItem(Screen.STREAM);
        } else if (NavigationIntentHelper.shouldGoToSearch(data)) {
            selectDiscovery();
        }
    }

    private void resolveIntentFromAction(@NonNull final Intent intent) {
        switch (intent.getAction()) {
            case Actions.STREAM:
                mainNavigationView.selectItem(Screen.STREAM);
                break;
            case Actions.COLLECTION:
                mainNavigationView.selectItem(Screen.COLLECTIONS);
                break;
            case Actions.DISCOVERY:
                selectDiscovery();
                break;
            case Actions.SEARCH:
                selectDiscovery();
                navigationExecutor.openSearch(activity, intent);
                break;
            case Actions.MORE:
                mainNavigationView.selectItem(Screen.MORE);
                break;
            case Actions.SHORTCUT_SEARCH:
                shortcutController.reportUsage(SEARCH);
                selectDiscovery();
                navigationExecutor.openSearchFromShortcut(activity);
                break;
            case Actions.SHORTCUT_PLAY_LIKES:
                shortcutController.reportUsage(PLAY_LIKES);
                mainNavigationView.selectItem(Screen.COLLECTIONS);
                navigationExecutor.openTrackLikesFromShortcut(activity, intent);
                break;
            default:
                break;
        }
    }

    private void selectDiscovery() {
        mainNavigationView.selectItem(Screen.DISCOVER);
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