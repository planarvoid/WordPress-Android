package com.soundcloud.android.search;

import com.soundcloud.android.api.ApiModule;
import dagger.Module;

@Module(complete = false,
        injects = {CombinedSearchActivity.class, SearchResultsFragment.class},
        includes = {ApiModule.class}
)
public class SearchModule {}
