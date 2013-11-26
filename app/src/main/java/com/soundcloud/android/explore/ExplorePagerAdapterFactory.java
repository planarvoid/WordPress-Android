package com.soundcloud.android.explore;


import android.content.res.Resources;
import android.support.v4.app.FragmentManager;

import javax.inject.Inject;

/**
 * This factory is a workaround, since dagger does not provide assisted injection
 * Using this factory, the ExploreFragment can provide its own Fragment Manager
 */
public class ExplorePagerAdapterFactory {

    private final Resources mResources;

    @Inject
    public ExplorePagerAdapterFactory(Resources resources) {
        mResources = resources;
    }

    public ExplorePagerAdapter create(FragmentManager fragmentManager) {
        return new ExplorePagerAdapter(mResources, fragmentManager);
    }
}
