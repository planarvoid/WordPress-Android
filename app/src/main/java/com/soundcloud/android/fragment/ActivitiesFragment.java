package com.soundcloud.android.fragment;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.ActivityAdapter;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.event.Event;
import com.soundcloud.android.rx.schedulers.LoadActivitiesStrategy;
import com.soundcloud.android.rx.schedulers.SyncManager;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;
import rx.Subscription;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.util.List;

public class ActivitiesFragment extends ReactiveListFragment<Activity> {

    public static final String EXTRA_STREAM_URI = "stream_uri";

    private Uri mContentUri;

    private SyncManager<List<Activity>> mSyncManager;
    private Subscription mAssocChangedSubscription;
    private Observable<List<Activity>> mLoadActivities;


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

        SyncManager.LocalStorageStrategy<List<Activity>> storageStrategy = new LoadActivitiesStrategy(getActivity());
        mSyncManager = new SyncManager<List<Activity>>(getActivity(), storageStrategy);
        mLoadActivities = storageStrategy.loadFromContentUri(mContentUri);

        mAssocChangedSubscription = Event.anyOf(Event.LIKE_CHANGED, Event.REPOST_CHANGED).subscribe(mLoadActivities, mLoadItemsObserver);

        if (savedInstanceState == null) {
            Log.d(this, "first start, scheduling possible sync");
            mScheduler.addPendingObservable(mSyncManager.syncIfNecessary(mContentUri));
        }
    }

    @Override
    protected void configureEmptyListView(EmptyListView emptyView) {
        switch (Content.match(mContentUri)) {
            case ME_SOUND_STREAM:
                configureEmptySoundStream(emptyView);
            case ME_ACTIVITIES:
                configureEmptyNewsStream(emptyView);

        }
    }

    private void configureEmptyNewsStream(EmptyListView emptyView) {
        User loggedInUser = SoundCloudApplication.fromContext(getActivity()).getLoggedInUser();
        if (loggedInUser == null || loggedInUser.track_count > 0) {
            emptyView.setMessageText(R.string.list_empty_activity_message)
                    .setImage(R.drawable.empty_share)
                    .setActionText(R.string.list_empty_activity_action)
                    .setSecondaryText(R.string.list_empty_activity_secondary)
                    .setButtonActionListener(new EmptyListView.ActionListener() {
                        @Override
                        public void onAction() {
                            startActivity(new Intent(Actions.YOUR_SOUNDS));
                        }

                        @Override
                        public void onSecondaryAction() {
                            goTo101s();
                        }
                    });
        } else {
            final EmptyListView.ActionListener record = new EmptyListView.ActionListener() {
                @Override
                public void onAction() {
                    startActivity(new Intent(Actions.RECORD));
                }

                @Override
                public void onSecondaryAction() {
                    goTo101s();
                }
            };

            emptyView.setMessageText(R.string.list_empty_activity_nosounds_message)
                    .setImage(R.drawable.empty_rec)
                    .setActionText(R.string.list_empty_activity_nosounds_action)
                    .setSecondaryText(R.string.list_empty_activity_nosounds_secondary)
                    .setButtonActionListener(record)
                    .setImageActionListener(record);
        }
    }

    private void goTo101s() {
        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("http://soundcloud.com/101")));
    }

    private void configureEmptySoundStream(EmptyListView emptyView) {
        emptyView.setMessageText(R.string.list_empty_stream_message)
                .setImage(R.drawable.empty_follow)
                .setActionText(R.string.list_empty_stream_action)
                .setSecondaryText(R.string.list_empty_stream_secondary)
                .setButtonActionListener(new EmptyListView.ActionListener() {
                    @Override
                    public void onAction() {
                        startActivity(new Intent(Actions.WHO_TO_FOLLOW));
                    }

                    @Override
                    public void onSecondaryAction() {
                        startActivity(new Intent(Actions.FRIEND_FINDER));
                    }
                });
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
        mSyncManager.syncNow(mContentUri).subscribe(mLoadItemsObserver);
    }

    @Override
    protected IScAdapter<Activity> newAdapter() {
        return new ActivityAdapter(getActivity(), null);
    }

    @Override
    protected Observable<List<Activity>> getListItemsObservable() {
        return mLoadActivities;
    }
}
