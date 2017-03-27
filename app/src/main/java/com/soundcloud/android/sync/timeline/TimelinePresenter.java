package com.soundcloud.android.sync.timeline;

import static com.soundcloud.android.rx.RxUtils.IS_VALID_TIMESTAMP;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.view.NewItemsIndicator;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.util.Date;
import java.util.List;

public abstract class TimelinePresenter<ItemT>
        extends RecyclerViewPresenter<List<ItemT>, ItemT>
        implements NewItemsIndicator.Listener {

    private final NewItemsIndicator newItemsIndicator;
    private final TimelineOperations<?, ItemT> operations;
    private final PagingRecyclerItemAdapter<ItemT, ? extends RecyclerView.ViewHolder> adapter;

    private final Action1<Integer> updateNewItemsIndicator = new Action1<Integer>() {
        @Override
        public void call(Integer newItems) {
            newItemsIndicator.update(newItems);
        }
    };

    private final Func1<Long, Observable<Integer>> newItemsCount = new Func1<Long, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Long time) {
            return operations.newItemsSince(time);
        }
    };

    private final Observable<Long> mostRecentTimestamp = Observable.defer(new Func0<Observable<Long>>() {
        @Override
        public Observable<Long> call() {
            Optional<Date> date = operations.getFirstItemTimestamp(adapter.getItems());
            return Observable.just(date.isPresent() ? date.get().getTime() : Consts.NOT_SET);
        }
    });

    public TimelinePresenter(SwipeRefreshAttacher swipeRefreshAttacher,
                             Options build,
                             NewItemsIndicator newItemsIndicator,
                             TimelineOperations<?, ItemT> operations,
                             PagingRecyclerItemAdapter<ItemT, ? extends RecyclerView.ViewHolder> adapter) {
        super(swipeRefreshAttacher, build);
        this.newItemsIndicator = newItemsIndicator;
        this.operations = operations;
        this.adapter = adapter;

        newItemsIndicator.setTextResourceId(getNewItemsTextResourceId());
        newItemsIndicator.setClickListener(this);
    }

    public abstract int getNewItemsTextResourceId();

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        refreshAndUpdateIndicator();
    }

    @Override
    public void onNewItemsIndicatorClicked() {
        getRecyclerView().scrollToPosition(0);
        rebuildBinding(null).connect();
    }

    @Override
    protected void onCreateCollectionView(Fragment fragment, View view, Bundle savedInstanceState) {
        super.onCreateCollectionView(fragment, view, savedInstanceState);
        newItemsIndicator.setTextView((TextView) view.findViewById(R.id.new_items_indicator));
        getRecyclerView().addOnScrollListener(newItemsIndicator.getScrollListener());
    }

    protected Observable<Integer> updateIndicatorFromMostRecent() {
        return mostRecentTimestamp
                .filter(IS_VALID_TIMESTAMP)
                .flatMap(newItemsCount)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(updateNewItemsIndicator);
    }

    private void refreshAndUpdateIndicator() {
        fireAndForget(operations.updatedTimelineItemsForStart()
                  .flatMap(o -> updateIndicatorFromMostRecent()));
    }
}
