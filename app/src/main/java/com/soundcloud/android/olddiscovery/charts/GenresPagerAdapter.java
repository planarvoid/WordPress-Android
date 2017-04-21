package com.soundcloud.android.olddiscovery.charts;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartCategory;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.Locale;

class GenresPagerAdapter extends FragmentPagerAdapter {

    private final Resources resources;

    GenresPagerAdapter(FragmentManager fm, Resources resources) {
        super(fm);
        this.resources = resources;
    }

    @Override
    public Fragment getItem(int position) {
        return GenresFragment.create(getCategory(position));
    }

    ChartCategory getCategory(int position) {
        return position == ChartCategory.MUSIC.ordinal() ? ChartCategory.MUSIC : ChartCategory.AUDIO;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getCategory(position) == ChartCategory.MUSIC ?
               resources.getString(R.string.charts_music).toUpperCase(Locale.US) :
               resources.getString(R.string.charts_audio).toUpperCase(Locale.US);
    }
}
