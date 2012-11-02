package com.soundcloud.android.fragment;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.actionbarsherlock.app.SherlockListFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.ActivityAdapter;
import com.soundcloud.android.adapter.CommentAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.adapter.PlayableAdapter;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.adapter.SearchAdapter;
import com.soundcloud.android.adapter.TrackAdapter;
import com.soundcloud.android.adapter.UserAdapter;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.collection.CollectionParams;
import com.soundcloud.android.task.collection.CollectionTask;
import com.soundcloud.android.task.collection.ReturnData;
import com.soundcloud.android.task.collection.UpdateCollectionTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;

public class ScListFragment extends SherlockListFragment
        implements PullToRefreshBase.OnRefreshListener, DetachableResultReceiver.Receiver, LocalCollection.OnChangeListener, CollectionTask.Callback, AbsListView.OnScrollListener {

    protected static final int CONNECTIVITY_MSG = 0;

    private @Nullable ScListView mListView;
    private @NotNull EmptyCollection mEmptyCollection;

    private final DetachableResultReceiver mDetachableReceiver = new DetachableResultReceiver(new Handler());
    private @Nullable Content mContent;
    private @NotNull Uri mContentUri;

    private NetworkConnectivityListener connectivityListener;
    private @Nullable CollectionTask mRefreshTask;
    private @Nullable UpdateCollectionTask mUpdateCollectionTask;
    protected @Nullable LocalCollection mLocalCollection;
    private ChangeObserver mChangeObserver;

    private boolean mContentInvalid, mObservingContent, mIgnorePlaybackStatus, mKeepGoing;
    protected String mNextHref;


    public static ScListFragment newInstance(Content content) {
        return newInstance(content.uri);
    }

    public static ScListFragment newInstance(Uri contentUri) {
        ScListFragment fragment = new ScListFragment();
        Bundle args = new Bundle();
        args.putParcelable("contentUri", contentUri);
        fragment.setArguments(args);
        return fragment;
    }

    public ScListFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mContentUri = (Uri) getArguments().get("contentUri");
        mContent = Content.match(mContentUri);
        mKeepGoing = true;

        if (mContent.isSyncable()) {

            final ContentResolver contentResolver = getActivity().getContentResolver();
            // TODO :  Move off the UI thread.
            mLocalCollection = LocalCollection.fromContentUri(mContentUri, contentResolver, true);
            mLocalCollection.startObservingSelf(contentResolver, this);

            mChangeObserver = new ChangeObserver();
            mObservingContent = true;
            contentResolver.registerContentObserver(mContentUri, true, mChangeObserver);
            refreshSyncData();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ScBaseAdapter<?> adapter;
        if (getListAdapter() == null && mContent != null) {
            switch (mContent) {
                case ME_SOUND_STREAM:
                case ME_EXCLUSIVE_STREAM:
                case ME_ACTIVITIES:
                    adapter = new ActivityAdapter(getActivity(), mContentUri);
                    break;

                case ME_FOLLOWERS:
                case ME_FOLLOWINGS:
                case USER_FOLLOWINGS:
                case USER_FOLLOWERS:
                case TRACK_LIKERS:
                    adapter = new UserAdapter(getActivity(), mContentUri);
                    break;

                case ME_TRACKS:
                    adapter = new MyTracksAdapter(getScActivity(), mContentUri);
                    break;

                case ME_LIKES:
                case USER_LIKES:
                    adapter = new TrackAdapter(getActivity(), mContentUri);
                    break;

                case SEARCH:
                    adapter = new SearchAdapter(getActivity(), Content.SEARCH.uri);
                    break;

                case TRACK_COMMENTS:
                    adapter = new CommentAdapter(getActivity(), mContentUri);
                    break;

                 default:
                     adapter = new TrackAdapter(getActivity(), mContentUri);

            }
            setListAdapter(adapter);
            if (canAppend()) append();
        }
    }

    protected boolean canAppend() {
        return mKeepGoing;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();

        FrameLayout root = new FrameLayout(context);

        mListView = buildList();
        mListView.setOnRefreshListener(this);
        mListView.setOnScrollListener(this);

        mEmptyCollection = EmptyCollection.fromContent(context, mContent);
        resetEmptyCollection();
        mListView.setEmptyView(mEmptyCollection);

        if (isRefreshing() || waitingOnInitialSync()){
            mListView.setRefreshing(false);
        }

        root.addView(mListView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ------------------------------------------------------------------

        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));



        return root;
    }

    private boolean waitingOnInitialSync() {
        return (mLocalCollection != null && !mLocalCollection.hasSyncedBefore());
    }

    @Override
    public void onAttach(android.app.Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        switch (getListAdapter().handleListItemClick(position - getListView().getHeaderViewsCount(), id)){
            case ScBaseAdapter.ItemClickResults.LEAVING:
                mIgnorePlaybackStatus = true;
                break;
            default:
        }
    }

    public ScActivity getScActivity() {
        return (ScActivity) getActivity();
    }


    public ScListView buildList() {
        return configureList(new ScListView(getActivity()));
    }

    public ScListView configureList(ScListView lv) {
        //lv.setId(android.R.id.list);
        lv.getRefreshableView().setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        lv.getRefreshableView().setFastScrollEnabled(false);
        return lv;
    }

    @Override
    public ScBaseAdapter getListAdapter() {
        return (ScBaseAdapter) super.getListAdapter();
    }

    protected DetachableResultReceiver getReceiver() {
        mDetachableReceiver.setReceiver(this);
        return mDetachableReceiver;
    }

    @Override
    public void onStart() {
        super.onStart();
        connectivityListener = new NetworkConnectivityListener();
        connectivityListener.registerHandler(connHandler, CONNECTIVITY_MSG);

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(CloudPlaybackService.META_CHANGED);
        playbackFilter.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        playbackFilter.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
        getActivity().registerReceiver(mPlaybackStatusListener, new IntentFilter(playbackFilter));

        IntentFilter generalIntentFilter = new IntentFilter();
        generalIntentFilter.addAction(Actions.CONNECTION_ERROR);
        generalIntentFilter.addAction(Actions.LOGGING_OUT);
        getActivity().registerReceiver(mGeneralIntentListener, generalIntentFilter);

        final ScBaseAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof PlayableAdapter) listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mPlaybackStatusListener);
        getActivity().unregisterReceiver(mGeneralIntentListener);
        mIgnorePlaybackStatus = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getListAdapter() != null) getListAdapter().onResume();
    }

    protected void onDataConnectionUpdated(boolean isConnected) {
        if (isConnected) {
            if (getListAdapter().needsItems() && getScActivity().getApp().getAccount() != null) {
                refresh(false);
            }
        }
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
        return (mRefreshTask != null && !AndroidUtils.isTaskFinished(mRefreshTask));
    }

    protected void doneRefreshing() {
        if (isSyncable()) setListLastUpdated();
        if (mListView != null) {
            mListView.onRefreshComplete();
        }
    }

    protected boolean isSyncable() {
        return mContent != null && mContent.isSyncable();
    }

    public void setListLastUpdated() {
        if (mLocalCollection != null && mListView != null && mLocalCollection.last_sync_success > 0) {
            mListView.setLastUpdated(mLocalCollection.last_sync_success);
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
            case ApiSyncService.STATUS_SYNC_ERROR: {
                if (resultData != null && !resultData.getBoolean(mContentUri.toString()) && !isRefreshing()) {
                    doneRefreshing(); // nothing changed
                } else if (mContentInvalid && !isRefreshTaskActive()) {
                    executeRefreshTask();
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
        if (!isRefreshing()){
            executeRefreshTask();
        }
    }

    public void executeRefreshTask() {
        mEmptyCollection.setMode(mLocalCollection == null || mLocalCollection.hasSyncedBefore() ? EmptyCollection.Mode.WAITING_FOR_DATA : EmptyCollection.Mode.WAITING_FOR_SYNC);
        mRefreshTask = buildTask();
        mRefreshTask.execute(getTaskParams(true));
    }

    protected CollectionTask buildTask() {
        return new CollectionTask(SoundCloudApplication.fromContext(getActivity()), this);
    }


    protected CollectionParams getTaskParams(final boolean refresh) {
        CollectionParams params = getListAdapter().getParams(refresh);
        params.request = buildRequest(refresh);
        params.refreshPageItems = !isSyncable();
        return params;
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
        if (mContent == null || !mContent.hasRequest()) return null;
        return !(isRefresh) && !TextUtils.isEmpty(mNextHref) ? new Request(mNextHref) : mContent.request(mContentUri);
    }


    private void refreshSyncData() {
        if (isSyncable() && mLocalCollection != null) {
            setListLastUpdated();

            if (mLocalCollection.shouldAutoRefresh() && !isRefreshing()) {
                refresh(false);
                // this is to show the user something at the initial load
                if (!mLocalCollection.hasSyncedBefore() && mListView != null) {
                    mListView.setRefreshing();
                }
            }
        }
    }

    public void refresh(final boolean userRefresh) {
        if (userRefresh) {
            if (getListAdapter() instanceof FollowStatus.Listener) {
                FollowStatus.get(getActivity()).requestUserFollowings((FollowStatus.Listener) getListAdapter());
            }
        }

        if (isSyncable()) {
            requestSync();
        } else if (getActivity() != null) {
            executeRefreshTask();
            getListAdapter().notifyDataSetChanged();
        }
    }

    public void reset() {
        resetEmptyCollection();
        mNextHref = "";
        mKeepGoing = true;
        clearRefreshTask();
        clearUpdateTask();

        final ScBaseAdapter adp = getListAdapter();
        if (adp != null) {
            adp.clearData();
            setListAdapter(adp);
            adp.notifyDataSetChanged();
        }
    }

    private void resetEmptyCollection() {
        mEmptyCollection.setMode((mLocalCollection == null || mLocalCollection.hasSyncedBefore()) ?
                (canAppend() ? EmptyCollection.Mode.WAITING_FOR_DATA : EmptyCollection.Mode.IDLE)
                : EmptyCollection.Mode.WAITING_FOR_SYNC);
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

    protected boolean handleResponseCode(int responseCode) {
        switch (responseCode) {
            case HttpStatus.SC_CONTINUE: // do nothing
            case HttpStatus.SC_OK: // do nothing
            case HttpStatus.SC_NOT_MODIFIED:
                return true;

            case HttpStatus.SC_UNAUTHORIZED:
                //getActivity().safeShowDialog(Consts.Dialogs.DIALOG_UNAUTHORIZED);
                //noinspection fallthrough
            default:
                Log.w(TAG, "unexpected responseCode " + responseCode);
                return false;
        }
    }

    @Override
    public void onRefresh() {
        refresh(true);
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

    @Override
    public void onPostTaskExecute(ReturnData data) {
        mKeepGoing = data.keepGoing;
        if (data.success) mNextHref = data.nextHref;
        getListAdapter().handleTaskReturnData(data);

        if (data.wasRefresh && !waitingOnInitialSync()) doneRefreshing();
        mEmptyCollection.setMode(waitingOnInitialSync() ? EmptyCollection.Mode.WAITING_FOR_SYNC : EmptyCollection.Mode.IDLE);
        handleResponseCode(data.responseCode);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (getListAdapter().shouldRequestNextPage(firstVisibleItem, visibleItemCount, totalItemCount) && mKeepGoing) {
            append();
        }
    }

    protected void append() {
        buildTask().execute(getTaskParams(false));
        getListAdapter().setIsLoadingData(true);
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


    private BroadcastReceiver mPlaybackStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIgnorePlaybackStatus || !(getListAdapter() instanceof PlayableAdapter)) return;

            final String action = intent.getAction();
            if (CloudPlaybackService.META_CHANGED.equals(action)
                    || CloudPlaybackService.PLAYBACK_COMPLETE.equals(action)
                    || CloudPlaybackService.PLAYSTATE_CHANGED.equals(action)) {
                getListAdapter().notifyDataSetChanged();
            }
        }
    };


    private BroadcastReceiver mGeneralIntentListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Actions.LOGGING_OUT.equals(intent.getAction())) {
                // alert lists?
            }
        }
    };
}