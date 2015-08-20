package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.view.SlidingTabLayout;
import com.soundcloud.rx.eventbus.EventBus;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
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

    @Inject EventBus eventBus;
    @Inject Resources resources;

    private ViewPager pager;


    public static TabbedSearchFragment newInstance(String query) {
        TabbedSearchFragment fragment = new TabbedSearchFragment();

        Bundle bundle = new Bundle();
        bundle.putString(KEY_QUERY, query);
        fragment.setArguments(bundle);
        return fragment;
    }

    public TabbedSearchFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    TabbedSearchFragment(EventBus eventBus, Resources resources) {
        this.eventBus = eventBus;
        this.resources = resources;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Workaround for onPageSelected not being triggered on creation
        eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_EVERYTHING));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tabbed_search_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String query = getArguments().getString(KEY_QUERY);
        boolean firstTime = savedInstanceState == null;
        SearchPagerAdapter searchPagerAdapter = new SearchPagerAdapter(resources, this.getChildFragmentManager(), query, firstTime);

        pager = (ViewPager) view.findViewById(R.id.pager);
        pager.setAdapter(searchPagerAdapter);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        SlidingTabLayout tabIndicator = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        tabIndicator.setViewPager(pager);
        tabIndicator.setOnPageChangeListener(new SearchPagerScreenListener(eventBus));
    }

    @Override
    public void onDestroyView() {
        // Avoid leaking context through internal dataset observer in adapter
        pager = null;
        super.onDestroyView();
    }

    protected static class SearchPagerScreenListener implements ViewPager.OnPageChangeListener {
        private final EventBus eventBus;

        public SearchPagerScreenListener(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        @Override
        public void onPageScrolled(int i, float v, int i2) {}

        @Override
        public void onPageSelected(int pageSelected) {
            switch (pageSelected) {
                case SearchPagerAdapter.TAB_ALL:
                    eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_EVERYTHING));
                    break;
                case SearchPagerAdapter.TAB_TRACKS:
                    eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_TRACKS));
                    break;
                case SearchPagerAdapter.TAB_PLAYLISTS:
                    eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_PLAYLISTS));
                    break;
                case SearchPagerAdapter.TAB_PEOPLE:
                    eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SEARCH_USERS));
                    break;
                default:
                    throw new IllegalArgumentException("Did not recognise page in pager to publish screen event");
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {}
    }

}
