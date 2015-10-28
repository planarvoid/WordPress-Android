package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

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

@Deprecated // Explore is moving to standalone ExploreActivity
@SuppressLint("ValidFragment")
public class ExploreFragment extends Fragment {

    @Inject Resources resources;
    @Inject ExplorePagerAdapterFactory pagerAdapterFactory;
    @Inject ExplorePagerScreenListener screenListener;

    private ExplorePagerAdapter pagerAdapter;
    private ViewPager pager;

    public ExploreFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    ExploreFragment(Resources resources, ExplorePagerAdapterFactory pagerAdapterFactory,
                    ExplorePagerScreenListener screenListener) {
        this.resources = resources;
        this.pagerAdapterFactory = pagerAdapterFactory;
        this.screenListener = screenListener;
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
        pager.setPageMargin(resources.getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        pagerAdapter = pagerAdapterFactory.create(this.getChildFragmentManager());
        pager.setAdapter(pagerAdapter);

        TabLayout tabIndicator = (TabLayout) view.findViewById(R.id.tab_indicator);
        tabIndicator.setupWithViewPager(pager);
        pager.addOnPageChangeListener(screenListener);

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

}
