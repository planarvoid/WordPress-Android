package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.view.SlidingTabLayout;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class ExploreFragment extends Fragment {

    @Inject EventBus eventBus;
    @Inject ExplorePagerAdapterFactory pagerAdapterFactory;

    private ExplorePagerAdapter pagerAdapter;
    private ViewPager pager;

    public ExploreFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    ExploreFragment(ExplorePagerAdapterFactory pagerAdapterFactory, EventBus eventBus) {
        this.pagerAdapterFactory = pagerAdapterFactory;
        this.eventBus = eventBus;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.explore_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pager = (ViewPager) view.findViewById(R.id.pager);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        pagerAdapter = pagerAdapterFactory.create(this.getChildFragmentManager());
        pager.setAdapter(pagerAdapter);

        SlidingTabLayout tabIndicator = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        tabIndicator.setViewPager(pager);
        tabIndicator.setOnPageChangeListener(new ExplorePagerScreenListener(eventBus));

        if (savedInstanceState == null) {
            pager.setCurrentItem(1);
        }
    }

    @Override
    public void onDestroyView() {
        // it's important to reset the adapter here. since otherwise this will leak a Context reference through
        // the dataset observer Android registers internally (and we're retaining the adapter instance)
        pager = null;
        pagerAdapter = null;
        super.onDestroyView();
    }

    protected static class ExplorePagerScreenListener implements ViewPager.OnPageChangeListener {
        private final EventBus eventBus;

        public ExplorePagerScreenListener(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        @Override
        public void onPageScrolled(int i, float v, int i2) {}

        @Override
        public void onPageSelected(int pageSelected) {
            switch (pageSelected) {
                case ExplorePagerAdapter.TAB_GENRES:
                    eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.EXPLORE_GENRES));
                    break;
                case ExplorePagerAdapter.TAB_TRENDING_MUSIC:
                    eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.EXPLORE_TRENDING_MUSIC));
                    break;
                case ExplorePagerAdapter.TAB_TRENDING_AUDIO:
                    eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.EXPLORE_TRENDING_AUDIO));
                    break;
                default:
                    throw new IllegalArgumentException("Did not recognise page in pager to publish screen event");
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {}
    }
}
