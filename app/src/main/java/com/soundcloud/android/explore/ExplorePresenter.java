package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class ExplorePresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private final Resources resources;
    private final ExplorePagerAdapterFactory adapterFactory;
    private final ExplorePagerScreenListener screenListener;

    private ExplorePagerAdapter pagerAdapter;
    private ViewPager pager;

    @Inject
    public ExplorePresenter(Resources resources, ExplorePagerAdapterFactory adapterFactory,
                            ExplorePagerScreenListener screenListener) {
        this.resources = resources;
        this.adapterFactory = adapterFactory;
        this.screenListener = screenListener;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        pager = (ViewPager) activity.findViewById(R.id.pager);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        pagerAdapter = adapterFactory.create(activity.getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);

        TabLayout tabIndicator = (TabLayout) activity.findViewById(R.id.tab_indicator);
        tabIndicator.setupWithViewPager(pager);
        pager.addOnPageChangeListener(screenListener);

        if (bundle == null) {
            pager.setCurrentItem(1);
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        pagerAdapter = null;
        pager = null;
    }

}
