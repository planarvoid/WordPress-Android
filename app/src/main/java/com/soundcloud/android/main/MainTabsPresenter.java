package com.soundcloud.android.main;

import static com.soundcloud.android.deeplinks.ShortcutController.Shortcut.PLAY_LIKES;
import static com.soundcloud.android.deeplinks.ShortcutController.Shortcut.SEARCH;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.deeplinks.ShortcutController;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import rx.Subscription;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import javax.inject.Inject;

public class MainTabsPresenter extends ActivityLightCycleDispatcher<RootActivity> {

    private final BaseLayoutHelper layoutHelper;
    private final MainPagerAdapter.Factory pagerAdapterFactory;
    private final Navigator navigator;
    private final ShortcutController shortcutController;
    private final FeatureOperations featureOperations;

    private RootActivity activity;

    private Subscription subscriber = RxUtils.invalidSubscription();

    @LightCycle final MainTabsView mainTabsView;

    @Inject
    MainTabsPresenter(BaseLayoutHelper layoutHelper,
                      MainPagerAdapter.Factory pagerAdapterFactory,
                      Navigator navigator,
                      ShortcutController shortcutController,
                      FeatureOperations featureOperations,
                      MainTabsView mainTabsView) {
        this.layoutHelper = layoutHelper;
        this.pagerAdapterFactory = pagerAdapterFactory;
        this.navigator = navigator;
        this.shortcutController = shortcutController;
        this.featureOperations = featureOperations;
        this.mainTabsView = mainTabsView;
    }

    public void setBaseLayout(RootActivity activity) {
        layoutHelper.setBaseTabsLayout(activity);
    }

    @Override
    public void onCreate(RootActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        this.activity = activity;

        mainTabsView.setupViews(activity, pagerAdapterFactory.create(activity));

        if (bundle == null) {
            setTabFromIntent(activity.getIntent());
        }
        startDevelopmentMenuStream();
    }

    @Override
    public void onDestroy(RootActivity activity) {
        subscriber.unsubscribe();

        super.onDestroy(activity);
    }

    @Override
    public void onNewIntent(RootActivity activity, Intent intent) {
        super.onNewIntent(activity, intent);

        setTabFromIntent(intent);
    }

    void hideToolbar() {
        mainTabsView.hideToolbar();
    }

    void showToolbar() {
        mainTabsView.showToolbar();
    }

    private void startDevelopmentMenuStream() {
        subscriber = featureOperations.developmentMenuEnabled()
                                      .startWith(featureOperations.isDevelopmentMenuEnabled())
                                      .subscribe(new UpdateDevelopmentMenuAction());
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
            mainTabsView.selectItem(Screen.SEARCH_MAIN);
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
                mainTabsView.selectItem(Screen.SEARCH_MAIN);
                break;
            case Actions.SEARCH:
                mainTabsView.selectItem(Screen.SEARCH_MAIN);
                openSearchScreen(intent);
                break;
            case Actions.MORE:
                mainTabsView.selectItem(Screen.MORE);
                break;
            case Actions.SHORTCUT_SEARCH:
                shortcutController.reportUsage(SEARCH);
                mainTabsView.selectItem(Screen.SEARCH_MAIN);
                navigator.openSearchFromShortcut(activity);
                break;
            case Actions.SHORTCUT_PLAY_LIKES:
                shortcutController.reportUsage(PLAY_LIKES);
                mainTabsView.selectItem(Screen.COLLECTIONS);
                navigator.openTrackLikesFromShortcut(activity, intent);
                break;
            default:
                break;
        }
    }

    private void openSearchScreen(final Intent intent) {
        if (intent.hasExtra(Navigator.EXTRA_SEARCH_INTENT)) {
            navigator.openSearch(activity, intent.getParcelableExtra(Navigator.EXTRA_SEARCH_INTENT));
        } else {
            navigator.openSearch(activity);
        }
    }

    private class UpdateDevelopmentMenuAction extends DefaultSubscriber<Boolean> {
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
