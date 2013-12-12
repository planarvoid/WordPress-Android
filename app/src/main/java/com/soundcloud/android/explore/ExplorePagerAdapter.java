package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.ExploreGenre;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.Locale;

public class ExplorePagerAdapter extends FragmentPagerAdapter {
    protected static final int TAB_GENRES = 0;
    protected static final int TAB_TRENDING_MUSIC = 1;
    protected static final int TAB_TRENDING_AUDIO = 2;

    private final Resources mResources;

    public ExplorePagerAdapter(Resources resources, FragmentManager fm) {
        super(fm);
        mResources = resources;
    }

    @Override
    public float getPageWidth(int position) {
        return position == 0 ? mResources.getDimension(R.dimen.explore_category_page_size) : super.getPageWidth(position);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case TAB_GENRES:
                return new ExploreGenresFragment();
            case TAB_TRENDING_MUSIC:
                return ExploreTracksFragment.create(ExploreGenre.POPULAR_MUSIC_CATEGORY, Screen.EXPLORE_TRENDING_MUSIC);
            case TAB_TRENDING_AUDIO:
                return ExploreTracksFragment.create(ExploreGenre.POPULAR_AUDIO_CATEGORY, Screen.EXPLORE_TRENDING_AUDIO);
        }
        throw new IllegalArgumentException("Unexpected position for getItem " + position);
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_GENRES:
                return mResources.getString(R.string.explore_genres).toUpperCase(Locale.getDefault());
            case TAB_TRENDING_MUSIC:
                return mResources.getString(R.string.explore_category_trending_music).toUpperCase(Locale.getDefault());
            case TAB_TRENDING_AUDIO:
                return mResources.getString(R.string.explore_category_trending_audio).toUpperCase(Locale.getDefault());
        }
        throw new IllegalArgumentException("Unexpected position for getPageTitle " + position);
    }
}
