package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class ChartPagerAdapter extends FragmentPagerAdapter {

    private final Resources resources;
    private final Urn chartGenreUrn;

    ChartPagerAdapter(FragmentManager fm, Resources resources, Urn chartGenreUrn) {
        super(fm);
        this.resources = resources;
        this.chartGenreUrn = chartGenreUrn;
    }

    @Override
    public Fragment getItem(int position) {
        return ChartTracksFragment.create(getType(position), chartGenreUrn);
    }

    @NonNull
    ChartType getType(int position) {
        return position == ChartType.TRENDING.ordinal() ? ChartType.TRENDING : ChartType.TOP;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getType(position) == ChartType.TRENDING ?
               resources.getString(R.string.charts_trending) :
               resources.getString(R.string.charts_top);
    }
}
