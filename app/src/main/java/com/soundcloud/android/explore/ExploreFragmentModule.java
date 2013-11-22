package com.soundcloud.android.explore;

import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;
import android.support.v4.app.FragmentManager;

@Module (complete = false, injects = {ExploreFragment.class})
public class ExploreFragmentModule {

    private FragmentManager mFragmentManager;

    public ExploreFragmentModule(FragmentManager fragmentManager){
        this.mFragmentManager = fragmentManager;
    }

    @Provides
    public ExplorePagerAdapter provideExplorePagerAdapter(Resources resources){
        return new ExplorePagerAdapter(resources, mFragmentManager);
    }
}
