package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ExploreTracksCategoriesFragment;
import com.soundcloud.android.fragment.ExploreTracksFragment;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.viewpagerindicator.TabPageIndicator;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import java.util.Locale;

public class ExploreActivity extends ScActivity implements ScLandingPage
{
    private TabPageIndicator mIndicator;
    private ViewPager mPager;
    private ExplorePagerAdapter mExplorePagerAdapter;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.explore_activity);

        mExplorePagerAdapter = new ExplorePagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mExplorePagerAdapter);

        mIndicator = (TabPageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);

        mPager.setCurrentItem(1);
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_discover;
    }

    class ExplorePagerAdapter extends FragmentPagerAdapter {

        public ExplorePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public float getPageWidth(int position) {
            return position == 0 ? getResources().getDimension(R.dimen.explore_category_page_size) : super.getPageWidth(position);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new ExploreTracksCategoriesFragment();
                case 1:
                    return ExploreTracksFragment.fromCategory(ExploreTracksCategory.POPULAR_MUSIC_CATEGORY);
                case 2:
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
            switch (position){
                case 0:
                    return getString(R.string.discover_genres).toUpperCase(Locale.getDefault());
                case 1:
                    return getString(R.string.discover_category_trending_music).toUpperCase(Locale.getDefault());
                case 2:
                    return getString(R.string.discover_category_trending_audio).toUpperCase(Locale.getDefault());
            }
            throw new RuntimeException("Unexpected position for getPageTitle " + position);

        }
    }
}
