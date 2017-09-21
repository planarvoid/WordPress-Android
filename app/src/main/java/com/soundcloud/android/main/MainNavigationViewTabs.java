package com.soundcloud.android.main;

import butterknife.BindView;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

public class MainNavigationViewTabs extends MainNavigationView {

    @BindView(R.id.pager) ViewPager pager;
    @BindView(R.id.tab_layout) TabLayout tabBar;
    private final ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {

        @Override
        public void onPageSelected(int i) {
            enterScreenDispatcher.onPageSelected(i);
        }

    };

    @Inject
    public MainNavigationViewTabs(EnterScreenDispatcher enterScreenDispatcher,
                                  NavigationModel navigationModel,
                                  EventTracker eventTracker,
                                  IntroductoryOverlayPresenter introductoryOverlayPresenter) {
        super(enterScreenDispatcher, navigationModel, eventTracker, introductoryOverlayPresenter);
    }

    @Override
    protected void onSetupView(RootActivity activity, MainPagerAdapter pagerAdapter) {
        pager.setPageMargin(ViewUtils.dpToPx(activity, 10));
        pager.setPageMarginDrawable(R.color.page_background);
        pager.setAdapter(pagerAdapter);
        bindPagerToTabs(pagerAdapter);
    }

    @Override
    protected void onSelectItem(int position) {
        tabBar.getTabAt(position).select();
    }

    @Override
    protected Optional<View> getMoreTabCustomView() {
        int morePosition = navigationModel.getPosition(Screen.MORE);
        return morePosition == NavigationModel.NOT_FOUND
               ? Optional.absent()
               : Optional.of(tabBar.getTabAt(morePosition).getCustomView());
    }

    @Override
    public void onPause(RootActivity activity) {
        pager.removeOnPageChangeListener(onPageChangeListener);
        super.onPause(activity);
    }

    @Override
    public void onResume(RootActivity activity) {
        super.onResume(activity);
        pager.addOnPageChangeListener(onPageChangeListener);
    }

    @Override
    protected NavigationModel.Target currentTargetItem() {
        return navigationModel.getItem(pager.getCurrentItem());
    }

    private void bindPagerToTabs(MainPagerAdapter pagerAdapter) {
        tabBar.addOnTabSelectedListener(tabSelectedListener(pager, pagerAdapter));
        pager.addOnPageChangeListener(pageChangeListenerFor(tabBar, pagerAdapter));
        setTabIcons(pagerAdapter, tabBar, pager.getCurrentItem());
    }

    private void setTabIcons(MainPagerAdapter pagerAdapter, TabLayout tabBar, int currentItem) {
        int tabCount = pagerAdapter.getCount();
        tabBar.removeAllTabs();
        for (int pageIndex = 0; pageIndex < tabCount; pageIndex++) {
            TabLayout.Tab tab = tabBar.newTab();
            tab.setCustomView(createTabViewFor(tabBar.getContext(), navigationModel.getItem(pageIndex)));
            tabBar.addTab(tab, pageIndex, pageIndex == currentItem);
        }
    }

    private View createTabViewFor(Context context, NavigationModel.Target target) {
        ImageView view = new ImageView(context);
        view.setImageResource(target.getIcon());
        view.setContentDescription(context.getString(target.getName()));
        return view;
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

    private static ViewPager.OnPageChangeListener pageChangeListenerFor(final TabLayout tabBar, final MainPagerAdapter pagerAdapter) {
        return new TabLayout.TabLayoutOnPageChangeListener(tabBar) {
            /*
            * Workaround for tab re-selection callback issue:
            * https://code.google.com/p/android/issues/detail?id=177189#c16
            */
            @Override
            public void onPageSelected(int position) {
                if (tabBar.getSelectedTabPosition() != position) {
                    super.onPageSelected(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    pagerAdapter.setCurrentFragmentFocused();
                }
            }
        };
    }
}
