package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class TabbedGenresPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private final Resources resources;

    private TabbedGenresAdapter adapter;
    private ViewPager pager;

    @Inject
    TabbedGenresPresenter(Resources resources) {
        this.resources = resources;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        adapter = new TabbedGenresAdapter(activity.getSupportFragmentManager(), resources);
        pager = (ViewPager) activity.findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        TabLayout tabIndicator = (TabLayout) activity.findViewById(R.id.tab_indicator);
        tabIndicator.setupWithViewPager(pager);
    }
}
