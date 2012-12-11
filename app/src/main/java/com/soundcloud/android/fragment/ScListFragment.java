package com.soundcloud.android.fragment;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.utils.AndroidUtils.isTaskFinished;

import com.actionbarsherlock.app.SherlockListFragment;
import com.soundcloud.android.imageloader.ImageLoader;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.ActivityAdapter;
import com.soundcloud.android.adapter.CommentAdapter;
import com.soundcloud.android.adapter.FriendAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.adapter.PlayableAdapter;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.adapter.SearchAdapter;
import com.soundcloud.android.adapter.SoundAssociationAdapter;
import com.soundcloud.android.adapter.TrackAdapter;
import com.soundcloud.android.adapter.UserAdapter;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.ContentStats;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.collection.CollectionParams;
import com.soundcloud.android.task.collection.CollectionTask;
import com.soundcloud.android.task.collection.ReturnData;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.view.EmptyListView;
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
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ListView;

public class ScListFragment extends SherlockListFragment implements PullToRefreshBase.OnRefreshListener,
                                                            DetachableResultReceiver.Receiver,
                                                            LocalCollection.OnChangeListener,
                                                            CollectionTask.Callback,
                                                            AbsListView.OnScrollListener,
                                                            ImageLoader.LoadBlocker {
    private static final int CONNECTIVITY_MSG = 0;

    @Nullable private ScListView mListView;
    private final DetachableResultReceiver mDetachableReceiver = new DetachableResultReceiver(new Handler());

    protected @Nullable EmptyListView mEmptyListView;
    private @Nullable Content mContent;
    private @NotNull Uri mContentUri;
    private NetworkConnectivityListener connectivityListener;
    private @Nullable CollectionTask mRefreshTask;
    private @Nullable LocalCollection mLocalCollection;
    private ChangeObserver mChangeObserver;
    private boolean mIgnorePlaybackStatus, mKeepGoing, mPendingSync;
    private CollectionTask mAppendTask;
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

    public ScListFragment() {
    }

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
            contentResolver.registerContentObserver(mContentUri, true, mChangeObserver);
            refreshSyncData();
        }
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
    public void onDestroy() {
        super.onDestroy();
        stopObservingChanges();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && mContent != null) {
            switch (mContent) {
                case ME_SOUND_STREAM:
                case ME_ACTIVITIES:
                    ContentStats.updateCount(getActivity(), mContent, 0);
                    ContentStats.setLastSeen(getActivity(), mContent, System.currentTimeMillis());
                    break;
            }
        }
        final ScBaseAdapter adapter = getListAdapter();
        if (adapter != null) adapter.onResume();

        if (mPendingSync){
            mPendingSync = false;
            requestSync();
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ScBaseAdapter<?> adapter;
        if (getListAdapter() == null && mContent != null) {
            switch (mContent) {
                case ME_SOUND_STREAM:
                case ME_ACTIVITIES:
                    adapter = new ActivityAdapter(getActivity(), mContentUri);
                    break;
                case ME_FOLLOWERS:
                case ME_FOLLOWINGS:
                case USER_FOLLOWINGS:
                case USER_FOLLOWERS:
                case TRACK_LIKERS:
                case TRACK_REPOSTERS:
                case SUGGESTED_USERS:
                    adapter = new UserAdapter(getActivity(), mContentUri);
                    break;
                case ME_FRIENDS:
                    adapter = new FriendAdapter(getActivity(), mContentUri);
                    break;
                case ME_SOUNDS:
                    adapter = new MyTracksAdapter(getScActivity(), mContentUri);
                    break;
                case ME_LIKES:
                case USER_LIKES:
                case USER_SOUNDS:
                    adapter = new SoundAssociationAdapter(getActivity(), mContentUri);
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
            configureEmptyView();
            if (canAppend()) {
                append(false);
            } else {
                mKeepGoing = false;
            }
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();

        FrameLayout root = new FrameLayout(context);

        mListView = configureList(new ScListView(getActivity()));
        mListView.setOnRefreshListener(this);
        mListView.setOnScrollListener(this);
        setEmptyCollection((mEmptyListView == null) ?
                EmptyListView.fromContent(context, mContent) : mEmptyListView);

        mListView.setEmptyView(mEmptyListView);

        if (isRefreshing() || waitingOnInitialSync()){
            mListView.setRefreshing(false);
        }

        root.addView(mListView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        return root;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final ScBaseAdapter adapter = getListAdapter();
        if (adapter == null) return;

        switch (adapter.handleListItemClick(position - getListView().getHeaderViewsCount(), id)){
            case ScBaseAdapter.ItemClickResults.LEAVING:
                mIgnorePlaybackStatus = true;
                break;
            default:
        }
    }

    public void setEmptyCollection(EmptyListView emptyCollection){
        mEmptyListView = emptyCollection;
        configureEmptyView();
        if (getView() != null && getListView() != null) {
            getListView().setEmptyView(emptyCollection);
        }
    }

    @Nullable
    public ScActivity getScActivity() {
        return (ScActivity) getActivity();
    }

    @Override @Nullable
    public ScBaseAdapter getListAdapter() {
        return (ScBaseAdapter) super.getListAdapter();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
            case ApiSyncService.STATUS_SYNC_ERROR: {

                final boolean nothingChanged = resultData != null && !resultData.getBoolean(mContentUri.toString());
                if (nothingChanged && !isRefreshing()) {
                    doneRefreshing();

                    // first time user with no account data. this will force the empty screen that was held back earlier
                    final ScBaseAdapter adapter = getListAdapter();
                    if (!waitingOnInitialSync() && adapter != null && adapter.getItemCount() == 0) {
                        mKeepGoing = true;
                        append(false);
                    }
                } else if (!nothingChanged) {
                    // something was changed by the sync, if we aren't refreshing already, do it
                    if (!isRefreshTaskActive()) {
                        executeRefreshTask();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onLocalCollectionChanged() {
        refreshSyncData();
    }

    @Override
    public void onPostTaskExecute(ReturnData data) {
        final ScBaseAdapter adapter = getListAdapter();
        if (adapter == null) return;

        if (data.success) {
            mNextHref = data.nextHref;
        }

        if (data.wasRefresh) {
            mRefreshTask = null; // allows isRefreshing to return false for display purposes
        } else {
            mKeepGoing = data.keepGoing;
        }

        adapter.handleTaskReturnData(data);
        handleResponseCode(data.responseCode);

        final boolean notRefreshing = (data.wasRefresh || !isRefreshing()) && !waitingOnInitialSync();
        if (notRefreshing) {
            doneRefreshing();
        } else {
            configureEmptyView();
        }

        if (adapter.isEmpty() && mKeepGoing){
            // this can happen if we manually filter out the entire collection (e.g. all playlists)
            append(true);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState){
            case SCROLL_STATE_FLING:
            case SCROLL_STATE_TOUCH_SCROLL:
                ImageLoader.get(getActivity()).block(this);
                break;
            case SCROLL_STATE_IDLE:
                ImageLoader.get(getActivity()).unblock(this);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        final ScBaseAdapter adapter = getListAdapter();
        if (adapter != null
            && adapter.shouldRequestNextPage(firstVisibleItem, visibleItemCount, totalItemCount)
            && canAppend()) {
            append(false);
        }
    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        refresh(true);
    }


    protected Request getRequest(boolean isRefresh) {
        if (mContent == null || !mContent.hasRequest()) return null;
        return !(isRefresh) && !TextUtils.isEmpty(mNextHref) ? new Request(mNextHref) : mContent.request(mContentUri);
    }

    protected boolean canAppend() {
        return mKeepGoing && !waitingOnInitialSync();
    }

    protected void refresh(final boolean userRefresh) {
        // this needs to happen regardless of context/adapter availability, it will setup a pending sync if needed
        if (isSyncable()) {
            requestSync();
        }

        final ScBaseAdapter adapter = getListAdapter();
        if (adapter != null) {
            if (userRefresh) {
                if (adapter instanceof FollowStatus.Listener) {
                    FollowStatus.get(getActivity()).requestUserFollowings((FollowStatus.Listener) adapter);
                }
            }
            if (!isSyncable() && getActivity() != null) {
                executeRefreshTask();
                adapter.notifyDataSetChanged();
            }
        }
    }

    protected void reset() {
        mNextHref = "";
        mKeepGoing = true;
        clearRefreshTask();
        configureEmptyView();

        final ScBaseAdapter adp = getListAdapter();
        if (adp != null) {
            adp.clearData();
            setListAdapter(adp);
            adp.notifyDataSetChanged();
        }

        if (canAppend()) append(false);
    }

    protected void configureEmptyView(){
        final boolean wait = canAppend() || isRefreshing() || waitingOnInitialSync();
        if (mEmptyListView != null) {
            mEmptyListView.setMode(wait ? EmptyListView.Mode.WAITING_FOR_DATA : EmptyListView.Mode.IDLE);
        }
    }

    private void executeRefreshTask() {
        final Context context = getActivity();
        final ScBaseAdapter adapter = getListAdapter();
        if (context != null && adapter != null) {
            mRefreshTask = buildTask(context);
            mRefreshTask.execute(getTaskParams(adapter, true));
        }
        configureEmptyView();
    }


    private ScListView configureList(ScListView lv) {
        lv.getRefreshableView().setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        lv.getRefreshableView().setFastScrollEnabled(false);
        return lv;
    }

    private void onDataConnectionUpdated(boolean isConnected) {
        final ScBaseAdapter adapter = getListAdapter();
        if (isConnected && adapter != null) {
            if (adapter.needsItems() && getScActivity().getApp().getAccount() != null) {
                refresh(false);
            }
        }
    }

    private DetachableResultReceiver getReceiver() {
        mDetachableReceiver.setReceiver(this);
        return mDetachableReceiver;
    }


    private void requestSync() {
        if (getActivity() != null && mContent != null) {
            Intent intent = new Intent(getActivity(), ApiSyncService.class)
                    .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver())
                    .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                    .setData(mContent.uri);
            getActivity().startService(intent);
        } else {
            mPendingSync = true;
        }
    }

    private boolean isRefreshing() {
        if (mLocalCollection != null) {
            return mLocalCollection.sync_state == LocalCollection.SyncState.SYNCING
                    || mLocalCollection.sync_state == LocalCollection.SyncState.PENDING
                    || isRefreshTaskActive();
        } else {
            return isRefreshTaskActive();
        }
    }

    private boolean waitingOnInitialSync() {
        return (mLocalCollection != null && !mLocalCollection.hasSyncedBefore());
    }

    private boolean isRefreshTaskActive() {
        return (mRefreshTask != null && !AndroidUtils.isTaskFinished(mRefreshTask));
    }

    private void doneRefreshing() {
        if (isSyncable()) setListLastUpdated();
        if (mListView != null) {
            mListView.onRefreshComplete();
        }
        configureEmptyView();
    }

    private boolean isSyncable() {
        return mContent != null && mContent.isSyncable();
    }


    private void setListLastUpdated() {
        if (mLocalCollection != null && mListView != null && mLocalCollection.last_sync_success > 0) {
            mListView.setLastUpdated(mLocalCollection.last_sync_success);
        }
    }

    private void stopObservingChanges() {
        if (mChangeObserver != null) {
            getActivity().getContentResolver().unregisterContentObserver(mChangeObserver);
            mChangeObserver = null;
            if (mLocalCollection != null) {
                mLocalCollection.stopObservingSelf();
            }
        }
    }

    private void onContentChanged() {
        final ScBaseAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof ActivityAdapter && ((ActivityAdapter) listAdapter).isExpired(mLocalCollection)) {
            executeRefreshTask();
        }
    }

    private CollectionTask buildTask(Context context) {
        return new CollectionTask(SoundCloudApplication.fromContext(context), this);
    }

    private CollectionParams getTaskParams(@NotNull ScBaseAdapter adapter, final boolean refresh) {
        CollectionParams params = adapter.getParams(refresh);
        params.request = buildRequest(refresh);
        params.refreshPageItems = !isSyncable();
        return params;
    }

    private Request buildRequest(boolean isRefresh) {
        Request request = getRequest(isRefresh);
        if (request != null) {
            request.add("linked_partitioning", "1");
            request.add("limit", Consts.COLLECTION_PAGE_SIZE);
        }
        return request;
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

    private void clearRefreshTask() {
        if (mRefreshTask != null && !AndroidUtils.isTaskFinished(mRefreshTask)) mRefreshTask.cancel(true);
        mRefreshTask = null;
    }

    private boolean handleResponseCode(int responseCode) {
        switch (responseCode) {
            case HttpStatus.SC_CONTINUE: // do nothing
            case HttpStatus.SC_OK: // do nothing
            case HttpStatus.SC_NOT_MODIFIED:
                return true;

            case HttpStatus.SC_UNAUTHORIZED:
                final FragmentActivity activity = getActivity();
                if (activity != null && !activity.isFinishing()) {
                    activity.showDialog(Consts.Dialogs.DIALOG_UNAUTHORIZED);
                }
                //noinspection fallthrough
            default:
                Log.w(TAG, "unexpected responseCode " + responseCode);
                return false;
        }
    }

    private final Handler connHandler = new Handler() {
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

    private void append(boolean force) {
        final Context context = getActivity();
        final ScBaseAdapter adapter = getListAdapter();
        if (context == null || adapter == null) return; // has been detached

        if (force || isTaskFinished(mAppendTask)){
            mAppendTask = buildTask(context);
            mAppendTask.executeOnThreadPool(getTaskParams(adapter, false));
        }
        adapter.setIsLoadingData(true);
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
            if (mChangeObserver != null) onContentChanged();
        }
    }

    private BroadcastReceiver mPlaybackStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final ScBaseAdapter adapter = getListAdapter();
            if (mIgnorePlaybackStatus || !(adapter instanceof PlayableAdapter)) return;

            final String action = intent.getAction();
            if (CloudPlaybackService.META_CHANGED.equals(action)
                || CloudPlaybackService.PLAYBACK_COMPLETE.equals(action)
                || CloudPlaybackService.PLAYSTATE_CHANGED.equals(action)) {

                adapter.notifyDataSetChanged();
            }
        }
    };

    private BroadcastReceiver mGeneralIntentListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Actions.LOGGING_OUT.equals(intent.getAction())) {
                stopObservingChanges();
            }
        }
    };
}