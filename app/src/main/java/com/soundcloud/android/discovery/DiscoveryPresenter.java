package com.soundcloud.android.discovery;

import static com.soundcloud.java.collections.Iterables.concat;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;
import static java.util.Collections.singleton;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.RecyclerViewParallaxer;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

class DiscoveryPresenter extends RecyclerViewPresenter<List<DiscoveryItem>, DiscoveryItem> implements DiscoveryAdapter.DiscoveryItemListenerBucket {

    private final DiscoveryOperations discoveryOperations;
    private final DiscoveryAdapter adapter;
    private final ImagePauseOnScrollListener imagePauseOnScrollListener;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaybackInitiator playbackInitiator;
    private final Navigator navigator;
    private final FeatureFlags featureFlags;

    @Inject
    DiscoveryPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                       DiscoveryOperations discoveryOperations,
                       DiscoveryAdapter adapter,
                       ImagePauseOnScrollListener imagePauseOnScrollListener,
                       Provider<ExpandPlayerSubscriber> subscriberProvider,
                       PlaybackInitiator playbackInitiator,
                       Navigator navigator,
                       FeatureFlags featureFlags) {
        super(swipeRefreshAttacher, Options.defaults());
        this.discoveryOperations = discoveryOperations;
        this.adapter = adapter;
        this.imagePauseOnScrollListener = imagePauseOnScrollListener;
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
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        addScrollListeners();
    }

    @Override
    public void onSearchClicked(Context context) {
        navigator.openSearch((Activity) context);
    }

    @Override
    public void onTagSelected(Context context, String tag) {
        navigator.openPlaylistDiscoveryTag(context, tag);
    }

    @Override
    public void onReasonClicked(RecommendationBucket recommendationBucket) {
        playTracks(recommendationBucket, recommendationBucket.getSeedTrackUrn());
    }

    @Override
    public void onRecommendationClicked(RecommendationBucket recommendationBucket, TrackItem recommendation) {
        playTracks(recommendationBucket, recommendation.getUrn());
    }

    private void playTracks(RecommendationBucket recommendationBucket, Urn urnToPlay) {
        List<Urn> trackUrnsToPlay = buildPlayQueue(recommendationBucket);
        int startPosition = trackUrnsToPlay.indexOf(urnToPlay);
        playbackInitiator.playTracks(trackUrnsToPlay, startPosition, new PlaySessionSource(Screen.RECOMMENDATIONS_MAIN)).subscribe(expandPlayerSubscriberProvider.get());
    }

    @Override
    public void onViewAllClicked() {
        // TODO: Implement in a future story where we support the full list of recommendations
    }

    @NonNull
    @SuppressWarnings("unchecked")
    private List<Urn> buildPlayQueue(RecommendationBucket recommendationBucket) {
        Iterable<Urn> seedUrn = singleton(recommendationBucket.getSeedTrackUrn());
        Iterable<Urn> recommendationUrns = transform(recommendationBucket.getRecommendations(), PlayableItem.TO_URN);
        return newArrayList(concat(seedUrn, recommendationUrns));
    }

    @Override
    protected CollectionBinding<List<DiscoveryItem>, DiscoveryItem> onBuildBinding(Bundle bundle) {
        adapter.setDiscoveryListener(this);
        return CollectionBinding
                .from(discoveryOperations.discoveryItems())
                .withAdapter(adapter).build();
    }

    private void addScrollListeners() {
        getRecyclerView().addOnScrollListener(imagePauseOnScrollListener);
        if (featureFlags.isEnabled(Flag.DISCOVERY_RECOMMENDATIONS)) {
            getRecyclerView().addOnScrollListener(new RecyclerViewParallaxer());
        }
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

}
