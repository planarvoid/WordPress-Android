package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockListFragment;
import com.commonsware.cwac.endless.EndlessAdapter;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.adapter.ScEndlessAdapter;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.CollectionLoader;
import com.soundcloud.android.task.ILazyAdapterTask;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.android.task.UpdateCollectionTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.api.Request;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Color;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

import java.util.List;

public class ScListFragment extends SherlockListFragment
        implements DetachableResultReceiver.Receiver, LocalCollection.OnChangeListener {

    private CollectionLoader mItemLoader;

    private int mState;
    private static final int INIT = 0;
    private static final int WAITING = 1; // waiting for interaction
    private static final int LOADING = 2; // refreshing cursor
    private static final int APPENDING = 3;
    private static final int DONE = 4;
    private static final int ERROR = 5;

    private ScListView mListView;
    private ScBaseAdapter mBaseAdapter;
    private EmptyCollection mEmptyCollection;
    private EmptyCollection mDefaultEmptyCollection;
    private String mEmptyCollectionText;

    private DetachableResultReceiver mDetachableReceiver;
    private Content mContent;
    private Uri mContentUri;
    private boolean mIsConnected;
    private NetworkConnectivityListener connectivityListener;

    private AsyncTask<Object, List<? super Parcelable>, Boolean> mRefreshTask;
    private UpdateCollectionTask mUpdateCollectionTask;

    private Boolean mIsSyncable;
        protected LocalCollection mLocalCollection;
        private ChangeObserver mChangeObserver;
        private boolean mContentInvalid, mObservingContent;

        protected String mNextHref;


    protected static final int CONNECTIVITY_MSG = 0;
    ;
    protected boolean mAppendable = true;
    private int mPageIndex = 0;

    public static ScListFragment newInstance(Content content) {
        ScListFragment fragment = new ScListFragment();
        Bundle args = new Bundle();
        args.putParcelable("contentUri", content.uri);
        fragment.setArguments(args);
        return fragment;
    }

    public ScListFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mContentUri = (Uri) getArguments().get("contentUri");
        mContent = Content.byUri(mContentUri);

        final ContentResolver contentResolver = getActivity().getContentResolver();
        if (mContentUri != null) {
            // TODO :  Move off the UI thread.
            mLocalCollection = LocalCollection.fromContentUri(mContentUri, contentResolver, true);
            mLocalCollection.startObservingSelf(contentResolver, this);
            mChangeObserver = new ChangeObserver();
            mObservingContent = true;
            contentResolver.registerContentObserver(mContentUri, true, mChangeObserver);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBaseAdapter = new ScBaseAdapter(getActivity(), mContent);
        setListAdapter(mBaseAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();

        FrameLayout root = new FrameLayout(context);

        mListView = buildList();
        root.addView(mListView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ------------------------------------------------------------------

        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        return root;
    }

    @Override
    public void onAttach(android.app.Activity activity) {
        super.onAttach(activity);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public void setCustomEmptyCollection(EmptyCollection emptyCollection) {
        mEmptyCollection = emptyCollection;
    }

    public void setEmptyViewText(String str) {
        mEmptyCollectionText = str;
    }

    public ScActivity getScActivity() {
        return (ScActivity) getActivity();
    }

    public ScListView getScListView() {
        return mListView;
    }

    public ScListView buildList() {
        return configureList(new ScListView((ScListActivity) getActivity()), false);
    }

    public ScListView buildList(boolean longClickable) {
        return configureList(new ScListView((ScListActivity) getActivity()), longClickable);
    }


    public ScListView configureList(ScListView lv, boolean longClickable) {
        //lv.setId(android.R.id.list);
        lv.getRefreshableView().setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        lv.getRefreshableView().setFastScrollEnabled(false);
        lv.getRefreshableView().setDivider(getResources().getDrawable(R.drawable.list_separator));
        lv.getRefreshableView().setDividerHeight(1);
        lv.getRefreshableView().setCacheColorHint(Color.TRANSPARENT);
        lv.getRefreshableView().setLongClickable(longClickable);
        return lv;
    }

    public void setListAdapter(ListAdapter adapter) {
        getListView().setAdapter(adapter);
    }

    public EndlessAdapter getWrapper() {
        return mEndlessAdapter;
    }

    public ScBaseAdapter getBaseAdapter() {
        return mBaseAdapter;
    }


    private Uri getCurrentUri() {
        return mContent.uri.buildUpon().appendQueryParameter("limit", String.valueOf(mPageIndex * Consts.COLLECTION_PAGE_SIZE)).build();
    }

    protected DetachableResultReceiver getReceiver() {
        if (mDetachableReceiver == null) mDetachableReceiver = new DetachableResultReceiver(new Handler());
        mDetachableReceiver.setReceiver(this);
        return mDetachableReceiver;
    }


    @Override
    public void onStart() {
        super.onStart();
        connectivityListener = new NetworkConnectivityListener();
        connectivityListener.registerHandler(connHandler, CONNECTIVITY_MSG);
    }

    private Handler connHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    if (connectivityListener != null) {
                        final NetworkInfo networkInfo = connectivityListener.getNetworkInfo();
                        if (networkInfo != null) {
                            onDataConnectionUpdated(networkInfo.isConnectedOrConnecting());
                        }
                    }
                    break;
            }
        }
    };

    protected void onDataConnectionUpdated(boolean isConnected) {
        mIsConnected = isConnected;
        if (isConnected && !isConnected) {
            if (getBaseAdapter().needsItems() && getScActivity().getApp().getAccount() != null) {

            }
        }
    }


    private void setEmptyView(EmptyCollection emptyCollection) {
        final ViewGroup root = (ViewGroup) getView();
        if (root.findViewById(android.R.id.empty) != null) {
            root.removeView(root.findViewById(android.R.id.empty));
        }
        root.addView(emptyCollection);
        emptyCollection.setId(android.R.id.empty);
    }

    private void applyEmptyView() {
        final boolean error = mState == ERROR;

        if (mEmptyCollection != null && !error) {
            setEmptyView(mEmptyCollection);
        } else {
            if (mDefaultEmptyCollection == null) {
                mDefaultEmptyCollection = new EmptyCollection(getActivity());
            }
            mDefaultEmptyCollection.setImage(error ? R.drawable.empty_connection : R.drawable.empty_collection);
            mDefaultEmptyCollection.setMessageText((!error && !TextUtils.isEmpty(mEmptyCollectionText)) ? mEmptyCollectionText : getEmptyText());
            setEmptyView(mDefaultEmptyCollection);
        }
    }

    private String getEmptyText() {
        final Class loadModel = getBaseAdapter().getLoadModel();
        final boolean error = mState == ERROR;
        if (Track.class.equals(loadModel)) {
            return !error ? getResources().getString(
                    R.string.tracklist_empty) : getResources().getString(
                    R.string.tracklist_error);
        } else if (User.class.equals(loadModel)) {
            return !error ? getResources().getString(
                    R.string.userlist_empty) : getResources().getString(
                    R.string.userlist_error);
        } else if (Comment.class.equals(loadModel)) {
            return !error ? getResources().getString(
                    R.string.tracklist_empty) : getResources().getString(
                    R.string.commentslist_error);
        } else if (Activity.class.equals(loadModel)) {
            return !error ? getResources().getString(
                    R.string.tracklist_empty) : getResources().getString(
                    R.string.tracklist_error);
        } else {
            return "";
        }
    }

    public Uri getPlayableUri() {
        return mContentInvalid ? null : mContentUri;
    }

    protected void requestSync() {
        Intent intent = new Intent(getActivity(), ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver())
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(mContent.uri);
        getActivity().startService(intent);
    }

    public boolean isRefreshing() {
        if (mLocalCollection != null) {
            return mLocalCollection.sync_state == LocalCollection.SyncState.SYNCING
                    || mLocalCollection.sync_state == LocalCollection.SyncState.PENDING
                    || isRefreshTaskActive();
        } else {
            return isRefreshTaskActive();
        }
    }

    private boolean isRefreshTaskActive() {
        return (mRefreshTask != null && !AndroidUtils.isTaskFinished((AsyncTask) mRefreshTask));
    }

    protected void doneRefreshing() {
        if (isSyncable()) setListLastUpdated();
        if (mListView != null) mListView.onRefreshComplete();
    }

    protected boolean isSyncable() {
        if (mIsSyncable == null) {
            mIsSyncable = mContent != null && mContent.isSyncable();
        }
        return mIsSyncable;
    }

    public void setListLastUpdated() {
        if (mListView != null) {
            if (mLocalCollection.last_sync_success > 0) mListView.setLastUpdated(mLocalCollection.last_sync_success);
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
            case ApiSyncService.STATUS_SYNC_ERROR: {
                if (mContentUri != null && resultData != null &&
                        !resultData.getBoolean(mContentUri.toString()) && !isRefreshing()) {
                    doneRefreshing(); // nothing changed
                }
                break;
            }
        }
    }

    private void stopObservingChanges() {
        if (mObservingContent) {
            mObservingContent = false;

            if (mChangeObserver != null) {
                getActivity().getContentResolver().unregisterContentObserver(mChangeObserver);
            }
            if (mLocalCollection != null) {
                mLocalCollection.stopObservingSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopObservingChanges();
    }

    /*
    ToDo, broadcast listener
    @Override
    public void onLogout() {
        stopObservingChanges();
    } */

    protected void onContentChanged() {
        mContentInvalid = true;
        executeRefreshTask();
    }

    public void executeRefreshTask() {
        mRefreshTask = buildTask();
        mRefreshTask.execute(getTaskParams(true));
    }

    @Override
    protected AsyncTask<Object, List<? super Parcelable>, Boolean> buildTask() {
        return new RemoteCollectionTask((SoundCloudApplication.fromContext(getActivity()), this);
    }


    protected Object getTaskParams(final boolean refresh) {
        return new RemoteCollectionTask.CollectionParams() {{
            loadModel = mContent.resourceType;
            contentUri = mContentUri;
            request = buildRequest(refresh);
            isRefresh = refresh;
            refreshPageItems = !isSyncable();
            startIndex = refresh ? 0 : getBaseAdapter().getData().size();
            maxToLoad = Consts.COLLECTION_PAGE_SIZE;
        }};
    }

    protected Request buildRequest(boolean isRefresh) {
        Request request = getRequest(isRefresh);
        if (request != null) {
            request.add("linked_partitioning", "1");
            request.add("limit", Consts.COLLECTION_PAGE_SIZE);
        }
        return request;
    }


    protected Request getRequest(boolean isRefresh) {
        if (!mContent.hasRequest()) return null;
        return !(isRefresh) && !TextUtils.isEmpty(mNextHref) ? new Request(mNextHref) : mContent.request();
    }

    private void refreshSyncData() {
        if (isSyncable()) {
            setListLastUpdated();

            if ((mContent != null) && mLocalCollection.shouldAutoRefresh() && !isRefreshing()) {
                refresh(false);
                // TODO : Causes loop with stale collection and server error
                // this is to show the user something at the initial load
                if (mLocalCollection.hasSyncedBefore()) mListView.setRefreshing();
            }
        }
    }

    @Override
    public void refresh(final boolean userRefresh) {
        if (userRefresh) {
                    if (getBaseAdapter() instanceof FollowStatus.Listener) {
                        FollowStatus.get().requestUserFollowings(SoundCloudApplication.fromContext(getActivity()),
                                (FollowStatus.Listener) getBaseAdapter(), true);
                    }
                } else {
                    reset();
                }

        if (isSyncable()) {
            requestSync();
        } else {
            clearAppendTask();
            executeRefreshTask();
            notifyDataSetChanged();
        }
    }

    public void reset() {
        getBaseAdapter().clearData();
        mEndlessAdapter.reset();
            mPageIndex = 0;


            clearRefreshTask();
            clearUpdateTask();

            mState = mAutoAppend ? IDLE : INITIALIZED;
            notifyDataSetChanged();
        }

    protected void clearRefreshTask() {
        if (mRefreshTask != null && !AndroidUtils.isTaskFinished(mRefreshTask)) mRefreshTask.cancel(true);
        mRefreshTask = null;
    }

    protected void clearUpdateTask() {
        if (mUpdateCollectionTask != null && !AndroidUtils.isTaskFinished(mUpdateCollectionTask))
            mUpdateCollectionTask.cancel(true);
        mUpdateCollectionTask = null;
    }

    @Override
    public void onLocalCollectionChanged() {
        refreshSyncData();
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            // even after unregistering, we will still get asynchronous change notifications, so make sure we want them
            if (mObservingContent) onContentChanged();
        }
    }

}