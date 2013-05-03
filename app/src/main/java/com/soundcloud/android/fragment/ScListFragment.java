package com.soundcloud.android.fragment;

import static com.soundcloud.android.utils.AndroidUtils.isTaskFinished;

import com.actionbarsherlock.app.SherlockListFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.Wrapper;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.ActivityAdapter;
import com.soundcloud.android.adapter.CommentAdapter;
import com.soundcloud.android.adapter.DefaultPlayableAdapter;
import com.soundcloud.android.adapter.FriendAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
import com.soundcloud.android.adapter.PlayableAdapter;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.adapter.SearchAdapter;
import com.soundcloud.android.adapter.SoundAssociationAdapter;
import com.soundcloud.android.adapter.UserAdapter;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.ContentStats;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.SyncStateManager;
import com.soundcloud.android.task.collection.CollectionParams;
import com.soundcloud.android.task.collection.CollectionTask;
import com.soundcloud.android.task.collection.ReturnData;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.EmptyListViewFactory;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.BroadcastReceiver;
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

import java.lang.ref.WeakReference;

public class ScListFragment extends SherlockListFragment implements PullToRefreshBase.OnRefreshListener,
                                                            DetachableResultReceiver.Receiver,
                                                            LocalCollection.OnChangeListener,
                                                            CollectionTask.Callback,
                                                            AbsListView.OnScrollListener,
                                                            ImageLoader.LoadBlocker {
    private static final int CONNECTIVITY_MSG = 0;
    public static final String TAG = ScListFragment.class.getSimpleName();
    private static final String EXTRA_CONTENT_URI = "contentUri";

    private @Nullable ScListView mListView;
    private ScBaseAdapter<?> mAdapter;
    private final DetachableResultReceiver mDetachableReceiver = new DetachableResultReceiver(new Handler());

    protected @Nullable EmptyListView mEmptyListView;
    private EmptyListViewFactory mEmptyListViewFactory;

    private Content mContent;
    private Uri mContentUri;
    private NetworkConnectivityListener connectivityListener;
    private Handler connectivityHandler;
    private @Nullable CollectionTask mRefreshTask;
    private @Nullable LocalCollection mLocalCollection;
    private ChangeObserver mChangeObserver;
    private boolean mIgnorePlaybackStatus, mKeepGoing, mPendingSync;
    private CollectionTask mAppendTask;
    protected String mNextHref;

    protected int mStatusCode;

    private @Nullable BroadcastReceiver mPlaylistChangedReceiver;

    private SyncStateManager mSyncStateManager;

    private int mRetainedListPosition;

    public static ScListFragment newInstance(Content content) {
        return newInstance(content.uri);
    }

    public static ScListFragment newInstance(Uri contentUri) {
        ScListFragment fragment = new ScListFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_CONTENT_URI, contentUri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mContentUri == null) {
            // only should happen once
            mContentUri = (Uri) getArguments().get(EXTRA_CONTENT_URI);
            mContent = Content.match(mContentUri);

            if (mContent.isSyncable()) {
                mSyncStateManager = new SyncStateManager();
                mLocalCollection = mSyncStateManager.fromContentAsync(mContentUri, this);
                mChangeObserver = new ChangeObserver();
                refreshSyncData();
            }
        }
        // should happen once per activity lifecycle
        startObservingChanges();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mEmptyListViewFactory = new EmptyListViewFactory().forContent(getActivity(), mContent, null);
        mKeepGoing = true;
        setupListAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();

        FrameLayout root = new FrameLayout(context);

        mListView = configureList(new ScListView(getActivity()));
        mListView.setOnRefreshListener(this);
        mListView.setOnScrollListener(this);

        if (mEmptyListView == null) {
            mEmptyListView = mEmptyListViewFactory.build(getActivity());
        }
        mEmptyListView.setStatus(mStatusCode);
        mListView.setEmptyView(mEmptyListView);

        configurePullToRefreshState();

        if (isRefreshing() || waitingOnInitialSync()){
            final ScBaseAdapter listAdapter = getListAdapter();
            if (listAdapter == null || listAdapter.isEmpty()){
                configureEmptyView();
            } else if (isRefreshing()){
                mListView.setRefreshing(false);
            }
        }

        root.addView(mListView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mSyncStateManager != null){
            mLocalCollection = mSyncStateManager.fromContentAsync(mContentUri, this);
        }

        connectivityListener = new NetworkConnectivityListener();
        connectivityHandler = new ConnectivityHandler(this, connectivityListener);
        connectivityListener.registerHandler(connectivityHandler, CONNECTIVITY_MSG);

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(CloudPlaybackService.META_CHANGED);
        playbackFilter.addAction(CloudPlaybackService.PLAYBACK_COMPLETE);
        playbackFilter.addAction(CloudPlaybackService.PLAYSTATE_CHANGED);
        getActivity().registerReceiver(mPlaybackStatusListener, new IntentFilter(playbackFilter));

        IntentFilter generalIntentFilter = new IntentFilter();
        generalIntentFilter.addAction(Actions.CONNECTION_ERROR);
        generalIntentFilter.addAction(Actions.LOGGING_OUT);
        getActivity().registerReceiver(mGeneralIntentListener, generalIntentFilter);

        if (mContent.shouldListenForPlaylistChanges()) {
            listenForPlaylistChanges();
        }

        final ScBaseAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof PlayableAdapter) listAdapter.notifyDataSetChanged();

        if (mRetainedListPosition > 0) {
            mListView.getRefreshableView().setSelection(mRetainedListPosition);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopListening();
        mIgnorePlaybackStatus = false;
        mRetainedListPosition = mListView.getRefreshableView().getFirstVisiblePosition();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mContent != null) {
            switch (mContent) {
                case ME_SOUND_STREAM:
                case ME_ACTIVITIES:
                    ContentStats.updateCount(getActivity(), mContent, 0);
                    ContentStats.setLastSeen(getActivity(), mContent, System.currentTimeMillis());
                    break;
            }
        }
        final ScBaseAdapter adapter = getListAdapter();
        if (adapter != null) adapter.onResume(getScActivity());

        if (mPendingSync){
            mPendingSync = false;
            requestSync();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // null out view references to avoid leaking the current Context in case we detach/re-attach
        mListView = null;
        mEmptyListView = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stopObservingChanges();
    }

    private void startObservingChanges() {
        if (mChangeObserver != null) {
            getActivity().getContentResolver().registerContentObserver(mContentUri, true, mChangeObserver);
        }
    }

    private void stopObservingChanges(){
        if (mChangeObserver != null) {
            getActivity().getContentResolver().unregisterContentObserver(mChangeObserver);
            mChangeObserver = null;
        }
    }

    private void stopListening() {
        getActivity().unregisterReceiver(mPlaybackStatusListener);
        getActivity().unregisterReceiver(mGeneralIntentListener);
        if (mContent.shouldListenForPlaylistChanges() && mPlaylistChangedReceiver != null) {
            getActivity().unregisterReceiver(mPlaylistChangedReceiver);
        }

        if (mSyncStateManager != null && mLocalCollection != null) {
            mSyncStateManager.removeChangeListener(mLocalCollection);
        }
    }

    private void setupListAdapter() {
        if (getListAdapter() == null && mContent != null) {
            switch (mContent) {
                case ME_SOUND_STREAM:
                case ME_ACTIVITIES:
                    mAdapter = new ActivityAdapter(mContentUri);
                    break;
                case ME_FOLLOWERS:
                case ME_FOLLOWINGS:
                case USER_FOLLOWINGS:
                case USER_FOLLOWERS:
                case TRACK_LIKERS:
                case TRACK_REPOSTERS:
                case PLAYLIST_LIKERS:
                case PLAYLIST_REPOSTERS:
                case SUGGESTED_USERS:
                    mAdapter = new UserAdapter(mContentUri);
                    break;
                case ME_FRIENDS:
                    mAdapter = new FriendAdapter(mContentUri);
                    break;
                case ME_SOUNDS:
                    mAdapter = new MyTracksAdapter(getScActivity(), mContentUri);
                    break;
                case ME_LIKES:
                case USER_LIKES:
                case USER_SOUNDS:
                    mAdapter = new SoundAssociationAdapter(mContentUri);
                    break;
                case SEARCH:
                    mAdapter = new SearchAdapter(Content.SEARCH.uri);
                    break;
                case TRACK_COMMENTS:
                    mAdapter = new CommentAdapter(mContentUri);
                    break;
                case ME_PLAYLISTS:
                case USER_PLAYLISTS:
                default:
                    mAdapter = new DefaultPlayableAdapter(mContentUri);
            }
            setListAdapter(mAdapter);
            configureEmptyView();
            if (canAppend()) {
                append(false);
            } else {
                mKeepGoing = false;
            }
        }
    }

    private void listenForPlaylistChanges() {
        mPlaylistChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (mAdapter != null) {
                    PlayableAdapter playableAdapter = (PlayableAdapter) mAdapter;

                    long playlistId = intent.getLongExtra(Playlist.EXTRA_ID, -1);
                    int newTracksCount = intent.getIntExtra(Playlist.EXTRA_TRACKS_COUNT, -1);

                    for (int i = 0; i < mAdapter.getCount(); i++) {
                        Playable playable = playableAdapter.getPlayable(i);
                        if (playable instanceof Playlist && playable.id == playlistId) {
                            Playlist playlist = (Playlist) playable;
                            // TODO: this should be updated by the model manager

                            playlist.setTrackCount(newTracksCount);
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Playlist.ACTION_CONTENT_CHANGED);
        getActivity().registerReceiver(mPlaylistChangedReceiver, intentFilter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final ScBaseAdapter adapter = getListAdapter();
        if (adapter == null) return;

        switch (adapter.handleListItemClick(getActivity(), position - getListView().getHeaderViewsCount(), id)){
            case ScBaseAdapter.ItemClickResults.LEAVING:
                mIgnorePlaybackStatus = true;
                break;
            default:
        }
    }

    public void setEmptyViewFactory(EmptyListViewFactory factory) {
        mEmptyListViewFactory = factory;
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
                log("Returned from sync. Change: " + !nothingChanged);
                if (nothingChanged && !isRefreshTaskActive()) {
                    doneRefreshing();
                    checkAllowInitalAppend();

                } else if (!nothingChanged) {
                    // something was changed by the sync, if we aren't refreshing already, do it
                    if (!isRefreshTaskActive()) {
                        log("Something changed, Refreshing....");
                        executeRefreshTask();
                    } else {
                        log("Something changed, Already Refreshing, skipping refresh.");
                    }
                }
                break;
            }
        }
    }

    /**
     * This will allow the empty screen to be shown, in case
     * {@link this#waitingOnInitialSync())} was true earlier, suppressing it.
     */
    private void checkAllowInitalAppend() {
        log("Should allow initial appending: [waitingOnInitialSync:" + waitingOnInitialSync() + ",mKeepGoing:" + mKeepGoing + "]"  );
        final ScBaseAdapter adapter = getListAdapter();
        if (!mKeepGoing && !waitingOnInitialSync() && adapter != null && adapter.needsItems()) {
            mKeepGoing = true;
            append(false);
        }
    }

    @Override
    public void onLocalCollectionChanged(LocalCollection localCollection) {
        mLocalCollection = localCollection;
        configurePullToRefreshState();
        log("Local collection changed " + mLocalCollection);
        // do not autorefresh me_followings based on observing because this would refresh everytime you use the in list toggles
        if (mContent != Content.ME_FOLLOWINGS || getListAdapter().needsItems()) {
            refreshSyncData();
        } else {
            checkAllowInitalAppend();
        }
    }

    /**
     * Set the pull to refresh state based on having a valid local collection (that has finished any async initialization)
     * that is also in idle state. If not in that state, then set the loading state to prevent unwanted refreshes/syncs
     */
    private void configurePullToRefreshState() {
        if (mListView != null && mLocalCollection != null) {
            if (mLocalCollection.isIdle()) {
                if (mListView.isRefreshing()) mListView.onRefreshComplete();
            } else if (!mListView.isRefreshing()){
                mListView.setRefreshing(false);
            }
        }
    }

    @Override
    public void onPostTaskExecute(ReturnData data) {
        final ScBaseAdapter adapter = getListAdapter();
        if (adapter == null) return;

        if (data.success) {
            mNextHref = data.nextHref;
        }

        // this will represent the end append state of the list on an append, or on a successful refresh
        if (!data.wasRefresh || data.success){
            mKeepGoing = data.keepGoing;
        }

        if (data.wasRefresh) {
            mRefreshTask = null; // allows isRefreshing to return false for display purposes
        }

        mKeepGoing = data.keepGoing;
        adapter.handleTaskReturnData(data, getActivity());
        configureEmptyView(data.responseCode);

        final boolean notRefreshing = (data.wasRefresh || !isRefreshing()) && !waitingOnInitialSync();
        if (notRefreshing) {
            doneRefreshing();
        }

        if (adapter.isEmpty() && mKeepGoing){
            // this can happen if we manually filter out the entire collection (e.g. all playlists)
            append(true);
        }

        // show unauthorized dialog if applicable
        if (data.responseCode == HttpStatus.SC_UNAUTHORIZED) {
            final FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.sendBroadcast(new Intent(Consts.GeneralIntents.UNAUTHORIZED));
            }
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
        if (!isRefresh && !TextUtils.isEmpty(mNextHref)) {
            return new Request(mNextHref);
        } else if (mContent != null && mContent.hasRequest()) {
            return mContent.request(mContentUri);
        } else {
            return null;
        }
    }

    protected boolean canAppend() {
        log("Can Append [mKeepGoing: " + mKeepGoing + ", waitingOnInitialSync: "+waitingOnInitialSync()+"]");
        return mKeepGoing && !waitingOnInitialSync();
    }

    protected void refresh(final boolean userRefresh) {
        log("Refresh [userRefresh: " + userRefresh + "]");

        // this needs to happen regardless of context/adapter availability, it will setup a pending sync if needed
        if (isSyncable()) {
            requestSync();
        }

        final ScBaseAdapter adapter = getListAdapter();
        if (adapter != null && getActivity() != null) {
            if (userRefresh) {
                adapter.refreshCreationStamps(getActivity());
                if (adapter instanceof FollowStatus.Listener) {
                    FollowStatus.get().requestUserFollowings((FollowStatus.Listener) adapter);
                }
            }
            if (!isSyncable()) {
                executeRefreshTask();
                adapter.notifyDataSetChanged();
            }
        }
    }

    protected void reset() {
        mNextHref = "";
        mKeepGoing = true;
        clearRefreshTask();
        clearAppendTask();
        configureEmptyView();

        final ScBaseAdapter adp = getListAdapter();
        if (adp != null) {
            adp.clearData();
            setListAdapter(adp);
            adp.notifyDataSetChanged();
        }

        if (canAppend()) append(false);
    }

    protected void configureEmptyView() {
        configureEmptyView(EmptyListView.Status.OK);
    }

    protected void configureEmptyView(int statusCode) {
        final boolean wait = canAppend() || isRefreshing() || waitingOnInitialSync();
        log("Configure empty view [waiting:" + wait + "]");
        mStatusCode = wait ? EmptyListView.Status.WAITING : statusCode;
        if (mEmptyListView != null) {
            mEmptyListView.setStatus(mStatusCode);
        }
    }

    private void executeRefreshTask() {
        final Context context = getActivity();
        final ScBaseAdapter adapter = getListAdapter();
        if (context != null && adapter != null) {
            mRefreshTask = buildTask(context);
            mRefreshTask.execute(getTaskParams(adapter, true));
        }

        if (mListView != null && !mListView.isRefreshing()) {
            configureEmptyView();
        }
    }


    private ScListView configureList(ScListView lv) {
        lv.getRefreshableView().setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        lv.getRefreshableView().setFastScrollEnabled(false);
        return lv;
    }

    private void onDataConnectionUpdated(boolean isConnected) {
        final ScBaseAdapter adapter = getListAdapter();
        if (isConnected && adapter != null) {
            if (adapter.needsItems() && getScActivity() != null && getScActivity().getApp().getAccount() != null) {
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
            log("Requesting Sync");
            Intent intent = new Intent(getActivity(), ApiSyncService.class)
                    .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver())
                    .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                    .setData(mContent.uri);
            getActivity().startService(intent);
        } else {
            log("Bypassing sync request, no context");
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
    }

    private boolean isSyncable() {
        return mContent != null && mContent.isSyncable();
    }


    private void setListLastUpdated() {
        if (mLocalCollection != null && mListView != null && mLocalCollection.last_sync_success > 0) {
            mListView.setLastUpdated(mLocalCollection.last_sync_success);
        }
    }

    private void onContentChanged() {
        final ScBaseAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof ActivityAdapter && !((ActivityAdapter) listAdapter).isExpired(mLocalCollection)) {
            log("Activity content has changed, no newer items, skipping refresh");
        } else {
            log("Content changed, adding newer items.");
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
            request.add(Wrapper.LINKED_PARTITIONING, "1");
            request.add("limit", Consts.COLLECTION_PAGE_SIZE);
        }
        return request;
    }


    private void refreshSyncData() {
        if (isSyncable() && mLocalCollection != null) {
            setListLastUpdated();
            if (mLocalCollection.shouldAutoRefresh()) {
                log("Auto refreshing content");
                if (!isRefreshing()) {
                    refresh(false);
                    if (mListView != null) mListView.setRefreshing(false);
                }
            } else {
                log("Skipping auto refresh");
                checkAllowInitalAppend();
            }
        }
    }

    private void clearRefreshTask() {
        if (mRefreshTask != null && !AndroidUtils.isTaskFinished(mRefreshTask)) mRefreshTask.cancel(true);
        mRefreshTask = null;
    }

    private void clearAppendTask() {
        if (mAppendTask != null && !AndroidUtils.isTaskFinished(mAppendTask)) mAppendTask.cancel(true);
        mAppendTask = null;
    }

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

    private static final class ConnectivityHandler extends Handler {
        private WeakReference<ScListFragment> mFragmentRef;
        private WeakReference<NetworkConnectivityListener> mListenerRef;

        private ConnectivityHandler(ScListFragment fragment, NetworkConnectivityListener listener) {
            this.mFragmentRef = new WeakReference<ScListFragment>(fragment);
            this.mListenerRef = new WeakReference<NetworkConnectivityListener>(listener);
        }

        @Override
        public void handleMessage(Message msg) {
            final ScListFragment fragment = mFragmentRef.get();
            final NetworkConnectivityListener listener = mListenerRef.get();
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    if (fragment != null && listener != null) {
                        final NetworkInfo networkInfo = listener.getNetworkInfo();
                        if (networkInfo != null) {
                            fragment.onDataConnectionUpdated(networkInfo.isConnectedOrConnecting());
                        }
                    }
                    break;
            }
        }
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
                stopListening();
            }
        }
    };

    private static void log(String msg){
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, msg);
    }
}