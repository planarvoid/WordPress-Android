package com.soundcloud.android.discovery;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.search.PlaylistTagsPresenter;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.lightcycle.LightCycle;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

public class DiscoveryPresenter extends RecyclerViewPresenter<DiscoveryItem> implements DiscoveryAdapter.DiscoveryItemListener {

    final @LightCycle DiscoveryView discoveryView;

    private final DiscoveryOperations discoveryOperations;
    private final DiscoveryAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaybackOperations playbackOperations;
    private final Navigator navigator;

    @Nullable private PlaylistTagsPresenter.Listener tagsListener;

    @Inject
    DiscoveryPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                       DiscoveryOperations discoveryOperations,
                       DiscoveryView discoveryView,
                       DiscoveryAdapter adapter,
                       Provider<ExpandPlayerSubscriber> subscriberProvider,
                       PlaybackOperations playbackOperations,
                       Navigator navigator) {
        super(swipeRefreshAttacher, Options.cards());
        this.discoveryOperations = discoveryOperations;
        this.discoveryView = discoveryView;
        this.adapter = adapter;
        this.expandPlayerSubscriberProvider = subscriberProvider;
        this.playbackOperations = playbackOperations;
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
        if (tagsListener != null){
            tagsListener.onTagSelected(tag);
        }
    }

    @Override
    protected void onItemClicked(View view, int position) {
        //TODO: make it not abstract on android-kit since there is no-op here.
    }

    @Override
    protected CollectionBinding<DiscoveryItem> onBuildBinding(Bundle bundle) {
        adapter.setOnRecommendationClickListener(this);
        return CollectionBinding.from(discoveryOperations.recommendationsAndPlaylistDiscovery())
                .withAdapter(adapter).build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable throwable) {
        return null;
    }

    @Override
    public void onRecommendationReasonClicked(RecommendationItem recommendationItem) {
        playRecommendations(recommendationItem.getSeedTrackUrn(), discoveryOperations.recommendationsWithSeedTrack(recommendationItem.getSeedTrackLocalId(), recommendationItem.getSeedTrackUrn()));
    }

    @Override
    public void onRecommendationArtworkClicked(RecommendationItem recommendationItem) {
        playRecommendations(recommendationItem.getRecommendationUrn(), discoveryOperations.recommendationsForSeedTrack(recommendationItem.getSeedTrackLocalId()));

    }

    @Override
    public void onRecommendationViewAllClicked(Context context, RecommendationItem recommendationItem) {
        navigator.openRecommendation(context, recommendationItem.getSeedTrackLocalId());
    }

    private void playRecommendations(Urn firstTrackUrn, Observable<List<Urn>> playQueue) {
        playbackOperations.playTracks(playQueue, firstTrackUrn, 0,
                new PlaySessionSource(Screen.RECOMMENDATIONS_MAIN)).subscribe(expandPlayerSubscriberProvider.get());
    }
}
