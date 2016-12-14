package com.soundcloud.android.main;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycles;
import rx.Subscription;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

public class MainTabsPresenter extends ActivityLightCycleDispatcher<RootActivity>
        implements EnterScreenDispatcher.Listener {

    private final BaseLayoutHelper layoutHelper;
    private final MainPagerAdapter.Factory pagerAdapterFactory;
    private final Navigator navigator;
    private final EventTracker eventTracker;
    private final NavigationModel navigationModel;
    private final FeatureOperations featureOperations;

    private RootActivity activity;
    private MainPagerAdapter pagerAdapter;

    @Bind(R.id.pager) ViewPager pager;
    @Bind(R.id.tab_layout) TabLayout tabBar;
    @Bind(R.id.toolbar_id) Toolbar toolBar;
    @Bind(R.id.appbar) AppBarLayout appBarLayout;
    @Bind(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbarLayout;

    private Subscription subscription = RxUtils.invalidSubscription();

    @LightCycle final EnterScreenDispatcher enterScreenDispatcher;

    @Inject
    MainTabsPresenter(NavigationModel navigationModel,
                      BaseLayoutHelper layoutHelper,
                      MainPagerAdapter.Factory pagerAdapterFactory,
                      Navigator navigator,
                      EventTracker eventTracker,
                      EnterScreenDispatcher enterScreenDispatcher,
                      FeatureOperations featureOperations) {
        this.navigationModel = navigationModel;
        this.layoutHelper = layoutHelper;
        this.pagerAdapterFactory = pagerAdapterFactory;
        this.navigator = navigator;
        this.eventTracker = eventTracker;
        this.enterScreenDispatcher = enterScreenDispatcher;
        this.featureOperations = featureOperations;
        enterScreenDispatcher.setListener(this);
        LightCycles.bind(this);
    }

    public void setBaseLayout(RootActivity activity) {
        layoutHelper.setBaseTabsLayout(activity);
    }

    @Override
    public void onCreate(RootActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        this.activity = activity;
        pagerAdapter = pagerAdapterFactory.create(activity);

        ButterKnife.bind(this, activity);
        setupViews(activity);

        if (bundle == null) {
            setTabFromIntent(activity.getIntent());
        }
        startDevelopmentMenuStream();
    }

    @Override
    public void onResume(RootActivity activity) {
        super.onResume(activity);

        pager.addOnPageChangeListener(enterScreenDispatcher);
        toolBar.setTitle(pagerAdapter.getPageTitle(pager.getCurrentItem()));
    }

    @Override
    public void onPause(RootActivity activity) {
        pager.removeOnPageChangeListener(enterScreenDispatcher);

        super.onPause(activity);
    }

    @Override
    public void onDestroy(RootActivity activity) {
        subscription.unsubscribe();

        super.onDestroy(activity);
    }

    @Override
    public void onNewIntent(RootActivity activity, Intent intent) {
        super.onNewIntent(activity, intent);

        setTabFromIntent(intent);
    }

    @Override
    public void onEnterScreen(RootActivity activity) {
        toolBar.setTitle(pagerAdapter.getPageTitle(pager.getCurrentItem()));
        eventTracker.trackScreen(ScreenEvent.create(getScreen()), activity.getReferringEvent());
    }

    void hideToolbar() {
        if (collapsingToolbarLayout != null) {
            collapsingToolbarLayout.setVisibility(View.GONE);
        }
    }

    void showToolbar() {
        if (collapsingToolbarLayout != null && appBarLayout != null) {
            appBarLayout.setExpanded(true);
            collapsingToolbarLayout.setVisibility(View.VISIBLE);
        }
    }

    private void startDevelopmentMenuStream() {
        subscription = featureOperations.developmentMenuEnabled()
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
            selectItem(Screen.STREAM);
        } else if (NavigationIntentHelper.shouldGoToSearch(data)) {
            selectItem(Screen.SEARCH_MAIN);
        }
    }

    private void resolveIntentFromAction(@NonNull final Intent intent) {
        switch (intent.getAction()) {
            case Actions.STREAM:
                selectItem(Screen.STREAM);
                break;
            case Actions.COLLECTION:
                selectItem(Screen.COLLECTIONS);
                break;
            case Actions.DISCOVERY:
                selectItem(Screen.SEARCH_MAIN);
                break;
            case Actions.SEARCH:
                selectItem(Screen.SEARCH_MAIN);
                openSearchScreen(intent);
                break;
            case Actions.MORE:
                selectItem(Screen.MORE);
                break;
            default:
                break;
        }
    }

    private void selectItem(Screen screen) {
        int position = navigationModel.getPosition(screen);
        if (position != NavigationModel.NOT_FOUND) {
            tabBar.getTabAt(position).select();
        }
    }

    private void setupViews(RootActivity activity) {
        pager.setPageMargin(ViewUtils.dpToPx(activity, 10));
        pager.setPageMarginDrawable(R.color.page_background);

        activity.setSupportActionBar(toolBar);

        bindPagerToTabs();
    }

    private void bindPagerToTabs() {
        pager.setAdapter(pagerAdapter);
        tabBar.setOnTabSelectedListener(tabSelectedListener(pager, pagerAdapter));
        pager.addOnPageChangeListener(pageChangeListenerFor(tabBar));
        setTabIcons(pagerAdapter, tabBar, pager.getCurrentItem());
    }

    private void setTabIcons(MainPagerAdapter pagerAdapter, TabLayout tabBar, int currentItem) {
        int tabCount = pagerAdapter.getCount();
        tabBar.removeAllTabs();
        for (int pageIndex = 0; pageIndex < tabCount; pageIndex++) {
            TabLayout.Tab tab = tabBar.newTab();
            tab.setCustomView(createTabViewFor(navigationModel.getItem(pageIndex)));
            tabBar.addTab(tab, pageIndex, pageIndex == currentItem);
        }
    }

    private View createTabViewFor(NavigationModel.Target target) {
        ImageView view = new ImageView(activity);
        view.setImageResource(target.getIcon());
        view.setContentDescription(activity.getString(target.getName()));
        return view;
    }

    private void openSearchScreen(final Intent intent) {
        if (intent.hasExtra(Navigator.EXTRA_SEARCH_INTENT)) {
            navigator.openSearch(activity, intent.<Intent>getParcelableExtra(Navigator.EXTRA_SEARCH_INTENT));
        } else {
            navigator.openSearch(activity);
        }
    }

    private NavigationModel.Target currentTargetItem() {
        return navigationModel.getItem(pager.getCurrentItem());
    }

    Screen getScreen() {
        return currentTargetItem().getScreen();
    }

    private static TabLayout.ViewPagerOnTabSelectedListener tabSelectedListener(final ViewPager pager,
                                                                                final MainPagerAdapter pagerAdapter) {
        return new TabLayout.ViewPagerOnTabSelectedListener(pager) {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                pagerAdapter.resetScroll(tab.getPosition());
            }
        };
    }

    private static ViewPager.OnPageChangeListener pageChangeListenerFor(final TabLayout tabBar) {
        /*
         * Workaround for tab re-selection callback issue:
         * https://code.google.com/p/android/issues/detail?id=177189#c16
         */
        return new TabLayout.TabLayoutOnPageChangeListener(tabBar) {
            @Override
            public void onPageSelected(int position) {
                if (tabBar.getSelectedTabPosition() != position) {
                    super.onPageSelected(position);
                }
            }
        };
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
