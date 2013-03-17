package com.soundcloud.android.fragment;

import static com.soundcloud.android.rx.ScObservables.pending;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ActivityAdapter;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.schedulers.ActivitiesScheduler;
import com.soundcloud.android.rx.observers.ContextObserver;
import com.soundcloud.android.view.ScListView;
import rx.Observer;
import rx.Subscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ReactiveListFragment extends Fragment {

    private static final String TAG = ReactiveListFragment.class.getSimpleName();

    private ScListView mListView;
    private ActivityAdapter mAdapter;

    private ActivitiesScheduler mScheduler;
    private Subscription mSubscription;
    private ContextObserver<Activities> mActivitiesObserver;

    private class LoadActivitiesObserver implements Observer<Activities> {

        @Override
        public void onCompleted() {
            Log.d(TAG, "onCompleted t=" + Thread.currentThread().getName());
            Log.d(TAG, "done=" + mActivitiesObserver.isCompleted());
        }

        @Override
        public void onError(Exception e) {
            Log.d(TAG, "onError: " + e + "; t=" + Thread.currentThread().getName());
            e.printStackTrace();
        }

        @Override
        public void onNext(Activities activities) {
            Log.d(TAG, "onNext: " + activities.size() + "; t=" + Thread.currentThread().getName());
            mAdapter.addItems(activities);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        mAdapter = new ActivityAdapter(getActivity(), null);

        mScheduler = new ActivitiesScheduler(getActivity());

        if (savedInstanceState == null) {
            Log.d(TAG, "first start, scheduling possible sync");
            mScheduler.addPendingObservable(mScheduler.syncIfNecessary(Content.ME_SOUND_STREAM.uri));
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.playlist_fragment, container, false);

        mListView = (ScListView) layout.findViewById(R.id.list);
        mListView.setAdapter(mAdapter);

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(TAG, "onStart");

        // TODO: check for events that may have changed the underlying data
        if (mAdapter.isEmpty()) {
            Log.d(TAG, "Adapter is empty, scheduling possible local refresh");
            mScheduler.addPendingObservable(pending(mScheduler.loadActivitiesSince(Content.ME_SOUND_STREAM.uri, 0)));
        }

        mActivitiesObserver = new ContextObserver<Activities>(new LoadActivitiesObserver());
        mSubscription = mScheduler.scheduleFirstPendingObservable(mActivitiesObserver);

        Log.d(TAG, "onStart: done=" + mActivitiesObserver.isCompleted());
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: done=" + mActivitiesObserver.isCompleted());
        mSubscription.unsubscribe();

    }

}
