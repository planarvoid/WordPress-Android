package com.soundcloud.android.activities;

import static com.soundcloud.android.rx.observers.LambdaSubscriber.onNext;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.observers.LambdaMaybeObserver;
import com.soundcloud.android.sync.timeline.TimelinePresenter;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.NewItemsIndicator;
import com.soundcloud.java.optional.Optional;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class ActivitiesPresenter extends TimelinePresenter<ActivityItem> {

    private final ActivitiesOperations operations;
    private final ActivitiesAdapter adapter;
    private final TrackRepository trackRepository;
    private final NavigationExecutor navigationExecutor;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final Navigator navigator;
    private Disposable trackDisposable = Disposables.disposed();

    @Inject
    ActivitiesPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                        ActivitiesOperations operations,
                        ActivitiesAdapter adapter,
                        TrackRepository trackRepository,
                        NavigationExecutor navigationExecutor,
                        NewItemsIndicator newItemsIndicator,
                        PerformanceMetricsEngine performanceMetricsEngine,
                        Navigator navigator) {
        super(swipeRefreshAttacher, RecyclerViewPresenter.Options.list().build(),
              newItemsIndicator, operations, adapter);
        this.operations = operations;
        this.adapter = adapter;
        this.trackRepository = trackRepository;
        this.navigationExecutor = navigationExecutor;
        this.performanceMetricsEngine = performanceMetricsEngine;
        this.navigator = navigator;
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onCreateCollectionView(fragment, view, savedInstanceState);
        final EmptyView emptyView = getEmptyView();
        emptyView.setImage(R.drawable.empty_activity);
        emptyView.setMessageText(R.string.list_empty_notification_message);
        emptyView.setSecondaryText(R.string.list_empty_notification_secondary);
    }

    @Override
    protected CollectionBinding<List<ActivityItem>, ActivityItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.fromV2(operations.initialActivities().toObservable())
                                .withAdapter(adapter)
                                .withPager(operations.pagingFunction())
                                .addObserver(onNext(o -> endMeasuringLoadingTime()))
                                .build();
    }

    private void endMeasuringLoadingTime() {
        performanceMetricsEngine.endMeasuring(MetricType.ACTIVITIES_LOAD);
    }

    @Override
    protected CollectionBinding<List<ActivityItem>, ActivityItem> onRefreshBinding() {
        return CollectionBinding.fromV2(operations.updatedActivities().toObservable())
                                .withAdapter(adapter)
                                .withPager(operations.pagingFunction())
                                .build();
    }

    @Override
    protected EmptyView.Status handleError(Throwable error) {
        return ErrorUtils.emptyViewStatusFromError(error);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        trackDisposable.dispose();
        super.onDestroyView(fragment);
    }

    @Override
    protected void onItemClicked(final View view, int position) {
        final ActivityItem item = adapter.getItem(position);
        final Optional<Urn> commentedTrackUrn = item.getCommentedTrackUrn();
        if (commentedTrackUrn.isPresent()) {
            // for track comments we go to the comments screen
            final Urn trackUrn = commentedTrackUrn.get();
            trackDisposable = trackRepository.track(trackUrn).subscribeWith(LambdaMaybeObserver.onNext(track -> navigationExecutor.openTrackComments(view.getContext(), trackUrn)));
        } else {
            // in all other cases we simply go to the user profile
            navigator.navigateTo(ViewUtils.getFragmentActivity(view), NavigationTarget.forProfile(item.getUrn()));
        }
    }

    @Override
    public int getNewItemsTextResourceId() {
        return R.plurals.activities_new_notification;
    }
}
