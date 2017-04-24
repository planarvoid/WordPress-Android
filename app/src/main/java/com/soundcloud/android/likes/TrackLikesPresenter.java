package com.soundcloud.android.likes;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.LIKE_CHANGED;
import static com.soundcloud.android.events.EventQueue.OFFLINE_CONTENT_CHANGED;
import static com.soundcloud.android.events.EventQueue.REPOST_CHANGED;
import static com.soundcloud.android.events.EventQueue.TRACK_CHANGED;
import static com.soundcloud.android.rx.observers.LambdaSubscriber.onNext;
import static com.soundcloud.java.collections.Iterables.getLast;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.RefreshRecyclerViewAdapterSubscriber;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.LambdaSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayableAdapterSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.LikeEntityListSubscriber;
import com.soundcloud.android.view.adapters.OfflinePropertiesSubscriber;
import com.soundcloud.android.view.adapters.PrependItemToListSubscriber;
import com.soundcloud.android.view.adapters.RemoveEntityListSubscriber;
import com.soundcloud.android.view.adapters.RepostEntityListSubscriber;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateTrackListSubscriber;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

class TrackLikesPresenter extends RecyclerViewPresenter<TrackLikesPresenter.TrackLikesPage, TrackLikesItem> {

    private static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    private static final int EXTRA_LIST_ITEMS = 1;
    private final TrackLikesIntentResolver intentResolver;
    @LightCycle final TrackLikesHeaderPresenter headerPresenter;

    private final DataSource dataSource;
    private final OfflinePropertiesProvider offlinePropertiesProvider;
    private final FeatureFlags featureFlags;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final TrackLikeOperations likeOperations;
    private final PlaybackInitiator playbackOperations;
    private final OfflineContentOperations offlineContentOperations;
    private final TrackLikesAdapter adapter;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final EventBus eventBus;

    private CompositeSubscription viewLifeCycle;

    private Subscription collectionSubscription = RxUtils.invalidSubscription();
    private Subscription likeSubscription = RxUtils.invalidSubscription();

    private static final Func1<TrackLikesPage, ? extends Iterable<TrackLikesItem>> TO_TRACK_LIKES_ITEMS =
            (Func1<TrackLikesPage, Iterable<TrackLikesItem>>) page -> {
        List<TrackLikesItem> trackLikesItems = new ArrayList<>(page.getTrackLikes().size() + EXTRA_LIST_ITEMS);
        if (page.hasHeader() && !page.getTrackLikes().isEmpty()) {
            trackLikesItems.add(TrackLikesHeaderItem.create());
        }

        for (LikeWithTrack likeWithTrack : page.getTrackLikes()) {
            trackLikesItems.add(TrackLikesTrackItem.create(likeWithTrack.trackItem()));
        }
        return trackLikesItems;
    };

    @Inject
    TrackLikesPresenter(TrackLikeOperations likeOperations,
                        PlaybackInitiator playbackInitiator,
                        OfflineContentOperations offlineContentOperations,
                        TrackLikesAdapterFactory adapterFactory,
                        TrackLikesHeaderPresenter headerPresenter,
                        Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider, EventBus eventBus,
                        SwipeRefreshAttacher swipeRefreshAttacher,
                        TrackLikesIntentResolver intentResolver,
                        DataSource dataSource,
                        OfflinePropertiesProvider offlinePropertiesProvider,
                        FeatureFlags featureFlags,
                        PerformanceMetricsEngine performanceMetricsEngine) {
        super(swipeRefreshAttacher);
        this.likeOperations = likeOperations;
        this.playbackOperations = playbackInitiator;
        this.offlineContentOperations = offlineContentOperations;
        this.dataSource = dataSource;
        this.offlinePropertiesProvider = offlinePropertiesProvider;
        this.featureFlags = featureFlags;
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.adapter = adapterFactory.create(headerPresenter);
        this.headerPresenter = headerPresenter;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
        this.intentResolver = intentResolver;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected CollectionBinding<TrackLikesPage, TrackLikesItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(dataSource.initialTrackLikes(), TO_TRACK_LIKES_ITEMS)
                                .withAdapter(adapter)
                                .withPager(dataSource.pagingFunction())
                                .addObserver(onNext(o -> endMeasuringFirstPageLoadingTime()))
                                .build();
    }

    private void endMeasuringFirstPageLoadingTime() {
        performanceMetricsEngine.endMeasuring(MetricType.LIKED_TRACKS_FIRST_PAGE_LOAD);
    }

    @Override
    protected CollectionBinding<TrackLikesPage, TrackLikesItem> onRefreshBinding() {
        return CollectionBinding.from(dataSource.updatedTrackLikes(), TO_TRACK_LIKES_ITEMS)
                                .withAdapter(adapter)
                                .withPager(dataSource.pagingFunction())
                                .build();
    }

