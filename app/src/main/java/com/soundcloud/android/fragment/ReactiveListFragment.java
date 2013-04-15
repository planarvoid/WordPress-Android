package com.soundcloud.android.fragment;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.IScAdapter;
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
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

public abstract class ReactiveListFragment<T> extends Fragment implements PullToRefreshBase.OnRefreshListener,
        AdapterView.OnItemClickListener {

    private static final int PROGRESS_DELAY_MILLIS = 250;

    private Handler mShowProgressHandler = new Handler();
    private Runnable showProgress = new Runnable() {
        @Override
        public void run() {
            mEmptyView.setStatus(EmptyListView.Status.WAITING);
            mEmptyView.setVisibility(View.VISIBLE);
        }
    };

    private List<Observable<Observable<List<T>>>> mPendingObservables;

    protected Observer<List<T>> mLoadItemsObserver;
    protected Subscription mLoadItemsSubscription;

    protected ScListView mListView;
    protected EmptyListView mEmptyView;
    protected IScAdapter<T> mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(this, "onCreate");

        mPendingObservables = new ArrayList<Observable<Observable<List<T>>>>();

        mAdapter = newAdapter();
        mLoadItemsObserver = new LoadItemsObserver();
    }

    protected abstract IScAdapter<T> newAdapter();

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

    protected abstract void configureEmptyListView(EmptyListView emptyView);

    @Override
    public void onStart() {
        super.onStart();

        Log.d(this, "onStart");

        if (hasPendingObservables()) {
            mLoadItemsSubscription = scheduleFirstPendingObservable(mLoadItemsObserver);
            showProgressSpinner();
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

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        showProgressSpinner();
    }

    private void showProgressSpinner() {
        mShowProgressHandler.postDelayed(showProgress, PROGRESS_DELAY_MILLIS);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //TODO: item click handling does NOT belong in an adapter...
        int itemPosition = position - mListView.getRefreshableView().getHeaderViewsCount();
        mAdapter.handleListItemClick(itemPosition, id);
    }

    public IScAdapter<T> getListAdapter() {
        return mAdapter;
    }

    protected boolean hasPendingObservables() {
        return !mPendingObservables.isEmpty();
    }

    protected void addPendingObservable(Observable<Observable<List<T>>> observable) {
        mPendingObservables.add(observable);
    }

    //TODO: currently we lose the subscription to the underlying pending observable and merely return the one returned
    //from the decision function (which is fast to execute). We need a way to hold on to the subscription of the
    //actual long running task so that we can disconnect the observer at any point in time
    private Subscription scheduleFirstPendingObservable(Observer<List<T>> observer) {
        if (hasPendingObservables()) {
            Observable<Observable<List<T>>> observable = mPendingObservables.get(0);
            Subscription subscription = observable.subscribe(ScActions.pendingAction(observer));
            mPendingObservables.clear();
            return subscription;
        }
        return Subscriptions.empty();
    }

    protected class LoadItemsObserver implements Observer<List<T>> {

        @Override
        public void onCompleted() {
            Log.d(this, "onCompleted t=" + Thread.currentThread().getName());

            mShowProgressHandler.removeCallbacks(showProgress);

            if (mListView.isRefreshing()) {
                mListView.onRefreshComplete();
            }
        }

        @Override
        public void onError(Exception e) {
            Log.d(this, "onError: " + e + "; t=" + Thread.currentThread().getName());
            e.printStackTrace();

            onCompleted();

            // TODO: need to check if this is really always a connection error? do we treat errors from reading/writing
            // from and to local storage as connection errors too?
            mEmptyView.setStatus(EmptyListView.Status.CONNECTION_ERROR);
        }

        @Override
        public void onNext(List<T> items) {
            Log.d(this, "onNext: " + items.size() + "; t=" + Thread.currentThread().getName());
            if (items.isEmpty()) {
                mEmptyView.setStatus(EmptyListView.Status.OK);
            } else {
                mEmptyView.setVisibility(View.GONE);
                mAdapter.clearData();
                mAdapter.addItems(items);
                mAdapter.notifyDataSetChanged();
            }
        }
    }


}
