package com.soundcloud.android.activities;

import static com.soundcloud.android.rx.observers.LambdaSubscriber.onNext;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.sync.timeline.TimelinePresenter;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.NewItemsIndicator;
import com.soundcloud.java.optional.Optional;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

class ActivitiesPresenter extends TimelinePresenter<ActivityItem> {

    private final ActivitiesOperations operations;
    private final ActivitiesAdapter adapter;
    private final TrackRepository trackRepository;
    private final Navigator navigator;
    private final PerformanceMetricsEngine performanceMetricsEngine;
    private Subscription trackSubscription = RxUtils.invalidSubscription();

    @Inject
    ActivitiesPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                        ActivitiesOperations operations,
                        ActivitiesAdapter adapter,
                        TrackRepository trackRepository,
                        Navigator navigator,
                        NewItemsIndicator newItemsIndicator,
                        PerformanceMetricsEngine performanceMetricsEngine) {
        super(swipeRefreshAttacher, RecyclerViewPresenter.Options.list().build(),
              newItemsIndicator, operations, adapter);
        this.operations = operations;
        this.adapter = adapter;
        this.trackRepository = trackRepository;
        this.navigator = navigator;
        this.performanceMetricsEngine = performanceMetricsEngine;
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
        return CollectionBinding.from(RxJava.toV1Observable(operations.initialActivities().toObservable()))
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
        return CollectionBinding.from(RxJava.toV1Observable(operations.updatedActivities().toObservable()))
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
        trackSubscription.unsubscribe();
        super.onDestroyView(fragment);
    }

    @Override
    protected void onItemClicked(final View view, int position) {
        final ActivityItem item = adapter.getItem(position);
        final Optional<Urn> commentedTrackUrn = item.getCommentedTrackUrn();
        if (commentedTrackUrn.isPresent()) {
            // for track comments we go to the comments screen
            final Urn trackUrn = commentedTrackUrn.get();
            trackSubscription = RxJava.toV1Observable(trackRepository.track(trackUrn)).subscribe(new DefaultSubscriber<Track>() {
                @Override
                public void onNext(Track track) {
                    navigator.openTrackComments(view.getContext(), trackUrn);
                }
            });
        } else {
            // in all other cases we simply go to the user profile
            final Urn userUrn = item.getUrn();
            navigator.legacyOpenProfile(view.getContext(), userUrn);
        }
    }

    @Override
    public int getNewItemsTextResourceId() {
        return R.plurals.activities_new_notification;
    }
}
