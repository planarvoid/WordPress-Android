package com.soundcloud.android.search;

import dagger.Module;

@Module(complete = false,
        injects = {
                TabbedSearchFragment.class,
                SearchResultsFragment.class,
                PlaylistTagsFragment.class,
                PlaylistResultsFragment.class
        })
public class SearchModule {
}
