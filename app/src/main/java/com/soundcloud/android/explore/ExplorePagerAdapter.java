package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class ExplorePagerAdapter extends FragmentPagerAdapter {
    protected static final int TAB_GENRES = 0;
    protected static final int TAB_TRENDING_MUSIC = 1;
    protected static final int TAB_TRENDING_AUDIO = 2;

    private final Resources resources;

    public ExplorePagerAdapter(Resources resources, FragmentManager fm) {
        super(fm);
        this.resources = resources;
    }

    @Override
    public float getPageWidth(int position) {
        return position == 0 ? resources.getDimension(R.dimen.explore_category_page_size) : super.getPageWidth(position);
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
            default:
                throw new IllegalArgumentException("Unexpected position for getItem " + position);
        }
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_GENRES:
                return resources.getString(R.string.explore_genres);
            case TAB_TRENDING_MUSIC:
                return resources.getString(R.string.explore_category_trending_music);
            case TAB_TRENDING_AUDIO:
                return resources.getString(R.string.explore_category_trending_audio);
            default:
                throw new IllegalArgumentException("Unexpected position for getPageTitle " + position);
        }
    }

}
