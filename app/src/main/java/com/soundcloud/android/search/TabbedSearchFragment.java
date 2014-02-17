package com.soundcloud.android.search;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.dagger.DaggerDependencyInjector;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.view.SlidingTabLayout;

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
public class TabbedSearchFragment extends Fragment {

    public static final String TAG = "tabbed_search";

    private final static String KEY_QUERY = "query";

    @Inject
    EventBus mEventBus;
    @Inject
    Resources mResources;

    private SearchPagerAdapter mSearchPagerAdapter;
    private ViewPager mPager;

    public static TabbedSearchFragment newInstance(String query) {
        TabbedSearchFragment fragment = new TabbedSearchFragment();

        Bundle bundle = new Bundle();
        bundle.putString(KEY_QUERY, query);
        fragment.setArguments(bundle);
        return fragment;
    }

    public TabbedSearchFragment() {
        setRetainInstance(true);
        new DaggerDependencyInjector().fromAppGraphWithModules(new SearchModule()).inject(this);
    }

    @VisibleForTesting
    TabbedSearchFragment(EventBus eventBus, Resources resources) {
        mEventBus = eventBus;
        mResources = resources;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Workaround for onPageSelected not being triggered on creation
        mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_EVERYTHING.get());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tabbed_search_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String query = getArguments().getString(KEY_QUERY);
        mSearchPagerAdapter = new SearchPagerAdapter(mResources, this.getChildFragmentManager(), query);

        mPager = (ViewPager) view.findViewById(R.id.pager);
        mPager.setAdapter(mSearchPagerAdapter);
        mPager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        mPager.setPageMargin(mResources.getDimensionPixelOffset(R.dimen.view_pager_divider_width));
        mPager.setOffscreenPageLimit(2);

        SlidingTabLayout tabIndicator = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        tabIndicator.setViewPager(mPager);
        tabIndicator.setOnPageChangeListener(new SearchPagerScreenListener(mEventBus));
    }

    @Override
    public void onDestroyView() {
        // Avoid leaking context through internal dataset observer in adapter
        mPager = null;
        super.onDestroyView();
    }

    protected static class SearchPagerScreenListener implements ViewPager.OnPageChangeListener {
        private final EventBus mEventBus;

        public SearchPagerScreenListener(EventBus eventBus) {
            mEventBus = eventBus;
        }

        @Override
        public void onPageScrolled(int i, float v, int i2) {}

        @Override
        public void onPageSelected(int pageSelected) {
            switch (pageSelected) {
                case SearchPagerAdapter.TAB_ALL:
                    mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_EVERYTHING.get());
                    break;
                case SearchPagerAdapter.TAB_TRACKS:
                    mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_TRACKS.get());
                    break;
                case SearchPagerAdapter.TAB_PLAYLISTS:
                    mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_PLAYLISTS.get());
                    break;
                case SearchPagerAdapter.TAB_PEOPLE:
                    mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_USERS.get());
                    break;
                default:
                    throw new IllegalArgumentException("Did not recognise page in pager to publish screen event");
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {}
    }

}
