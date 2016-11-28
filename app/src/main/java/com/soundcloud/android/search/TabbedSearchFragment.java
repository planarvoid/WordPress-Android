package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
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
    private static final String KEY_QUERY_URN = "queryUrn";
    private static final String KEY_QUERY_POSITION = "queryPosition";

    @Inject Resources resources;
    @Inject SearchTracker searchTracker;

    private ViewPager pager;

    public static TabbedSearchFragment newInstance(String query,
                                                   Optional<Urn> queryUrn,
                                                   Optional<Integer> queryPosition) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_QUERY, query);
        if (queryUrn.isPresent()) {
            bundle.putParcelable(KEY_QUERY_URN, queryUrn.get());
        }

        if (queryPosition.isPresent()) {
            bundle.putInt(KEY_QUERY_POSITION, queryPosition.get());
        }

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
        searchTracker.init();
        searchTracker.trackResultsScreenEvent(SearchType.ALL, getSearchQuery());
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

        boolean firstTime = savedInstanceState == null;

        SearchPagerAdapter searchPagerAdapter =
                new SearchPagerAdapter(resources,
                                       this.getChildFragmentManager(),
                                       getSearchQuery(),
                                       getSearchQueryUrn(),
                                       getSearchQueryPosition(),
                                       firstTime);

        pager = (ViewPager) view.findViewById(R.id.pager);
        pager.setAdapter(searchPagerAdapter);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        TabLayout tabIndicator = (TabLayout) view.findViewById(R.id.tab_indicator);
        tabIndicator.setupWithViewPager(pager);
        pager.addOnPageChangeListener(new SearchPagerScreenListener(searchTracker, getSearchQuery()));
    }

    private String getSearchQuery() {
        return getArguments().getString(KEY_QUERY);
    }

    private Optional<Urn> getSearchQueryUrn() {
        return Optional.fromNullable(getArguments().<Urn>getParcelable(KEY_QUERY_URN));
    }

    private Optional<Integer> getSearchQueryPosition() {
        return Optional.fromNullable(getArguments().getInt(KEY_QUERY_POSITION));
    }

    @Override
    public void onDestroyView() {
        // Avoid leaking context through internal dataset observer in adapter
        pager = null;
        super.onDestroyView();
    }

    protected static class SearchPagerScreenListener implements ViewPager.OnPageChangeListener {
        private final SearchTracker searchTracker;
        private final String searchQuery;

        SearchPagerScreenListener(SearchTracker searchTracker, String searchQuery) {
            this.searchTracker = searchTracker;
            this.searchQuery = searchQuery;
        }

        @Override
        public void onPageScrolled(int i, float v, int i2) {
        }

        @Override
        public void onPageSelected(int pageSelected) {
            searchTracker.trackResultsScreenEvent(SearchType.get(pageSelected), searchQuery);
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }
    }

}
