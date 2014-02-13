package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.viewpagerindicator.TabPageIndicator;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TabbedSearchFragment extends Fragment {

    public static final String TAG = "tabbed_search";

    private final static String KEY_QUERY = "query";

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tabbed_search_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String query = getArguments().getString(KEY_QUERY);
        mSearchPagerAdapter = new SearchPagerAdapter(getResources(), this.getChildFragmentManager(), query);

        mPager = (ViewPager) view.findViewById(R.id.pager);
        mPager.setAdapter(mSearchPagerAdapter);
        mPager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        mPager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.view_pager_divider_width));
        mPager.setOffscreenPageLimit(2);

        TabPageIndicator mIndicator = (TabPageIndicator) view.findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
    }
}
