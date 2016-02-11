package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.discovery.SearchTracker;
import com.soundcloud.android.main.Screen;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class TabbedSearchFragment extends Fragment {

    public static final String TAG = "tabbed_search";

    private static final String KEY_QUERY = "query";

    @Inject Resources resources;
    @Inject SearchTracker searchTracker;

    private ViewPager pager;

    public static TabbedSearchFragment newInstance(String query) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_QUERY, query);

        TabbedSearchFragment fragment = new TabbedSearchFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public TabbedSearchFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    TabbedSearchFragment(Resources resources, SearchTracker searchTracker) {
        this.resources = resources;
        this.searchTracker = searchTracker;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchTracker.trackResultsScreenEvent(Screen.SEARCH_EVERYTHING);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.tabbed_search_fragment, container, false);
        fragmentView.setBackgroundColor(getResources().getColor(R.color.primary));
        return fragmentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String query = getArguments().getString(KEY_QUERY);
        boolean firstTime = savedInstanceState == null;

        SearchPagerAdapter searchPagerAdapter =
                new SearchPagerAdapter(resources, this.getChildFragmentManager(), query, firstTime);

        pager = (ViewPager) view.findViewById(R.id.pager);
        pager.setAdapter(searchPagerAdapter);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        TabLayout tabIndicator = (TabLayout) view.findViewById(R.id.tab_indicator);
        tabIndicator.setupWithViewPager(pager);
        pager.addOnPageChangeListener(new SearchPagerScreenListener(searchTracker));
    }

    @Override
    public void onDestroyView() {
        // Avoid leaking context through internal dataset observer in adapter
        pager = null;
        super.onDestroyView();
    }

    protected static class SearchPagerScreenListener implements ViewPager.OnPageChangeListener {
        private final SearchTracker searchTracker;

        public SearchPagerScreenListener(SearchTracker searchTracker) {
            this.searchTracker = searchTracker;
        }

        @Override
        public void onPageScrolled(int i, float v, int i2) {
        }

        @Override
        public void onPageSelected(int pageSelected) {
            switch (pageSelected) {
                case SearchPagerAdapter.TAB_ALL:
                    searchTracker.trackResultsScreenEvent(Screen.SEARCH_EVERYTHING);
                    break;
                case SearchPagerAdapter.TAB_TRACKS:
                    searchTracker.trackResultsScreenEvent(Screen.SEARCH_TRACKS);
                    break;
                case SearchPagerAdapter.TAB_PLAYLISTS:
                    searchTracker.trackResultsScreenEvent(Screen.SEARCH_PLAYLISTS);
                    break;
                case SearchPagerAdapter.TAB_PEOPLE:
                    searchTracker.trackResultsScreenEvent(Screen.SEARCH_USERS);
                    break;
                default:
                    throw new IllegalArgumentException("Did not recognise page in pager to publish screen event");
            }
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    }

}
