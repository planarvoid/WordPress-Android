package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class TabbedChartPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private final Resources resources;

    private TabbedChartAdapter adapter;
    private ViewPager pager;

    @Inject
    TabbedChartPresenter(Resources resources) {
        this.resources = resources;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        final ChartType chartType = (ChartType) activity.getIntent().getSerializableExtra(ChartFragment.EXTRA_TYPE);
        final Urn chartGenreUrn = activity.getIntent().getParcelableExtra(ChartFragment.EXTRA_GENRE_URN);
        adapter = new TabbedChartAdapter(activity.getSupportFragmentManager(), resources, chartGenreUrn);
        pager = (ViewPager) activity.findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        TabLayout tabIndicator = (TabLayout) activity.findViewById(R.id.tab_indicator);
        tabIndicator.setupWithViewPager(pager);

        if (bundle == null) {
            pager.setCurrentItem(chartType.ordinal());
        }
    }
}
