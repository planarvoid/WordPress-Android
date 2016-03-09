package com.soundcloud.android.main;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.CustomFontTabLayout;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;

public class MainTabsPresenter extends DefaultActivityLightCycle<AppCompatActivity>
        implements ViewPager.OnPageChangeListener {

    private final BaseLayoutHelper layoutHelper;
    private final MainPagerAdapter.Factory pagerAdapterFactory;
    private final EventBus eventBus;
    private final Navigator navigator;

    private NavigationModel navigationModel;

    private AppCompatActivity activity;
    private MainPagerAdapter pagerAdapter;
    private ViewPager pager;
    private TabLayout tabBar;

    @Inject
    MainTabsPresenter(NavigationModel navigationModel, BaseLayoutHelper layoutHelper,
                      MainPagerAdapter.Factory pagerAdapterFactory, EventBus eventBus, Navigator navigator) {
        this.navigationModel = navigationModel;
        this.layoutHelper = layoutHelper;
        this.pagerAdapterFactory = pagerAdapterFactory;
        this.eventBus = eventBus;
        this.navigator = navigator;
    }

    public void setBaseLayout(AppCompatActivity activity) {
        layoutHelper.setBaseTabsLayout(activity);
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        pagerAdapter = pagerAdapterFactory.create(activity.getSupportFragmentManager());
        setupViews(activity);
        if (bundle == null) {
            setTabFromIntent(activity.getIntent());
        }
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        pager.addOnPageChangeListener(this);
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        pager.removeOnPageChangeListener(this);
    }

    @Override
    public void onNewIntent(AppCompatActivity activity, Intent intent) {
        setTabFromIntent(intent);
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
            case Actions.SEARCH:
                selectItem(Screen.SEARCH_MAIN);
                openSearchScreen(intent);
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

    private void setupViews(AppCompatActivity activity) {
        pager = (ViewPager) activity.findViewById(R.id.pager);
        pager.setPageMargin(ViewUtils.dpToPx(activity, 10));
        pager.setPageMarginDrawable(R.color.page_background);
        tabBar = createTabs();
        bindPagerToTabs();
    }

    private void bindPagerToTabs() {
        pager.setAdapter(pagerAdapter);
        tabBar.setOnTabSelectedListener(tabSelectedListener(pager, pagerAdapter));
        pager.addOnPageChangeListener(pageChangeListenerFor(tabBar));
        setTabIcons(pagerAdapter, tabBar, pager.getCurrentItem());
    }

    @NonNull
    private TabLayout createTabs() {
        TabLayout tabBar = new CustomFontTabLayout(activity);
        tabBar.setTabGravity(TabLayout.GRAVITY_FILL);
        tabBar.setTabMode(TabLayout.MODE_FIXED);
        tabBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addToToolbar(tabBar);
        return tabBar;
    }

    private void addToToolbar(TabLayout tabBar) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_id);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.addView(tabBar);
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

    private View createTabViewFor(@DrawableRes NavigationModel.Target target) {
        ImageView view = new ImageView(activity);
        view.setImageResource(target.getIcon());
        view.setContentDescription(activity.getString(target.getName()));
        int tabPadding = (int) view.getContext().getResources().getDimension(R.dimen.fixed_tab_padding);
        view.setPadding(tabPadding, 0, tabPadding, 0);
        return view;
    }

    private void openSearchScreen(final Intent intent) {
        if (intent.hasExtra(Navigator.EXTRA_SEARCH_INTENT)) {
            navigator.openSearch(activity, intent.<Intent>getParcelableExtra(Navigator.EXTRA_SEARCH_INTENT));
        } else {
            navigator.openSearch(activity);
        }
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

    @Override
    public void onPageSelected(int position) {
        trackScreen();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    public void trackScreen() {
        final Screen currentScreen = getScreen();
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(currentScreen));
    }

    Screen getScreen() {
        return navigationModel.getItem(pager.getCurrentItem()).getScreen();
    }
}
