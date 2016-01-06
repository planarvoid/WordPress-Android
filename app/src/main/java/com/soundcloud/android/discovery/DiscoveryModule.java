package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.search.PlayFromVoiceSearchActivity;
import com.soundcloud.android.search.PlaylistResultsFragment;
import com.soundcloud.android.search.SearchPremiumResultsActivity;
import com.soundcloud.android.search.SearchPremiumResultsFragment;
import com.soundcloud.android.search.SearchResultsFragment;
import com.soundcloud.android.search.TabbedSearchFragment;
import dagger.Module;
import dagger.Provides;

import java.util.Random;

@Module(addsTo = ApplicationModule.class,
        injects = {
                DiscoveryFragment.class,
                RecommendedTracksActivity.class,
                RecommendedTracksFragment.class,
                SearchActivity.class,
                SearchPremiumResultsActivity.class,
                PlaylistDiscoveryActivity.class,
                TabbedSearchFragment.class,
                SearchResultsFragment.class,
                PlaylistResultsFragment.class,
                PlayFromVoiceSearchActivity.class,
                SearchPremiumResultsFragment.class
        }, includes = AssociationsModule.class)
public class DiscoveryModule {

    @Provides
    public Random provideRandom() {
        return new Random();
    }
}
