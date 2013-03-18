package com.soundcloud.android.fragment;

import static com.soundcloud.android.rx.ScObservables.pending;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ActivityAdapter;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.event.Events;
import com.soundcloud.android.rx.observers.ContextObserver;
import com.soundcloud.android.rx.schedulers.ActivitiesScheduler;
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

public class ReactiveListFragment extends Fragment implements PullToRefreshBase.OnRefreshListener,
        AdapterView.OnItemClickListener {

    private static final int PROGRESS_DELAY_MILLIS = 250;

    private ScListView mListView;
    private EmptyListView mEmptyView;
    private ActivityAdapter mAdapter;

    private ActivitiesScheduler mScheduler;
    private Subscription mLoadActivitiesSubscription;
    private Subscription mLikeSubscription;
    private ContextObserver<Activities> mActivitiesObserver;
    private Observable<Activities> mLoadActivities;

    private Handler showProgressHandler = new Handler();
    private Runnable showProgress = new Runnable() {
        @Override
        public void run() {
            mEmptyView.setStatus(EmptyListView.Status.WAITING);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(this, "onCreate");

        mAdapter = new ActivityAdapter(getActivity(), null);

        mScheduler = new ActivitiesScheduler(getActivity());
        mActivitiesObserver = new ContextObserver<Activities>(new LoadActivitiesObserver());
        mLoadActivities = mScheduler.loadActivities(Content.ME_SOUND_STREAM.uri);

        mLikeSubscription = Events.subscribe(Events.LIKE_CHANGED, mLoadActivities, mActivitiesObserver);

        if (savedInstanceState == null) {
            Log.d(this, "first start, scheduling possible sync");
            mScheduler.addPendingObservable(mScheduler.syncIfNecessary(Content.ME_SOUND_STREAM.uri));
        }

    }

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
        if (mAdapter.isEmpty()) {
            Log.d(this, "Adapter is empty, scheduling possible local refresh");
            mScheduler.addPendingObservable(pending(mLoadActivities));
        }

        mLoadActivitiesSubscription = mScheduler.scheduleFirstPendingObservable(mActivitiesObserver);

        Log.d(this, "onStart: done=" + mActivitiesObserver.isCompleted());

        mEmptyView.setStatus(EmptyListView.Status.OK);

        showProgressHandler.postDelayed(showProgress, PROGRESS_DELAY_MILLIS);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(this, "onStop: done=" + mActivitiesObserver.isCompleted());
        mLoadActivitiesSubscription.unsubscribe();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(this, "onDestroy");
        mLikeSubscription.unsubscribe();
    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        showProgressHandler.postDelayed(showProgress, PROGRESS_DELAY_MILLIS);
        mScheduler.syncNow(Content.ME_SOUND_STREAM.uri).subscribe(mActivitiesObserver);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //TODO: item click handling does NOT belong in an adapter...
        int itemPosition = position - mListView.getRefreshableView().getHeaderViewsCount();
        mAdapter.handleListItemClick(itemPosition, id);
    }

    private class LoadActivitiesObserver implements Observer<Activities> {

        @Override
        public void onCompleted() {
            Log.d(this, "onCompleted t=" + Thread.currentThread().getName());
            Log.d(this, "done=" + mActivitiesObserver.isCompleted());

            showProgressHandler.removeCallbacks(showProgress);
            mEmptyView.setStatus(EmptyListView.Status.OK);

            if (mListView.isRefreshing()) {
                mListView.onRefreshComplete();
            }
        }

        @Override
        public void onError(Exception e) {
            Log.d(this, "onError: " + e + "; t=" + Thread.currentThread().getName());
            e.printStackTrace();
            showProgressHandler.removeCallbacks(showProgress);
            // TODO: need to check if this is really always a connection error? do we treat errors from reading/writing
            // from and to local storage as connection errors too?
            mEmptyView.setStatus(EmptyListView.Status.CONNECTION_ERROR);
        }

        @Override
        public void onNext(Activities activities) {
            Log.d(this, "onNext: " + activities.size() + "; t=" + Thread.currentThread().getName());
            if (!activities.isEmpty()) {
                mAdapter.addItems(activities);
                mAdapter.notifyDataSetChanged();
            }
        }
    }


}
