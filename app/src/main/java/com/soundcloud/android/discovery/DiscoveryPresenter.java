package com.soundcloud.android.discovery;

import static com.soundcloud.android.discovery.DiscoveryTrackingManager.SCREEN;
import static com.soundcloud.android.feedback.Feedback.LENGTH_LONG;

import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.search.SearchItemRenderer.SearchListener;
import com.soundcloud.android.stream.StreamSwipeRefreshAttacher;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.android.view.adapters.RecyclerViewParallaxer;
import com.soundcloud.android.view.snackbar.FeedbackController;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.disposables.CompositeDisposable;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.CheckResult;
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
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Inject
    DiscoveryPresenter(StreamSwipeRefreshAttacher swipeRefreshAttacher,
                       DiscoveryAdapterFactory adapterFactory,
                       DiscoveryOperations discoveryOperations,
                       Navigator navigator,
                       DiscoveryTrackingManager discoveryTrackingManager,
                       FeedbackController feedbackController) {
        super(swipeRefreshAttacher, Options.defaults());
        this.discoveryOperations = discoveryOperations;
        adapter = adapterFactory.create(this);
        this.navigator = navigator;
        this.discoveryTrackingManager = discoveryTrackingManager;
        this.feedbackController = feedbackController;
        this.swipeRefreshAttacher = swipeRefreshAttacher;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    public void onStart(Fragment fragment) {
        super.onStart(fragment);
        final Observable<SelectionItem> selectionItemObservable = adapter.selectionItemClick().doOnNext(item -> discoveryTrackingManager.trackSelectionItemClick(item, adapter.getItems()));
        disposable.add(selectionItemObservable.subscribeWith(new DefaultObserver<SelectionItem>() {
            @Override
            public void onNext(SelectionItem item) {
                selectionItemClick(fragment.getActivity(), item);
            }
        }));
    }

    @Override
    public void onStop(Fragment fragment) {
        disposable.clear();
        super.onStop(fragment);
    }

    private void selectionItemClick(Activity activity, SelectionItem selectionItem) {
        selectionItem.link()
                     .ifPresent(link -> navigator.navigateTo(NavigationTarget.forNavigation(activity,
                                                                                            link,
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

    @CheckResult
    private SingleTransformer<DiscoveryResult, List<DiscoveryCard>> handleDiscoveryResult() {
        return discoveryResult -> discoveryResult.doOnSuccess(this::showErrorMessage)
                                                 .map(DiscoveryResult::cards)
                                                 .map(this::addSearchItem);
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onSearchClicked(Context context) {
        navigator.navigateTo(NavigationTarget.forSearchAutocomplete(ViewUtils.getFragmentActivity(context), Screen.DISCOVER));
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
