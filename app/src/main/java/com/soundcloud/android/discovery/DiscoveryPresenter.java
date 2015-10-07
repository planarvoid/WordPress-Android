package com.soundcloud.android.discovery;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

class DiscoveryPresenter extends RecyclerViewPresenter<DiscoveryItem> implements DiscoveryAdapter.DiscoveryItemListener {

    private final DiscoveryOperations discoveryOperations;
    private final DiscoveryAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaybackInitiator playbackInitiator;
    private final Navigator navigator;
    private final FeatureFlags featureFlags;

    @Inject
    DiscoveryPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                       DiscoveryOperations discoveryOperations,
                       DiscoveryAdapter adapter,
                       Provider<ExpandPlayerSubscriber> subscriberProvider,
                       PlaybackInitiator playbackInitiator,
                       Navigator navigator, FeatureFlags featureFlags) {
        super(swipeRefreshAttacher, Options.cards());
        this.discoveryOperations = discoveryOperations;
        this.adapter = adapter;
        this.expandPlayerSubscriberProvider = subscriberProvider;
        this.playbackInitiator = playbackInitiator;
        this.navigator = navigator;
        this.featureFlags = featureFlags;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onTagSelected(Context context, String tag) {
        navigator.openPlaylistDiscoveryTag(context, tag);
    }

    @Override
    protected CollectionBinding<DiscoveryItem> onBuildBinding(Bundle bundle) {
        adapter.setDiscoveryListener(this);
        return CollectionBinding.from(buildDiscoveryItemsObservable())
                .withAdapter(adapter).build();
    }

    private Observable<List<DiscoveryItem>> buildDiscoveryItemsObservable() {
        if (featureFlags.isEnabled(Flag.DISCOVERY_RECOMMENDATIONS)) {
            return discoveryOperations.discoveryItemsAndRecommendations();
        } else {
            return discoveryOperations.discoveryItems();
        }
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onRecommendationReasonClicked(RecommendationItem recommendationItem) {
        playRecommendations(recommendationItem.getSeedTrackUrn(), discoveryOperations.recommendedTracksWithSeed(recommendationItem));
    }

    @Override
    public void onRecommendationArtworkClicked(RecommendationItem recommendationItem) {
        playRecommendations(recommendationItem.getRecommendationUrn(), discoveryOperations.recommendedTracks());
    }

    @Override
    public void onRecommendationViewAllClicked(Context context, RecommendationItem recommendationItem) {
        navigator.openRecommendation(context, recommendationItem.getSeedTrackLocalId());
    }

    @Override
    public void onSearchTextPerformed(Context context, String query) {
        navigator.openSearchResults(context, query);
    }

    @Override
    public void onLaunchSearchSuggestion(Context context, Urn urn, SearchQuerySourceInfo searchQuerySourceInfo, Uri itemUri) {
        if (urn.isTrack()) {
            playSearchSuggestedTrack(urn, searchQuerySourceInfo);
        } else {
            navigator.launchSearchSuggestion(context, urn, searchQuerySourceInfo, itemUri);
        }
    }

    private void playRecommendations(Urn firstTrackUrn, Observable<List<Urn>> playQueue) {
        playbackInitiator.playTracks(playQueue, firstTrackUrn, 0,
                new PlaySessionSource(Screen.RECOMMENDATIONS_MAIN)).subscribe(expandPlayerSubscriberProvider.get());
    }

    private void playSearchSuggestedTrack(Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        playbackInitiator.startPlaybackWithRecommendations(urn, Screen.SEARCH_SUGGESTIONS, searchQuerySourceInfo)
                .subscribe(expandPlayerSubscriberProvider.get());
    }
}
