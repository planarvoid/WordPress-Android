package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.ProfileHeaderPresenter.ProfileHeaderPresenterFactory;
import static com.soundcloud.android.profile.ProfilePagerRefreshHelper.ProfilePagerRefreshHelperFactory;

import com.soundcloud.android.R;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.android.view.SlidingTabLayout;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import javax.inject.Inject;

class ProfilePresenter extends DefaultLightCycleActivity<AppCompatActivity> {

    private final ProfileHeaderPresenterFactory profileHeaderPresenterFactory;
    private final ProfilePagerRefreshHelperFactory profilePagerRefreshHelperFactory;
    private ViewPager pager;
    private ProfilePagerRefreshHelper refreshHelper;

    @Inject
    public ProfilePresenter(ProfilePagerRefreshHelperFactory profilePagerRefreshHelperFactory,
                            ProfileHeaderPresenterFactory profileHeaderPresenterFactory) {
        this.profilePagerRefreshHelperFactory = profilePagerRefreshHelperFactory;
        this.profileHeaderPresenterFactory = profileHeaderPresenterFactory;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);

        final MultiSwipeRefreshLayout refreshLayout = (MultiSwipeRefreshLayout) activity.findViewById(R.id.str_layout);
        refreshHelper = profilePagerRefreshHelperFactory.create(refreshLayout);

        final View headerView = activity.findViewById(R.id.profile_header);
        final ProfileHeaderPresenter headerPresenter = profileHeaderPresenterFactory.create(headerView);
        final ProfilePagerAdapter adapter = new ProfilePagerAdapter(activity.getSupportFragmentManager(), headerPresenter, refreshHelper);

        pager = (ViewPager) activity.findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(activity.getResources().getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        SlidingTabLayout tabIndicator = (SlidingTabLayout) activity.findViewById(R.id.sliding_tabs);
        tabIndicator.setViewPager(pager);
        tabIndicator.setOnPageChangeListener(new PageChangeListener());
        refreshHelper.setRefreshablePage(0);
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        pager = null;
        refreshHelper = null;
        super.onDestroy(activity);
    }

    private final class PageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // no-op
        }

        @Override
        public void onPageSelected(int position) {
            refreshHelper.setRefreshablePage(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // no-op
        }
    }
}