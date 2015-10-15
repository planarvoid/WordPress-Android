package com.soundcloud.android.discovery;

import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;
import static com.soundcloud.android.events.EventQueue.PLAY_QUEUE_TRACK;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.TracksRecyclerItemAdapter;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

class RecommendedTracksPresenter extends RecyclerViewPresenter<TrackItem> {

    static final String EXTRA_LOCAL_SEED_ID = "localSeedId";

    private final DiscoveryOperations discoveryOperations;
    private final TracksRecyclerItemAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaybackInitiator playbackInitiator;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;

    @Inject
    RecommendedTracksPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                               DiscoveryOperations discoveryOperations,
                               TracksRecyclerItemAdapter adapter,
                               Provider<ExpandPlayerSubscriber> subscriberProvider,
                               PlaybackInitiator playbackInitiator, EventBus eventBus) {
        super(swipeRefreshAttacher, Options.defaults());
        this.discoveryOperations = discoveryOperations;
        this.adapter = adapter;
        this.expandPlayerSubscriberProvider = subscriberProvider;
        this.playbackInitiator = playbackInitiator;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        viewLifeCycle = new CompositeSubscription(
                eventBus.subscribe(PLAY_QUEUE_TRACK,
                        new UpdatePlayingTrackSubscriber(adapter, adapter.getTrackRenderer())),
                eventBus.subscribe(ENTITY_STATE_CHANGED,
                        new UpdateEntityListSubscriber(adapter)));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    protected void onItemClicked(View view, int position) {
        playRecommendedTracks(adapter.getItem(position).getEntityUrn(), discoveryOperations.recommendedTracks());
    }

    @Override
    protected CollectionBinding<TrackItem> onBuildBinding(Bundle bundle) {
        final long localSeedId = bundle.getLong(EXTRA_LOCAL_SEED_ID);
        return CollectionBinding.from(discoveryOperations.recommendedTracksForSeed(localSeedId))
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    private void playRecommendedTracks(Urn firstTrackUrn, Observable<List<Urn>> playQueue) {
        final int incorrectPosition = 0; // https://github.com/soundcloud/SoundCloud-Android/issues/3705
        playbackInitiator.playTracks(playQueue, firstTrackUrn, incorrectPosition,
                new PlaySessionSource(Screen.RECOMMENDATIONS_MORE)).subscribe(expandPlayerSubscriberProvider.get());
    }
}
