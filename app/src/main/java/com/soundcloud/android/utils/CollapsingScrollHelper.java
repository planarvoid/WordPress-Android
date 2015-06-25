package com.soundcloud.android.utils;

import com.soundcloud.android.R;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import javax.inject.Inject;

public class CollapsingScrollHelper extends DefaultSupportFragmentLightCycle<Fragment>
        implements AppBarLayout.OnOffsetChangedListener {

    private static final int TOP = 0;
    private AppBarLayout appBarLayout;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Inject
    public CollapsingScrollHelper() {}

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.str_layout);
        if (this.swipeRefreshLayout == null) {
            throw new IllegalStateException("Expected to find SwipeRefreshLayout with ID R.id.str_layout");
        }

        appBarLayout = (AppBarLayout) view.findViewById(R.id.appbar);
        if (this.appBarLayout == null) {
            throw new IllegalStateException("Expected to find AppBarLayout with ID R.id.appbar");
        }

        appBarLayout.addOnOffsetChangedListener(this);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        appBarLayout.removeOnOffsetChangedListener(this);
        swipeRefreshLayout = null;
        appBarLayout = null;
        super.onDestroyView(fragment);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        swipeRefreshLayout.setEnabled(offset >= TOP);
    }

}
