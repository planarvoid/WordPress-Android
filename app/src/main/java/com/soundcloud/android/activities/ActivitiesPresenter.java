package com.soundcloud.android.activities;

import static com.soundcloud.android.rx.RxUtils.IS_VALID_TIMESTAMP;
import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CollectionBinding;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.NewItemsIndicator;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Date;

class ActivitiesPresenter extends RecyclerViewPresenter<ActivityItem> implements
        NewItemsIndicator.Listener {

    private final ActivitiesOperations operations;
    private final ActivitiesAdapter adapter;
    private final TrackRepository trackRepository;
    private final Navigator navigator;
    private final NewItemsIndicator newItemsIndicator;
    private Subscription trackSubscription = RxUtils.invalidSubscription();

    @Inject
    ActivitiesPresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                        ActivitiesOperations operations,
                        ActivitiesAdapter adapter,
                        TrackRepository trackRepository,
                        Navigator navigator,
                        NewItemsIndicator newItemsIndicator) {
        super(swipeRefreshAttacher, Options.list().build());
        this.operations = operations;
        this.adapter = adapter;
        this.trackRepository = trackRepository;
        this.navigator = navigator;
        this.newItemsIndicator = newItemsIndicator;

        newItemsIndicator.setTextResourceId(R.plurals.activities_new_notification);
        newItemsIndicator.setClickListener(this);
    }

    @Override
    public void onCreate(Fragment fragment, Bundle bundle) {
        super.onCreate(fragment, bundle);
        getBinding().connect();
        refreshAndUpdateIndicator();
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onCreateCollectionView(fragment, view, savedInstanceState);
        final EmptyView emptyView = getEmptyView();
        emptyView.setImage(R.drawable.empty_activity);
        emptyView.setMessageText(R.string.list_empty_notification_message);
        emptyView.setSecondaryText(R.string.list_empty_notification_secondary);

        newItemsIndicator.setTextView((TextView) view.findViewById(R.id.new_items_indicator));
        getRecyclerView().addOnScrollListener(newItemsIndicator.getScrollListener());
    }

    @Override
    protected CollectionBinding<ActivityItem> onBuildBinding(Bundle fragmentArgs) {
        return CollectionBinding.from(operations.initialActivities())
                .withAdapter(adapter)
                .withPager(operations.pagingFunction())
                .build();
    }

    @Override
    protected CollectionBinding<ActivityItem> onRefreshBinding() {
        return CollectionBinding.from(operations.updatedActivities())
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
            trackSubscription = trackRepository.track(trackUrn).subscribe(new DefaultSubscriber<PropertySet>() {
                @Override
                public void onNext(PropertySet track) {
                    navigator.openTrackComments(view.getContext(), trackUrn);
                }
            });
        } else {
            // in all other cases we simply go to the user profile
            final Urn userUrn = item.getEntityUrn();
            navigator.openProfile(view.getContext(), userUrn);
        }
    }

    private void softReload() {
        adapter.clear();
        rebuildBinding(null).connect();
    }

    @Override
    public void onNewItemsIndicatorClicked() {
        scrollToTop();
        softReload();
    }

    private Observable<Long> mostRecentTimestamp() {
        return Observable.create(new Observable.OnSubscribe<Long>() {
            @Override
            public void call(Subscriber<? super Long> subscriber) {
                Date date = operations.getFirstItemTimestamp(adapter.getItems());
                subscriber.onNext(date == null ? Consts.NOT_SET : date.getTime());
                subscriber.onCompleted();
            }
        });
    }

    private void refreshAndUpdateIndicator() {
        operations.updatedActivityItemsForStart()
                .flatMap(continueWith(updateIndicatorFromMostRecent()))
                .subscribe();
    }

    private Observable<Integer> updateIndicatorFromMostRecent() {
        return mostRecentTimestamp()
                .filter(IS_VALID_TIMESTAMP)
                .flatMap(newItemsCount())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(updateNewItemsIndicator());
    }

    private Func1<Long, Observable<Integer>> newItemsCount() {
        return new Func1<Long, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Long time) {
                return operations.newItemsSince(time);
            }
        };
    }

    private Action1<Integer> updateNewItemsIndicator() {
        return new Action1<Integer>() {
            @Override
            public void call(Integer newItems) {
                newItemsIndicator.update(newItems);
            }
        };
    }

}
