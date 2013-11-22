package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.model.ExploreTracksCategory;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import javax.inject.Inject;
import java.util.Locale;

public class ExplorePagerAdapter extends FragmentPagerAdapter {
    protected static final int TAB_CATEGORIES = 0;
    protected static final int TAB_POPULAR_MUSIC = 1;
    protected static final int TAB_POPULAR_AUDIO = 2;

    private final Resources mResources;

    @Inject
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
            case TAB_CATEGORIES:
                return new ExploreTracksCategoriesFragment();
            case TAB_POPULAR_MUSIC:
                return ExploreTracksFragment.fromCategory(ExploreTracksCategory.POPULAR_MUSIC_CATEGORY);
            case TAB_POPULAR_AUDIO:
                return ExploreTracksFragment.fromCategory(ExploreTracksCategory.POPULAR_AUDIO_CATEGORY);
        }
        throw new RuntimeException("Unexpected position for getItem " + position);
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_CATEGORIES:
                return mResources.getString(R.string.explore_genres).toUpperCase(Locale.getDefault());
            case TAB_POPULAR_MUSIC:
                return mResources.getString(R.string.explore_category_trending_music).toUpperCase(Locale.getDefault());
            case TAB_POPULAR_AUDIO:
                return mResources.getString(R.string.explore_category_trending_audio).toUpperCase(Locale.getDefault());
        }
        throw new RuntimeException("Unexpected position for getPageTitle " + position);
    }
}
