package com.soundcloud.android.likes;

import static com.soundcloud.android.events.EventQueue.CURRENT_PLAY_QUEUE_ITEM;
import static com.soundcloud.android.events.EventQueue.LIKE_CHANGED;
import static com.soundcloud.android.events.EventQueue.REPOST_CHANGED;
import static com.soundcloud.android.events.EventQueue.TRACK_CHANGED;
import static com.soundcloud.android.rx.observers.LambdaSubscriber.onNext;
import static com.soundcloud.java.collections.Iterables.getLast;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Entity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.offline.OfflinePropertiesProvider;
import com.soundcloud.android.playback.ExpandPlayerSingleObserver;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.RefreshRecyclerViewAdapterObserver;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.android.rx.observers.LambdaSingleObserver;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.UpdatePlayableAdapterObserver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.OfflinePropertiesObserver;
import com.soundcloud.android.view.adapters.RepostEntityListObserver;
import com.soundcloud.android.view.adapters.UpdateTrackListObserver;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import org.jetbrains.annotations.Nullable;
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
    private final PlaybackInitiator playbackInitiator;
    private final OfflineContentOperations offlineContentOperations;
    private final TrackLikesAdapter adapter;
    private final Provider<ExpandPlayerSingleObserver> expandPlayerObserverProvider;
    private final EventBusV2 eventBus;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    private final ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    private CompositeDisposable viewLifeCycle;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Disposable collectionDisposable = RxUtils.invalidDisposable();
    private Disposable likeDisposable = RxUtils.invalidDisposable();

    private static final Function<TrackLikesPage, ? extends Iterable<TrackLikesItem>> TO_TRACK_LIKES_ITEMS =
            (Function<TrackLikesPage, Iterable<TrackLikesItem>>) page -> {
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
                        Provider<ExpandPlayerSingleObserver> expandPlayerObserverProvider,
                        EventBusV2 eventBus,
                        SwipeRefreshAttacher swipeRefreshAttacher,
                        TrackLikesIntentResolver intentResolver,
                        DataSource dataSource,
                        OfflinePropertiesProvider offlinePropertiesProvider,
                        FeatureFlags featureFlags,
                        PerformanceMetricsEngine performanceMetricsEngine,
                        ChangeLikeToSaveExperiment changeLikeToSaveExperiment,
                        ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper) {
        super(swipeRefreshAttacher);
        this.likeOperations = likeOperations;
        this.playbackInitiator = playbackInitiator;
        this.offlineContentOperations = offlineContentOperations;
        this.dataSource = dataSource;
        this.offlinePropertiesProvider = offlinePropertiesProvider;
        this.featureFlags = featureFlags;
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
        this.changeLikeToSaveExperimentStringHelper = changeLikeToSaveExperimentStringHelper;
        this.adapter = adapterFactory.create(headerPresenter);
        this.headerPresenter = headerPresenter;
        this.expandPlayerObserverProvider = expandPlayerObserverProvider;
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
        return CollectionBinding.fromV2(dataSource.initialTrackLikes(), TO_TRACK_LIKES_ITEMS)
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
        return CollectionBinding.fromV2(dataSource.updatedTrackLikes(), TO_TRACK_LIKES_ITEMS)
                                .withAdapter(adapter)
                                .withPager(dataSource.pagingFunction())
                                .build();
    }

    @Override
    protected void onSubscribeBinding(CollectionBinding<TrackLikesPage, TrackLikesItem> collectionBinding,
                                      CompositeSubscription viewLifeCycle) {
        Single<List<Urn>> allLikedTrackUrns = RxJava.toV2Single(collectionBinding.items())
                                                    .flatMap(o -> likeOperations.likedTrackUrns())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .cache();
        collectionDisposable = allLikedTrackUrns.doOnSuccess(tick -> checkAutoPlayIntent())
                                                .map(List::size)
                                                .subscribeWith(LambdaSingleObserver.onNext(headerPresenter::updateTrackCount));
    }

    private void checkAutoPlayIntent() {
        if (intentResolver.consumePlaybackRequest()) {
            if (adapter.getItemCount() > 0) {
                onItemClicked(null, 1);
            }
        }
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);

        // remove the blinking whenever we notifyItemChanged
        ((DefaultItemAnimator) getRecyclerView().getItemAnimator()).setSupportsChangeAnimations(false);

        getEmptyView().setImage(changeLikeToSaveExperiment.isEnabled()
                                ? R.drawable.empty_tracks_added
                                : R.drawable.empty_like);
        getEmptyView().setMessageText(changeLikeToSaveExperimentStringHelper.getStringResId(ExperimentString.LIST_EMPTY_YOU_LIKES_MESSAGE));
        getEmptyView().setBackgroundResource(R.color.page_background);

        viewLifeCycle = new CompositeDisposable(

                eventBus.subscribe(CURRENT_PLAY_QUEUE_ITEM,
                                   new UpdatePlayableAdapterObserver(adapter)),

                subscribeToOfflineContent(),

                eventBus.subscribe(TRACK_CHANGED, new UpdateTrackListObserver(adapter)),
                eventBus.subscribe(REPOST_CHANGED, new RepostEntityListObserver(adapter)),

                likeOperations.onTrackLiked()
                              .map(TrackLikesTrackItem::create)
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribeWith(new AddLikedTrackObserver(adapter)),

                likeOperations.onTrackUnliked()
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribeWith(new RemoveLikedTrackObserver(adapter)),

                offlineContentOperations.getOfflineContentOrOfflineLikesStatusChanges().subscribeWith(new RefreshRecyclerViewAdapterObserver(adapter))
        );

        likeDisposable = eventBus.queue(LIKE_CHANGED)
                                 .filter(LikesStatusEvent::containsTrackChange)
                                 .flatMapSingle(o -> likeOperations.likedTrackUrns())
                                 .map(List::size)
                                 .observeOn(AndroidSchedulers.mainThread())
                                 .subscribeWith(LambdaObserver.onNext(headerPresenter::updateTrackCount));
    }

    private Disposable subscribeToOfflineContent() {
        return offlinePropertiesProvider.states()
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribeWith(new OfflinePropertiesObserver<>(adapter));
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        compositeDisposable.clear();
        likeDisposable.dispose();
        collectionDisposable.dispose();
        viewLifeCycle.clear();
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

            compositeDisposable.add(playbackInitiator.playTracks(likeOperations.likedTrackUrns(), initialTrack, position, playSessionSource)
                                                     .subscribeWith(expandPlayerObserverProvider.get()));
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

        Single<TrackLikesPage> initialTrackLikes() {
            return wrapLikedTracks(trackLikeOperations.likedTracks(), true);
        }

        Single<TrackLikesPage> updatedTrackLikes() {
            return wrapLikedTracks(trackLikeOperations.updatedLikedTracks(), true);
        }

        Pager.PagingFunction<TrackLikesPage> pagingFunction() {
            return result -> {
                if (result.getTrackLikes().isEmpty()) {
                    return Pager.finish();
                } else {
                    final long oldestLike = getLast(result.getTrackLikes()).like().likedAt().getTime();
                    return RxJava.toV1Observable(wrapLikedTracks(trackLikeOperations.likedTracks(oldestLike), false));
                }
            };
        }

        private Single<TrackLikesPage> wrapLikedTracks(Single<List<LikeWithTrack>> listObservable,
                                                       final boolean hasHeader) {
            return listObservable.map(likes -> hasHeader ? TrackLikesPage.withHeader(likes)
                                                         : TrackLikesPage.withoutHeader(likes));
        }
    }

    private static class AddLikedTrackObserver extends DefaultObserver<TrackLikesTrackItem> {
        private final TrackLikesAdapter adapter;

        public AddLikedTrackObserver(TrackLikesAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onNext(@NonNull TrackLikesTrackItem trackLikesTrackItem) {
            final TrackLikesItem firstItem = adapter.getItem(0);
            int targetPosition = isHeader(firstItem) ? 1 : 0;
            adapter.addItem(targetPosition, trackLikesTrackItem);
            adapter.notifyItemInserted(targetPosition);
        }

        private boolean isHeader(TrackLikesItem firstItem) {
            return firstItem != null && firstItem.getKind() == TrackLikesItem.Kind.HeaderItem;
        }
    }

    private static final class RemoveLikedTrackObserver extends DefaultObserver<Urn> {
        private final TrackLikesAdapter adapter;

        public RemoveLikedTrackObserver(TrackLikesAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public void onNext(final Urn urn) {
            final List<TrackLikesItem> items = adapter.getItems();
            for (int i = 0; i < items.size(); i++) {
                if (shouldRemove(items.get(i), urn)) {
                    removeItemFromAdapterAt(i);
                    break; //likes list should not contain duplications
                }
            }
        }

        private boolean shouldRemove(Object item, Urn urn) {
            return item != null && item instanceof Entity && ((Entity) item).getUrn().equals(urn);
        }

        private void removeItemFromAdapterAt(int position) {
            adapter.removeItem(position);
            adapter.notifyItemRemoved(position);
        }
    }
}
