package com.soundcloud.android.fragment;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.adapter.ActivityAdapter;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.event.Event;
import com.soundcloud.android.rx.schedulers.ActivitiesScheduler;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.Subscription;

import android.net.Uri;
import android.os.Bundle;

public class ActivitiesFragment extends ReactiveListFragment {

    public static final String EXTRA_STREAM_URI = "stream_uri";

    private Uri mContentUri;

    private ActivitiesScheduler mScheduler;
    private Subscription mAssocChangedSubscription;
    private Observable<Activities> mLoadActivities;


    public static ActivitiesFragment create(final Content streamContent) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_STREAM_URI, streamContent.uri);

        ActivitiesFragment fragment = new ActivitiesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContentUri = (Uri) getArguments().get(EXTRA_STREAM_URI);
        mScheduler = new ActivitiesScheduler(getActivity());
        mLoadActivities = mScheduler.loadFromLocalStorage(mContentUri);

        mAssocChangedSubscription = Event.anyOf(Event.LIKE_CHANGED, Event.REPOST_CHANGED).subscribe(mLoadActivities, mLoadItemsObserver);

        if (savedInstanceState == null) {
            Log.d(this, "first start, scheduling possible sync");
            mScheduler.addPendingObservable(mScheduler.syncIfNecessary(mContentUri));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(this, "onDestroy");
        mAssocChangedSubscription.unsubscribe();
    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        super.onRefresh(refreshView);
        mScheduler.syncNow(mContentUri).subscribe(mLoadItemsObserver);
    }

    @Override
    protected ScBaseAdapter newAdapter() {
        return new ActivityAdapter(getActivity(), null);
    }

    @Override
    protected ActivitiesScheduler getListItemsScheduler() {
        return mScheduler;
    }

    @Override
    protected Observable getListItemsObservable() {
        return mLoadActivities;
    }
}
