package com.soundcloud.android.search;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.associations.AssociationsModule;
import dagger.Module;
import dagger.Provides;

import java.util.Random;

@Module(addsTo = ApplicationModule.class,
        injects = {
                TabbedSearchFragment.class,
                SearchActivity.class,
                SearchResultsFragment.class,
                PlaylistTagsFragment.class,
                PlaylistResultsFragment.class,
                PlayFromVoiceSearchActivity.class
        }, includes = AssociationsModule.class)
public class SearchModule {

    @Provides
    public Random provideRandom() {
        return new Random();
    }
}
