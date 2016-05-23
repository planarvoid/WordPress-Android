package com.soundcloud.android.discovery;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.search.PlayFromVoiceSearchActivity;
import com.soundcloud.android.search.PlaylistResultsFragment;
import com.soundcloud.android.search.SearchPremiumResultsActivity;
import com.soundcloud.android.search.SearchPremiumResultsFragment;
import com.soundcloud.android.search.SearchResultsFragment;
import com.soundcloud.android.search.SearchTracker;
import com.soundcloud.android.search.TabbedSearchFragment;
import com.soundcloud.android.search.suggestions.LegacySuggestionsAdapter;
import com.soundcloud.android.search.suggestions.SearchSuggestionsFragment;
import com.soundcloud.android.utils.KeyboardHelper;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Module;
import dagger.Provides;

import android.content.res.Resources;

import javax.inject.Provider;
import java.util.Random;

@Module(addsTo = ApplicationModule.class,
        injects = {
                DiscoveryFragment.class,
                ViewAllRecommendedTracksActivity.class,
                ViewAllRecommendedTracksFragment.class,
                SearchActivity.class,
                SearchPremiumResultsActivity.class,
                PlaylistDiscoveryActivity.class,
                TabbedSearchFragment.class,
                SearchResultsFragment.class,
                SearchSuggestionsFragment.class,
                PlaylistResultsFragment.class,
                PlayFromVoiceSearchActivity.class,
                SearchPremiumResultsFragment.class
        }, includes = AssociationsModule.class)
public class DiscoveryModule {

    @Provides
    public Random provideRandom() {
        return new Random();
    }

    @Provides
    public FeaturedSearchPresenter provideSearchPresenter(FeatureFlags featureFlags,
                                                          SearchIntentResolverFactory intentResolverFactory,
                                                          SearchTracker tracker,
                                                          Resources resources,
                                                          LegacySuggestionsAdapter adapter,
                                                          SuggestionsHelperFactory suggestionsHelperFactory,
                                                          EventBus eventBus,
                                                          KeyboardHelper keyboardHelper,
                                                          Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                                                          PlaybackInitiator playbackInitiator,
                                                          Navigator navigator) {
        if (featureFlags.isEnabled(Flag.NEW_SEARCH_SUGGESTIONS)) {
            return new SearchPresenter(intentResolverFactory, tracker, resources, eventBus, keyboardHelper,
                    expandPlayerSubscriberProvider, playbackInitiator, navigator);
        } else {
            return new LegacySearchPresenter(intentResolverFactory, tracker, resources, adapter,
                    suggestionsHelperFactory, eventBus, keyboardHelper);
        }
    }

    @Provides
    public SearchIntentResolverFactory provideSearchIntentResolverFactory(Provider<Navigator> navigatorProvider,
                                                                          Provider<SearchTracker> trackerProvider) {
        return new SearchIntentResolverFactory(navigatorProvider, trackerProvider);
    }

    @Provides
    public SuggestionsHelperFactory provideSuggestionsHelperFactory(Provider<Navigator> navigatorProvider,
                                                                    Provider<EventBus> eventBusProvider,
                                                                    Provider<Provider<ExpandPlayerSubscriber>> expandPlayerSubscriberProviderProvider,
                                                                    Provider<PlaybackInitiator> playbackInitiatorProvider) {
        return new SuggestionsHelperFactory(navigatorProvider, eventBusProvider,
                expandPlayerSubscriberProviderProvider, playbackInitiatorProvider);
    }
}
