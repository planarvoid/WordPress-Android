package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.lightcycle.LightCycleFragment;
import com.soundcloud.android.view.SlidingTabLayout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class PlaylistsFragment extends LightCycleFragment {

    public PlaylistsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playlists_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PlaylistsPagerAdapter adapter = new PlaylistsPagerAdapter(this.getChildFragmentManager(), getResources());
        ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
        pager.setAdapter(adapter);
        pager.setPageMarginDrawable(R.drawable.divider_vertical_grey);
        pager.setPageMargin(getResources().getDimensionPixelOffset(R.dimen.view_pager_divider_width));

        SlidingTabLayout tabIndicator = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        tabIndicator.setViewPager(pager);
    }
}
