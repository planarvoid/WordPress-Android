package com.soundcloud.android.fragment;

import static com.soundcloud.android.rx.ScObservables.pending;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ActivityAdapter;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.observers.ContextObserver;
import com.soundcloud.android.rx.schedulers.ActivitiesScheduler;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.ScListView;
import rx.Observer;
import rx.Subscription;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ReactiveListFragment extends Fragment {

    private static final int PROGRESS_DELAY_MILLIS = 250;

    private ScListView mListView;
    private EmptyListView mEmptyView;
    private ActivityAdapter mAdapter;

    private ActivitiesScheduler mScheduler;
    private Subscription mSubscription;
    private ContextObserver<Activities> mActivitiesObserver;

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
            mScheduler.addPendingObservable(pending(mScheduler.loadActivitiesSince(Content.ME_SOUND_STREAM.uri, 0)));
        }

        mActivitiesObserver = new ContextObserver<Activities>(new LoadActivitiesObserver());
        mSubscription = mScheduler.scheduleFirstPendingObservable(mActivitiesObserver);

        Log.d(this, "onStart: done=" + mActivitiesObserver.isCompleted());

        mEmptyView.setStatus(EmptyListView.Status.OK);

        showProgressHandler.postDelayed(showProgress, PROGRESS_DELAY_MILLIS);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(this, "onStop: done=" + mActivitiesObserver.isCompleted());
        mSubscription.unsubscribe();

    }

    private class LoadActivitiesObserver implements Observer<Activities> {

        @Override
        public void onCompleted() {
            Log.d(this, "onCompleted t=" + Thread.currentThread().getName());
            Log.d(this, "done=" + mActivitiesObserver.isCompleted());

            showProgressHandler.removeCallbacks(showProgress);
            mEmptyView.setStatus(EmptyListView.Status.OK);
        }

        @Override
        public void onError(Exception e) {
            Log.d(this, "onError: " + e + "; t=" + Thread.currentThread().getName());
            e.printStackTrace();
            showProgressHandler.removeCallbacks(showProgress);
            mEmptyView.setStatus(EmptyListView.Status.ERROR);
        }

        @Override
        public void onNext(Activities activities) {
            Log.d(this, "onNext: " + activities.size() + "; t=" + Thread.currentThread().getName());
            mAdapter.addItems(activities);
            mAdapter.notifyDataSetChanged();
        }
    }



}
