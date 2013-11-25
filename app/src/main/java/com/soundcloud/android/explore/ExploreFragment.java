package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.viewpagerindicator.TabPageIndicator;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class ExploreFragment extends Fragment {

    private ViewPager mPager;

    @Inject
    Resources mResources;

    @Inject
    ExplorePagerAdapterFactory mExplorePagerAdapterFactory;

    private ExplorePagerAdapter mExplorePagerAdapter;

    public ExploreFragment() {
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
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

        mPager.setCurrentItem(ExplorePagerAdapter.TAB_CATEGORIES);
    }

    @Override
    public void onDestroyView() {
        // it's important to reset the adapter here. since otherwise this will leak a Context reference through
        // the dataset observer Android registers internally (and we're retaining the adapter instance)
        mPager.setAdapter(null);
        super.onDestroyView();
    }

}
