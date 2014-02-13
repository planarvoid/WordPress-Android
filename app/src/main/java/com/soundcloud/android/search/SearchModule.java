package com.soundcloud.android.search;

import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.storage.StorageModule;
import dagger.Module;

@Module(complete = false,
        injects = {CombinedSearchActivity.class, TabbedSearchFragment.class, SearchResultsFragment.class},
        includes = {ApiModule.class, StorageModule.class}
)
public class SearchModule {}
