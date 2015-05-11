package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.menu.DefaultActionMenuController;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.view.SlidingTabLayout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlaylistsFragment extends LightCycleSupportFragment {

    // A ActionMenuController can be injected in the inner pager fragments
    // if we need more granularity for the menu actions control
    @Inject DefaultActionMenuController defaultActionMenuController;
    @Inject FeatureFlags featureFlags;

    public PlaylistsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        defaultActionMenuController.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return defaultActionMenuController.onOptionsItemSelected(this, item);
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
