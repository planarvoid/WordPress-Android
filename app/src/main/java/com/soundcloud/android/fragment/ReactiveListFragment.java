package com.soundcloud.android.fragment;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.rx.ScActions;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.ScListView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

public abstract class ReactiveListFragment<T extends ScModel> extends Fragment implements PullToRefreshBase.OnRefreshListener,
        AdapterView.OnItemClickListener, AbsListView.OnScrollListener, ImageLoader.LoadBlocker {

    private static final int PROGRESS_DELAY_MILLIS = 250;

    private Handler mShowProgressHandler = new Handler();
    private Runnable showProgress = new Runnable() {
        @Override
        public void run() {
            mEmptyView.setStatus(EmptyListView.Status.WAITING);
            mEmptyView.setVisibility(View.VISIBLE);
        }
    };

    private List<Observable<Observable<T>>> mPendingObservables;

    protected Observer<T> mLoadItemsObserver;
    protected Subscription mLoadItemsSubscription;

    protected ScListView mListView;
    protected EmptyListView mEmptyView;
    protected ScBaseAdapter<T> mAdapter;

    ///////////////////////////////////////////////////////////////////////////
    // Public interface
    ///////////////////////////////////////////////////////////////////////////

    public ScBaseAdapter<T> getListAdapter() {
        return mAdapter;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment life cycle methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(this, "onCreate");

        mPendingObservables = new ArrayList<Observable<Observable<T>>>();

        mAdapter = newAdapter();
        mLoadItemsObserver = new LoadItemsObserver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.basic_list_fragment, container, false);

        mEmptyView = (EmptyListView) layout.findViewById(android.R.id.empty);
        configureEmptyListView(mEmptyView);

        mListView = (ScListView) layout.findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnRefreshListener(this);

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(this, "onStart");

        if (hasPendingObservables()) {
            prepareRefresh();
            mLoadItemsSubscription = loadListItems();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mLoadItemsSubscription.unsubscribe();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(this, "onDestroy");
    }

    ///////////////////////////////////////////////////////////////////////////
    // List view callbacks
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        prepareRefresh();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //TODO: item click handling does NOT belong in an adapter...
        int itemPosition = position - mListView.getRefreshableView().getHeaderViewsCount();
        mAdapter.handleListItemClick(itemPosition, id);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // to make image scrolling smoother, we block the image loader until scrolling has stopped
        switch (scrollState) {
            case SCROLL_STATE_FLING:
            case SCROLL_STATE_TOUCH_SCROLL:
                ImageLoader.get(getActivity()).block(this);
                break;
            case SCROLL_STATE_IDLE:
                ImageLoader.get(getActivity()).unblock(this);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private helpers
    ///////////////////////////////////////////////////////////////////////////

    //TODO: currently we lose the subscription to the underlying pending observable and merely return the one returned
    //from the decision function (which is fast to execute). We need a way to hold on to the subscription of the
    //actual long running task so that we can disconnect the observer at any point in time
    private Subscription loadListItems() {
        if (hasPendingObservables()) {
            Observable<Observable<T>> observable = mPendingObservables.get(0);
            Subscription subscription = observable.subscribe(ScActions.pendingAction(mLoadItemsObserver));
            mPendingObservables.clear();
            return subscription;
        }
        return Subscriptions.empty();
    }

    private void prepareRefresh() {
        mShowProgressHandler.postDelayed(showProgress, PROGRESS_DELAY_MILLIS);
        mAdapter.clearData();
        mAdapter.notifyDataSetChanged();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Subclass interface
    ///////////////////////////////////////////////////////////////////////////

    protected abstract ScBaseAdapter<T> newAdapter();

    protected abstract void configureEmptyListView(EmptyListView emptyView);

    protected boolean hasPendingObservables() {
        return !mPendingObservables.isEmpty();
    }

    protected void addPendingObservable(Observable<Observable<T>> observable) {
        mPendingObservables.add(observable);
    }

    protected class LoadItemsObserver implements Observer<T> {

        @Override
        public void onCompleted() {
            Log.d(this, "onCompleted t=" + Thread.currentThread().getName());

            if (mAdapter.isEmpty()) {
                mEmptyView.setStatus(EmptyListView.Status.OK);
            } else {
                mEmptyView.setVisibility(View.GONE);
                mAdapter.notifyDataSetChanged();
            }

            onFinished();
        }

        @Override
        public void onError(Exception e) {
            Log.d(this, "onError: " + e + "; t=" + Thread.currentThread().getName());
            e.printStackTrace();

            // TODO: need to check if this is really always a connection error? do we treat errors from reading/writing
            // from and to local storage as connection errors too?
            mEmptyView.setStatus(EmptyListView.Status.CONNECTION_ERROR);

            onFinished();
        }

        @Override
        public void onNext(T item) {
            Log.d(this, "onNext: " + item.toString() + "; t=" + Thread.currentThread().getName());
            mAdapter.insertItem(item);
        }

        protected void onFinished() {
            mShowProgressHandler.removeCallbacks(showProgress);

            if (mListView.isRefreshing()) {
                mListView.onRefreshComplete();
            }
        }
    }


}
