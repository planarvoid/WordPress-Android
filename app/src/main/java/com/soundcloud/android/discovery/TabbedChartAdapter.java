package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class TabbedChartAdapter extends FragmentPagerAdapter {

    private final Resources resources;
    private final Urn chartGenreUrn;

    public TabbedChartAdapter(FragmentManager fm, Resources resources, Urn chartGenreUrn) {
        super(fm);
        this.resources = resources;
        this.chartGenreUrn = chartGenreUrn;
    }

    @Override
    public Fragment getItem(int position) {
        return ChartFragment.create(getType(position), chartGenreUrn);
    }

    @NonNull
    private ChartType getType(int position) {
        return position == ChartType.TRENDING.ordinal() ? ChartType.TRENDING : ChartType.TOP;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getType(position) == ChartType.TRENDING ?
               resources.getString(R.string.charts_new_and_hot_tab_header) :
               resources.getString(R.string.charts_top_fifty_tab_header);
    }
}
