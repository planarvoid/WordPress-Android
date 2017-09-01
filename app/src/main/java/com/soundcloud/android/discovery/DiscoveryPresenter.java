package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryTrackingManager.SCREEN;
import static com.soundcloud.android.feedback.Feedback.LENGTH_LONG;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ReferringEventProvider;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.rx.observers.LambdaObserver;
import com.soundcloud.android.search.SearchItemRenderer.SearchListener;
import com.soundcloud.android.stream.StreamSwipeRefreshAttacher;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.android.view.adapters.RecyclerViewParallaxer;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;

class DiscoveryPresenter extends RecyclerViewPresenter<List<DiscoveryCardViewModel>, DiscoveryCardViewModel> implements SearchListener {

    private final DiscoveryOperations discoveryOperations;
    private final DiscoveryAdapter adapter;
    private final Navigator navigator;
    private final FeedbackController feedbackController;
    private final StreamSwipeRefreshAttacher swipeRefreshAttacher;
    private final EventTracker eventTracker;
    private final ReferringEventProvider referringEventProvider;
    private final SyncStateStorage syncStateStorage;
    private final CompositeDisposable disposable = new CompositeDisposable();
    private final BehaviorSubject<Optional<Urn>> queryUrn = BehaviorSubject.create();

    @Inject
    DiscoveryPresenter(StreamSwipeRefreshAttacher swipeRefreshAttacher,
                       DiscoveryAdapter.Factory adapterFactory,
                       DiscoveryOperations discoveryOperations,
                       Navigator navigator,
                       FeedbackController feedbackController,
                       EventTracker eventTracker,
                       ReferringEventProvider referringEventProvider,
                       SyncStateStorage syncStateStorage) {
        super(swipeRefreshAttacher, Options.defaults());
        this.discoveryOperations = discoveryOperations;
        adapter = adapterFactory.create(this);
        this.navigator = navigator;
        this.feedbackController = feedbackController;
        this.swipeRefreshAttacher = swipeRefreshAttacher;
        this.eventTracker = eventTracker;
        this.referringEventProvider = referringEventProvider;
        this.syncStateStorage = syncStateStorage;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();

        // reset our backoff. This is a cheap way of doing it for now.
        syncStateStorage.resetSyncMisses(Syncable.DISCOVERY_CARDS);

        final Observable<SelectionItemViewModel> selectionItemObservable = adapter.selectionItemClick()
                                                                                  .doOnNext(item -> item.getTrackingInfo()
                                                                                                        .ifPresent(trackingInfo -> eventTracker.trackClick(trackingInfo.toUIEvent())));
        disposable.add(selectionItemObservable.subscribeWith(LambdaObserver.onNext(this::selectionItemClick)));
        disposable.add(Observable.combineLatest(((RootActivity) fragment.getActivity()).enterScreenTimestamp(), queryUrn, Pair::of)
                                 .distinctUntilChanged(Pair::first)
                                 .subscribeWith(LambdaObserver.onNext(pair -> this.trackPageView(pair.second()))));
    }

    private void trackPageView(Optional<Urn> queryUrn) {
        eventTracker.trackScreen(ScreenEvent.create(Screen.DISCOVER.get(), queryUrn), referringEventProvider.getReferringEvent());
    }

    @Override
    public void onDestroy(Fragment fragment) {
        disposable.clear();
        super.onDestroy(fragment);
    }

    private void selectionItemClick(SelectionItemViewModel selectionItem) {
        selectionItem.link()
                     .ifPresent(link -> navigator.navigateTo(NavigationTarget.forNavigation(link,
                                                                                            selectionItem.getWebLink(),
                                                                                            SCREEN,
                                                                                            Optional.of(DiscoverySource.RECOMMENDATIONS)))); // TODO (REC-1302): Use correct one))));
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        getRecyclerView().addOnScrollListener(new RecyclerViewParallaxer());
    }

    @Override
    protected CollectionBinding<List<DiscoveryCardViewModel>, DiscoveryCardViewModel> onBuildBinding(Bundle bundle) {
        Single<List<DiscoveryCardViewModel>> dataSource = discoveryOperations.discoveryCards().compose(handleDiscoveryResult());
        return CollectionBinding
                .fromV2(dataSource.toObservable())
                .withAdapter(adapter)
                .build();
    }

    @Override
    protected CollectionBinding<List<DiscoveryCardViewModel>, DiscoveryCardViewModel> onRefreshBinding() {
        Single<List<DiscoveryCardViewModel>> dataSource = discoveryOperations.refreshDiscoveryCards().compose(handleDiscoveryResult());
        return CollectionBinding
                .fromV2(dataSource.toObservable())
                .withAdapter(adapter)
                .build();
    }

    private SingleTransformer<DiscoveryResult, List<DiscoveryCardViewModel>> handleDiscoveryResult() {
        return discoveryResult -> discoveryResult.doOnSuccess(this::showErrorMessage)
                                                 .doOnSuccess(this::emitQueryUrn)
                                                 .map(HomePresenterKt::toViewModel);
    }

    private void emitQueryUrn(DiscoveryResult discoveryResult) {
        if (!discoveryResult.getCards().isEmpty()) {
            queryUrn.onNext(discoveryResult.getCards().get(0).parentQueryUrn());
        }
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onSearchClicked(Context context) {
        navigator.navigateTo(NavigationTarget.forSearchAutocomplete(Screen.DISCOVER));
    }

    private void showErrorMessage(DiscoveryResult discoveryResult) {
        discoveryResult.getSyncError().ifPresent(syncError -> {
            if (syncError == ViewError.CONNECTION_ERROR) {
                feedbackController.showFeedback(Feedback.create(R.string.discovery_error_offline, LENGTH_LONG));
            } else if (syncError == ViewError.SERVER_ERROR) {
                feedbackController.showFeedback(Feedback.create(R.string.discovery_error_failed_to_load, R.string.discovery_error_retry_button, v -> swipeRefreshAttacher.forceRefresh()));
            }
        });
    }
}
