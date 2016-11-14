package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class AllGenresPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private final Resources resources;
    private final ChartsTracker chartsTracker;
    private ViewPager pager;
    private GenresPagerAdapter adapter;

    @Inject
    AllGenresPresenter(Resources resources, ChartsTracker chartsTracker) {
        this.resources = resources;
        this.chartsTracker = chartsTracker;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        adapter = new GenresPagerAdapter(activity.getSupportFragmentManager(), resources);
        pager = (ViewPager) activity.findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width));
        pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                chartsTracker.genrePageSelected(getScreen(position));
            }
        });

        TabLayout tabIndicator = (TabLayout) activity.findViewById(R.id.tab_indicator);
        tabIndicator.setupWithViewPager(pager);
    }

    public Screen getScreen() {
        return getScreen(pager.getCurrentItem());
    }

    private Screen getScreen(int position) {
        switch (adapter.getCategory(position)) {
            case AUDIO:
                return Screen.AUDIO_GENRES;
            case MUSIC:
            default:
                return Screen.MUSIC_GENRES;
        }
    }
}