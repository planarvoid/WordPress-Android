package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.dagger.DaggerDependencyInjector;
import com.soundcloud.android.dagger.DependencyInjector;
import com.soundcloud.android.events.EventBus;
import com.viewpagerindicator.TabPageIndicator;

import android.annotation.SuppressLint;
import android.content.res.Resources;
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
    Resources mResources;

    @Inject
    ExplorePagerAdapterFactory mExplorePagerAdapterFactory;

    private ExplorePagerAdapter mExplorePagerAdapter;
    private ViewPager mPager;
    private DependencyInjector mInjector;

    public ExploreFragment() {
        this(new DaggerDependencyInjector());
    }

    public ExploreFragment(DependencyInjector injector){
        mInjector = injector;
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mInjector.fromAppGraphWithModules(new ExploreModule()).inject(this);
        mExplorePagerAdapter = mExplorePagerAdapterFactory.create(this.getChildFragmentManager());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.explore_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPager = (ViewPager) view.findViewById(R.id.pager);
        mPager.setAdapter(mExplorePagerAdapter);
        mPager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        mPager.setPageMargin(mResources.getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        TabPageIndicator mIndicator = (TabPageIndicator) view.findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.setOnPageChangeListener(new ExplorePagerScreenListener());
    }

    @Override
    public void onDestroyView() {
        // it's important to reset the adapter here. since otherwise this will leak a Context reference through
        // the dataset observer Android registers internally (and we're retaining the adapter instance)
        mPager = null;
        super.onDestroyView();
    }

    protected static class ExplorePagerScreenListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int i, float v, int i2) {}

        @Override
        public void onPageSelected(int pageSelected) {
            switch (pageSelected) {
                case ExplorePagerAdapter.TAB_GENRES:
                    EventBus.SCREEN_ENTERED.publish(Screen.EXPLORE_GENRES.get());
                    break;
                case ExplorePagerAdapter.TAB_TRENDING_MUSIC:
                    EventBus.SCREEN_ENTERED.publish(Screen.EXPLORE_TRENDING_MUSIC.get());
                    break;
                case ExplorePagerAdapter.TAB_TRENDING_AUDIO:
                    EventBus.SCREEN_ENTERED.publish(Screen.EXPLORE_TRENDING_AUDIO.get());
                    break;
                default:
                    throw new IllegalArgumentException("Did not recognise page in pager to publish screen event");
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {}
    }
}
