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
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ViewUtils;
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
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class DiscoveryPresenter extends RecyclerViewPresenter<List<DiscoveryCard>, DiscoveryCard> implements SearchListener {

    private final DiscoveryOperations discoveryOperations;
    private final DiscoveryAdapter adapter;
    private final Navigator navigator;
    private final DiscoveryTrackingManager discoveryTrackingManager;
    private final FeedbackController feedbackController;
    private final StreamSwipeRefreshAttacher swipeRefreshAttacher;
    private final EventTracker eventTracker;
    private final ReferringEventProvider referringEventProvider;
    private final CompositeDisposable disposable = new CompositeDisposable();
    private final BehaviorSubject<Optional<Urn>> queryUrn = BehaviorSubject.create();

    @Inject
    DiscoveryPresenter(StreamSwipeRefreshAttacher swipeRefreshAttacher,
                       DiscoveryAdapterFactory adapterFactory,
                       DiscoveryOperations discoveryOperations,
                       Navigator navigator,
                       DiscoveryTrackingManager discoveryTrackingManager,
                       FeedbackController feedbackController,
                       EventTracker eventTracker,
                       ReferringEventProvider referringEventProvider) {
        super(swipeRefreshAttacher, Options.defaults());
        this.discoveryOperations = discoveryOperations;
        adapter = adapterFactory.create(this);
        this.navigator = navigator;
        this.discoveryTrackingManager = discoveryTrackingManager;
        this.feedbackController = feedbackController;
        this.swipeRefreshAttacher = swipeRefreshAttacher;
        this.eventTracker = eventTracker;
        this.referringEventProvider = referringEventProvider;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();

        final Observable<SelectionItem> selectionItemObservable = adapter.selectionItemClick().doOnNext(item -> discoveryTrackingManager.trackSelectionItemClick(item, adapter.getItems()));
        disposable.add(selectionItemObservable.subscribeWith(LambdaObserver.onNext(item -> selectionItemClick(fragment.getActivity(), item))));
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

    private void selectionItemClick(Activity activity, SelectionItem selectionItem) {
        selectionItem.link()
                     .ifPresent(link -> navigator.navigateTo(activity, NavigationTarget.forNavigation(link,
                                                                                                      selectionItem.webLink(),
                                                                                                      SCREEN,
                                                                                                      Optional.of(DiscoverySource.RECOMMENDATIONS)))); // TODO (REC-1302): Use correct one))));
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onViewCreated(fragment, view, savedInstanceState);
        getRecyclerView().addOnScrollListener(new RecyclerViewParallaxer());
    }

    @Override
    protected CollectionBinding<List<DiscoveryCard>, DiscoveryCard> onBuildBinding(Bundle bundle) {
        Single<List<DiscoveryCard>> dataSource = discoveryOperations.discoveryCards().compose(handleDiscoveryResult());
        return CollectionBinding
                .fromV2(dataSource.toObservable())
                .withAdapter(adapter)
                .build();
    }

    private List<DiscoveryCard> addSearchItem(List<DiscoveryCard> discoveryCards) {
        final List<DiscoveryCard> result = new ArrayList<>(discoveryCards.size() + 1);
        result.add(DiscoveryCard.forSearchItem());
        result.addAll(discoveryCards);
        return result;
    }

    @Override
    protected CollectionBinding<List<DiscoveryCard>, DiscoveryCard> onRefreshBinding() {
        Single<List<DiscoveryCard>> dataSource = discoveryOperations.refreshDiscoveryCards().compose(handleDiscoveryResult());
        return CollectionBinding
                .fromV2(dataSource.toObservable())
                .withAdapter(adapter)
                .build();
    }

    private SingleTransformer<DiscoveryResult, List<DiscoveryCard>> handleDiscoveryResult() {
        return discoveryResult -> discoveryResult.doOnSuccess(this::showErrorMessage)
                                                 .map(DiscoveryResult::cards)
                                                 .doOnSuccess(this::emitQueryUrn)
                                                 .map(this::addSearchItem);
    }

    private void emitQueryUrn(List<DiscoveryCard> discoveryCards) {
        if (!discoveryCards.isEmpty()) {
            queryUrn.onNext(discoveryCards.get(0).parentQueryUrn());
        }
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onSearchClicked(Context context) {
        navigator.navigateTo(ViewUtils.getFragmentActivity(context), NavigationTarget.forSearchAutocomplete(Screen.DISCOVER));
    }

    private void showErrorMessage(DiscoveryResult discoveryResult) {
        discoveryResult.syncError().ifPresent(syncError -> {
            if (syncError == ViewError.CONNECTION_ERROR) {
                feedbackController.showFeedback(Feedback.create(R.string.discovery_error_offline, LENGTH_LONG));
            } else if (syncError == ViewError.SERVER_ERROR) {
                feedbackController.showFeedback(Feedback.create(R.string.discovery_error_failed_to_load, R.string.discovery_error_retry_button, v -> swipeRefreshAttacher.forceRefresh()));
            }
        });
    }
}