    @Override
    protected void onSubscribeBinding(CollectionBinding<TrackLikesPage, TrackLikesItem> collectionBinding,
                                      CompositeSubscription viewLifeCycle) {
        Observable<List<Urn>> allLikedTrackUrns = collectionBinding.items()
                                                                   .first()
                                                                   .flatMap(o -> likeOperations.likedTrackUrns())
                                                                   .observeOn(AndroidSchedulers.mainThread())
                                                                   .cache();
        collectionSubscription = allLikedTrackUrns
                .doOnNext(tick -> checkAutoPlayIntent())
                .map(List::size)
                .subscribe(LambdaSubscriber.onNext(headerPresenter::updateTrackCount));
    }

    private void checkAutoPlayIntent() {
        if (intentResolver.consumePlaybackRequest()) {
            onItemClicked(null, 1);
        }
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        // remove the blinking whenever we notifyItemChanged
        ((DefaultItemAnimator) getRecyclerView().getItemAnimator()).setSupportsChangeAnimations(false);

        getEmptyView().setImage(R.drawable.empty_like);
        getEmptyView().setMessageText(R.string.list_empty_you_likes_message);
        getEmptyView().setBackgroundResource(R.color.page_background);

        viewLifeCycle = new CompositeSubscription(

                eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM,
                                   new UpdatePlayableAdapterSubscriber(adapter)),

                subscribeToOfflineContent(),

                eventBus.subscribe(TRACK_CHANGED, new UpdateTrackListSubscriber(adapter)),
                eventBus.subscribe(LIKE_CHANGED, new LikeEntityListSubscriber(adapter)),
                eventBus.subscribe(REPOST_CHANGED, new RepostEntityListSubscriber(adapter)),

                likeOperations.onTrackLiked()
                              .map(TrackLikesTrackItem::create)
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new PrependItemToListSubscriber<>(adapter)),

                likeOperations.onTrackUnliked()
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new RemoveEntityListSubscriber(adapter)),

                offlineContentOperations.getOfflineContentOrOfflineLikesStatusChanges()
                                        .subscribe(new RefreshRecyclerViewAdapterSubscriber(adapter))
        );

        likeSubscription = eventBus.queue(LIKE_CHANGED)
                                   .filter(LikesStatusEvent::containsTrackChange)
                                   .flatMap(o -> likeOperations.likedTrackUrns())
                                   .map(List::size)
                                   .observeOn(AndroidSchedulers.mainThread())
                                   .subscribe(onNext(headerPresenter::updateTrackCount));
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
        // TODO create subscription light cycle
        likeSubscription.unsubscribe();
        collectionSubscription.unsubscribe();
        viewLifeCycle.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    public void onItemClicked(View view, int position) {
        // here we assume that the list you are looking at is up to date with the database,
        // which is not necessarily the case a sync may have happened in the background.
        // This is def. an edge case, but worth handling maybe??
        final TrackLikesItem item = adapter.getItem(position);

        if (item == null) {
            String exceptionMessage = "Adapter item is null on item click, with adapter: "
                    + adapter + ", on position " + position;
            ErrorUtils.handleSilentException(new IllegalStateException(exceptionMessage));
        } else if (item.isTrack()) {
            TrackItem trackItem = ((TrackLikesTrackItem) item).getTrackItem();
            Urn initialTrack = trackItem.getUrn();
            PlaySessionSource playSessionSource = new PlaySessionSource(Screen.LIKES);

            playbackOperations
                    .playTracks(likeOperations.likedTrackUrns(), initialTrack, position, playSessionSource)
                    .subscribe(expandPlayerSubscriberProvider.get());
        }
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @AutoValue
    abstract static class TrackLikesPage {

        static TrackLikesPage withHeader(List<LikeWithTrack> trackLikes) {
            return new AutoValue_TrackLikesPresenter_TrackLikesPage(trackLikes, true);
        }

        static TrackLikesPage withoutHeader(List<LikeWithTrack> trackLikes) {
            return new AutoValue_TrackLikesPresenter_TrackLikesPage(trackLikes, false);
        }

        abstract List<LikeWithTrack> getTrackLikes();

        abstract boolean hasHeader();
    }

    static class DataSource {

        private final TrackLikeOperations trackLikeOperations;

        @Inject
        DataSource(TrackLikeOperations trackLikeOperations) {
            this.trackLikeOperations = trackLikeOperations;
        }

        Observable<TrackLikesPage> initialTrackLikes() {
            return wrapLikedTracks(trackLikeOperations.likedTracks(), true);
        }

        Observable<TrackLikesPage> updatedTrackLikes() {
            return wrapLikedTracks(trackLikeOperations.updatedLikedTracks(), true);
        }


        Pager.PagingFunction<TrackLikesPage> pagingFunction() {
            return result -> {
                if (result.getTrackLikes().size() < PAGE_SIZE) {
                    return Pager.finish();
                } else {
                    final long oldestLike = getLast(result.getTrackLikes()).like().likedAt().getTime();
                    return wrapLikedTracks(trackLikeOperations.likedTracks(oldestLike), false);
                }
            };
        }

        private Observable<TrackLikesPage> wrapLikedTracks(Observable<List<LikeWithTrack>> listObservable,
                                                           final boolean hasHeader) {
            return listObservable.map(likes -> hasHeader ? TrackLikesPage.withHeader(likes)
                                                         : TrackLikesPage.withoutHeader(likes));
        }
    }

}
