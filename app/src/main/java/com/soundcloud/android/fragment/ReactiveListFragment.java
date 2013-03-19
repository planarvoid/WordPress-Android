package com.soundcloud.android.fragment;

import static com.soundcloud.android.rx.ScObservables.pending;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.rx.observers.ContextObserver;
import com.soundcloud.android.rx.schedulers.ReactiveScheduler;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.ScListView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.util.List;

public abstract class ReactiveListFragment<T extends ScModel> extends Fragment implements PullToRefreshBase.OnRefreshListener,
        AdapterView.OnItemClickListener {

    private static final int PROGRESS_DELAY_MILLIS = 250;

    private Handler mShowProgressHandler = new Handler();
    private Runnable showProgress = new Runnable() {
        @Override
        public void run() {
            mEmptyView.setStatus(EmptyListView.Status.WAITING);
        }
    };

    protected ContextObserver<List<T>> mLoadItemsObserver;
    protected Subscription mLoadItemsSubscription;

    protected ScListView mListView;
    protected EmptyListView mEmptyView;
    protected ScBaseAdapter<T> mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(this, "onCreate");

        mAdapter = newAdapter();
        mLoadItemsObserver = new ContextObserver<List<T>>(new LoadItemsObserver());
    }

    protected abstract ScBaseAdapter<T> newAdapter();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.basic_list_fragment, container, false);

        mListView = (ScListView) layout.findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setOnRefreshListener(this);

        mEmptyView = (EmptyListView) layout.findViewById(android.R.id.empty);

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(this, "onStart");

        // TODO: check for events that may have changed the underlying data
        // TODO: this fires when the adapter is empty because the user simply has no sounds, but shouldn't.
        if (mAdapter.isEmpty()) {
            Log.d(this, "Adapter is empty, scheduling possible local refresh");
            getListItemsScheduler().addPendingObservable(pending(getListItemsObservable()));
        }

        mLoadItemsSubscription = getListItemsScheduler().scheduleFirstPendingObservable(mLoadItemsObserver);

        Log.d(this, "onStart: done=" + mLoadItemsObserver.isCompleted());

        mEmptyView.setStatus(EmptyListView.Status.OK);

        mShowProgressHandler.postDelayed(showProgress, PROGRESS_DELAY_MILLIS);
    }

    protected abstract ReactiveScheduler<List<T>> getListItemsScheduler();

    protected abstract Observable<List<T>> getListItemsObservable();

    @Override
    public void onStop() {
        super.onStop();
        Log.d(this, "onStop: done=" + mLoadItemsObserver.isCompleted());
        mLoadItemsSubscription.unsubscribe();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(this, "onDestroy");
    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        mShowProgressHandler.postDelayed(showProgress, PROGRESS_DELAY_MILLIS);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //TODO: item click handling does NOT belong in an adapter...
        int itemPosition = position - mListView.getRefreshableView().getHeaderViewsCount();
        mAdapter.handleListItemClick(itemPosition, id);
    }

    public ScBaseAdapter<T> getListAdapter() {
        return mAdapter;
    }

    protected class LoadItemsObserver implements Observer<List<T>> {

        @Override
        public void onCompleted() {
            Log.d(this, "onCompleted t=" + Thread.currentThread().getName());

            mShowProgressHandler.removeCallbacks(showProgress);
            mEmptyView.setStatus(EmptyListView.Status.OK);

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
            if (!items.isEmpty()) {
                mAdapter.addItems(items);
                mAdapter.notifyDataSetChanged();
            }
        }
    }


}
