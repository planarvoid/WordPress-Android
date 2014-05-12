package com.soundcloud.android.search;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                TabbedSearchFragment.class,
                SearchResultsFragment.class,
                PlaylistTagsFragment.class,
                PlaylistResultsFragment.class
        })
public class SearchModule {
}
