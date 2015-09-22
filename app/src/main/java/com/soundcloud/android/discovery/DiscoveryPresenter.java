package com.soundcloud.android.discovery;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.search.PlaylistTagsPresenter;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import android.app.Activity;
import android.content.Context;
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

    @Nullable private PlaylistTagsPresenter.Listener tagsListener;

    @Inject
    DiscoveryPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                       DiscoveryOperations discoveryOperations,
                       DiscoveryAdapter adapter,
                       Provider<ExpandPlayerSubscriber> subscriberProvider,
                       PlaybackInitiator playbackInitiator,
                       Navigator navigator) {
        super(swipeRefreshAttacher, Options.cards());
        this.discoveryOperations = discoveryOperations;
        this.adapter = adapter;
        this.expandPlayerSubscriberProvider = subscriberProvider;
        this.playbackInitiator = playbackInitiator;
        this.navigator = navigator;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onAttach(Fragment fragment, Activity activity) {
        super.onAttach(fragment, activity);
        checkArgument(activity instanceof PlaylistTagsPresenter.Listener, "Host activity must be a " + PlaylistTagsPresenter.Listener.class);
        this.tagsListener = ((PlaylistTagsPresenter.Listener) activity);
    }

    @Override
    public void onDetach(Fragment fragment) {
        this.tagsListener = null;
        super.onDetach(fragment);
    }

    @Override
    public void onTagSelected(String tag) {
        if (tagsListener != null) {
            tagsListener.onTagSelected(tag);
        }
    }

    @Override
    protected CollectionBinding<DiscoveryItem> onBuildBinding(Bundle bundle) {
        adapter.setOnRecommendationClickListener(this);
        return CollectionBinding.from(discoveryOperations.recommendationsAndPlaylistDiscovery())
                .withAdapter(adapter).build();
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

    private void playRecommendations(Urn firstTrackUrn, Observable<List<Urn>> playQueue) {
        playbackInitiator.playTracks(playQueue, firstTrackUrn, 0,
                new PlaySessionSource(Screen.RECOMMENDATIONS_MAIN)).subscribe(expandPlayerSubscriberProvider.get());
    }
}
