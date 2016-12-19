package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.discovery.charts.ChartTracksFragment.EXTRA_CATEGORY;
import static com.soundcloud.android.discovery.charts.ChartTracksFragment.EXTRA_GENRE_URN;
import static com.soundcloud.android.discovery.charts.ChartTracksFragment.EXTRA_TYPE;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.main.EnterScreenDispatcher;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;

import javax.inject.Inject;

class ChartPresenter extends ActivityLightCycleDispatcher<RootActivity> implements EnterScreenDispatcher.Listener {

    private final Resources resources;
    private final ChartsTracker chartsTracker;
    private ViewPager pager;
    private ChartPagerAdapter adapter;
    @LightCycle EnterScreenDispatcher enterScreenDispatcher;

    @Inject
    ChartPresenter(Resources resources, ChartsTracker chartsTracker, EnterScreenDispatcher enterScreenDispatcher) {
        this.resources = resources;
        this.chartsTracker = chartsTracker;
        this.enterScreenDispatcher = enterScreenDispatcher;
        this.enterScreenDispatcher.setListener(this);
    }

    @Override
    public void onCreate(RootActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        chartsTracker.clearTracker();
        final Intent intent = activity.getIntent();
        final ChartType chartType = (ChartType) intent.getSerializableExtra(EXTRA_TYPE);
        final ChartCategory chartCategory = (ChartCategory) intent.getSerializableExtra(EXTRA_CATEGORY);
        final Urn chartGenreUrn = intent.getParcelableExtra(EXTRA_GENRE_URN);
        adapter = new ChartPagerAdapter(activity.getSupportFragmentManager(), resources, chartGenreUrn);
        pager = (ViewPager) activity.findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width));
        if (bundle == null) {
            //If we first select the item and then add listener, we never trigger page selected event on create
            pager.setCurrentItem(chartType.ordinal());
        }

        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                final ChartType type = adapter.getType(position);
                chartsTracker.chartPageSelected(chartGenreUrn, chartCategory, type);
            }
        });

        final TabLayout tabLayout = (TabLayout) activity.findViewById(R.id.tab_indicator);
        tabLayout.setupWithViewPager(pager);
    }

    @Override
    public void onEnterScreen(RootActivity activity) {
        if (activity instanceof ChartActivity) {
            final Intent intent = activity.getIntent();
            final Urn chartGenreUrn = intent.getParcelableExtra(EXTRA_GENRE_URN);
            final ChartCategory chartCategory = (ChartCategory) intent.getSerializableExtra(EXTRA_CATEGORY);
            final ChartType type = adapter.getType(pager.getCurrentItem());
            chartsTracker.chartPageSelected(chartGenreUrn, chartCategory, type);
        }
    }
}
