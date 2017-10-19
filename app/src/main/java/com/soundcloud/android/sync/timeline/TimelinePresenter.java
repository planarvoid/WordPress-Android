package com.soundcloud.android.sync.timeline;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.PagingRecyclerItemAdapter;
import com.soundcloud.android.presentation.RecyclerViewPresenter;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.view.NewItemsIndicator;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Date;
import java.util.List;

public abstract class TimelinePresenter<ItemT>
        extends RecyclerViewPresenter<List<ItemT>, ItemT>
        implements NewItemsIndicator.Listener {

    private final NewItemsIndicator newItemsIndicator;
    private final TimelineOperations<?, ItemT> operations;
    private final PagingRecyclerItemAdapter<ItemT, ? extends RecyclerView.ViewHolder> adapter;

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
        newItemsIndicator.setTextView(view.findViewById(R.id.new_items_indicator));
        getRecyclerView().addOnScrollListener(newItemsIndicator.getScrollListener());
    }

    protected Observable<Integer> updateIndicatorFromMostRecent() {
        return Observable.defer(() -> {
            Optional<Date> date = operations.getFirstItemTimestamp(adapter.getItems());
            return Observable.just(date.isPresent() ? date.get().getTime() : Consts.NOT_SET);
        })
                         .filter(ts -> ts != Consts.NOT_SET)
                         .flatMap(operations::newItemsSince)
                         .observeOn(AndroidSchedulers.mainThread())
                         .doOnNext(newItemsIndicator::update);
    }

    private void refreshAndUpdateIndicator() {
        operations.updatedTimelineItemsForStart().toObservable().flatMap(o -> updateIndicatorFromMostRecent()).subscribe(new DefaultObserver<>());
    }
}
