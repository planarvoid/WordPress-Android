package com.soundcloud.android.fragment;

import com.google.common.collect.Maps;
import com.soundcloud.android.R;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.viewpagerindicator.TabPageIndicator;
import rx.Observable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;
import java.util.Map;

public class ExploreFragment extends Fragment {

    private static final int TAB_CATEGORIES = 0;
    private static final int TAB_POPULAR_MUSIC = 1;
    private static final int TAB_POPULAR_AUDIO = 2;

    private TabPageIndicator mIndicator;
    private ViewPager mPager;
    private ExplorePagerAdapter mExplorePagerAdapter;
    private final Map<String, Observable<?>> mObservableRegistry = Maps.newHashMapWithExpectedSize(3);

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setRetainInstance(true);
        mExplorePagerAdapter = new ExplorePagerAdapter(getChildFragmentManager());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.explore_activity, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPager = (ViewPager) view.findViewById(R.id.pager);
        mPager.setAdapter(mExplorePagerAdapter);
        mExplorePagerAdapter.notifyDataSetChanged();
        mPager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        mPager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        mIndicator = (TabPageIndicator) view.findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);

        mPager.setCurrentItem(TAB_POPULAR_MUSIC);
    }

    /* package */ Map<String, Observable<?>> getObservableRegistry() {
        return mObservableRegistry;
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
                    return getString(R.string.explore_genres).toUpperCase(Locale.getDefault());
                case TAB_POPULAR_MUSIC:
                    return getString(R.string.explore_category_trending_music).toUpperCase(Locale.getDefault());
                case TAB_POPULAR_AUDIO:
                    return getString(R.string.explore_category_trending_audio).toUpperCase(Locale.getDefault());
            }
            throw new RuntimeException("Unexpected position for getPageTitle " + position);
        }
    }
}
