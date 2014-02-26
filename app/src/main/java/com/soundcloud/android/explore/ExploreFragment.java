package com.soundcloud.android.explore;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.view.SlidingTabLayout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class ExploreFragment extends Fragment {

    @Inject
    EventBus mEventBus;
    @Inject
    ExplorePagerAdapterFactory mPagerAdapterFactory;

    private ExplorePagerAdapter mPagerAdapter;
    private ViewPager mPager;

    public ExploreFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    ExploreFragment(ExplorePagerAdapterFactory pagerAdapterFactory, EventBus eventBus) {
        mPagerAdapterFactory = pagerAdapterFactory;
        mEventBus = eventBus;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.explore_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPager = (ViewPager) view.findViewById(R.id.pager);
        mPager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        mPager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        mPagerAdapter = mPagerAdapterFactory.create(this.getChildFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        SlidingTabLayout tabIndicator = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        tabIndicator.setViewPager(mPager);
        tabIndicator.setOnPageChangeListener(new ExplorePagerScreenListener(mEventBus));

        if (savedInstanceState == null) {
            mPager.setCurrentItem(1);
        }
    }

    @Override
    public void onDestroyView() {
        // it's important to reset the adapter here. since otherwise this will leak a Context reference through
        // the dataset observer Android registers internally (and we're retaining the adapter instance)
        mPager = null;
        mPagerAdapter = null;
        super.onDestroyView();
    }

    protected static class ExplorePagerScreenListener implements ViewPager.OnPageChangeListener {
        private final EventBus mEventBus;

        public ExplorePagerScreenListener(EventBus eventBus) {
            mEventBus = eventBus;
        }

        @Override
        public void onPageScrolled(int i, float v, int i2) {}

        @Override
        public void onPageSelected(int pageSelected) {
            switch (pageSelected) {
                case ExplorePagerAdapter.TAB_GENRES:
                    mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.EXPLORE_GENRES.get());
                    break;
                case ExplorePagerAdapter.TAB_TRENDING_MUSIC:
                    mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.EXPLORE_TRENDING_MUSIC.get());
                    break;
                case ExplorePagerAdapter.TAB_TRENDING_AUDIO:
                    mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.EXPLORE_TRENDING_AUDIO.get());
                    break;
                default:
                    throw new IllegalArgumentException("Did not recognise page in pager to publish screen event");
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {}
    }
}
