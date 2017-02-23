package com.soundcloud.android.stream;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;
import static com.soundcloud.android.events.EventQueue.TRACK_CHANGED;

import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.RepoUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.OfflinePropertiesSubscriber;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateTrackListSubscriber;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

class StreamHighlightsPresenter extends RecyclerViewPresenter<List<TrackItem>, TrackItem> {

    private final PlaybackInitiator playbackOperations;
    private final StreamHighlightsAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final TrackRepository trackRepository;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private final FeatureFlags featureFlags;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;
    private List<Urn> urns;

    @Inject
    StreamHighlightsPresenter(PlaybackInitiator playbackInitiator,
                              StreamHighlightsAdapter adapter,
                              Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider, EventBus eventBus,
                              SwipeRefreshAttacher swipeRefreshAttacher,
                              TrackRepository trackRepository,
                              OfflinePropertiesProvider offlinePropertiesProvider,
                              FeatureFlags featureFlags) {
        super(swipeRefreshAttacher);
        this.playbackOperations = playbackInitiator;
        this.adapter = adapter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
        this.trackRepository = trackRepository;
        this.offlinePropertiesProvider = offlinePropertiesProvider;
        this.featureFlags = featureFlags;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
        urns = fragment.getArguments().getParcelableArrayList(StreamHighlightsActivity.URN_ARGS);
    }

    @Override
    protected CollectionBinding<List<TrackItem>, TrackItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(getTracklistFromUrns(fragmentArgs.getParcelableArrayList(StreamHighlightsActivity.URN_ARGS)))
                                .withAdapter(adapter)
                                .build();
    }

    @Override
    protected CollectionBinding<List<TrackItem>, TrackItem> onRefreshBinding() {
        return CollectionBinding.from(getTracklistFromUrns(urns))
                                .withAdapter(adapter)
                                .build();
    }

    private Observable<List<TrackItem>> getTracklistFromUrns(List<Urn> urns) {
        return RepoUtils.enrich(Lists.transform(urns, urn -> () -> urn),
                                trackRepository.fromUrns(urns),
                                (trackItem, urnHolder) -> trackItem)
                        .map(tracks -> Lists.transform(tracks, TrackItem::from));
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        // remove the blinking whenever we notifyItemChanged
        ((DefaultItemAnimator) getRecyclerView().getItemAnimator()).setSupportsChangeAnimations(false);

        getEmptyView().setImage(R.drawable.empty_stream);
        getEmptyView().setMessageText(R.string.list_empty_stream_message);
        getEmptyView().setBackgroundResource(R.color.page_background);

        viewLifeCycle = new CompositeSubscription(

                eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM,
                                   new UpdatePlayableAdapterSubscriber(adapter)),

                subscribeToOfflineContent(),

                eventBus.subscribe(TRACK_CHANGED, new UpdateTrackListSubscriber(adapter))
        );
    }

    private Subscription subscribeToOfflineContent() {
        if (featureFlags.isEnabled(Flag.OFFLINE_PROPERTIES_PROVIDER)) {
            return offlinePropertiesProvider.states()
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(new OfflinePropertiesSubscriber<>(adapter));
        } else {
            return eventBus.queue(OFFLINE_CONTENT_CHANGED)
                           .observeOn(AndroidSchedulers.mainThread())
                           .subscribe(new UpdateCurrentDownloadSubscriber(adapter));
        }
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onItemClicked(View view, int position) {
        TrackItem trackItem = adapter.getItem(position);
        Urn initialTrack = trackItem.getUrn();
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.STREAM_HIGHLIGHTS);
        playbackOperations
                .playTracks(urns, initialTrack, position, playSessionSource)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

}