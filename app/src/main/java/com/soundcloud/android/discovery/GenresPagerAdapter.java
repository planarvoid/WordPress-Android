package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartCategory;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class GenresPagerAdapter extends FragmentPagerAdapter {

    private final Resources resources;

    public GenresPagerAdapter(FragmentManager fm, Resources resources) {
        super(fm);
        this.resources = resources;
    }

    @Override
    public Fragment getItem(int position) {
        return GenresFragment.create(getCategory(position));
    }

    @NonNull
    private ChartCategory getCategory(int position) {
        return position == ChartCategory.MUSIC.ordinal() ? ChartCategory.MUSIC : ChartCategory.AUDIO;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getCategory(position) == ChartCategory.MUSIC ?
               resources.getString(R.string.explore_genre_header_music) :
               resources.getString(R.string.explore_genre_header_audio);
    }
}
