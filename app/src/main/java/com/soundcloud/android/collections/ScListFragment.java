package com.soundcloud.android.collections;

import static com.soundcloud.android.playback.PlaybackService.Broadcasts;
import static com.soundcloud.android.utils.AndroidUtils.isTaskFinished;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.activities.ActivitiesAdapter;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.collections.tasks.CollectionParams;
import com.soundcloud.android.collections.tasks.CollectionTask;
import com.soundcloud.android.collections.tasks.ReturnData;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.playlists.PlaylistChangedSubscriber;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.EmptyViewBuilder;
import com.soundcloud.android.view.adapters.PostsAdapter;
import com.soundcloud.android.view.adapters.SoundAdapter;
import com.soundcloud.android.view.adapters.UserAdapter;
import com.soundcloud.rx.eventbus.EventBus;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.subscriptions.CompositeSubscription;

import android.annotation.SuppressLint;
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
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.ListView;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

@Deprecated
public class ScListFragment extends ListFragment implements OnRefreshListener,
        DetachableResultReceiver.Receiver,
        LocalCollection.OnChangeListener,
        CollectionTask.Callback,
        AbsListView.OnScrollListener {
    public static final String TAG = ScListFragment.class.getSimpleName();
    private static final int CONNECTIVITY_MSG = 0;
    private static final String EXTRA_CONTENT_URI = "contentUri";
    private static final String EXTRA_TITLE_ID = "title";
    private static final String EXTRA_USERNAME = "username";
    private static final String EXTRA_SCREEN = "screen";
    private static final String EXTRA_QUERY_SOURCE_INFO = "querySourceInfo";
    private static final String KEY_IS_RETAINED = "is_retained";
    private final DetachableResultReceiver detachableReceiver = new DetachableResultReceiver(new Handler());
    private final BroadcastReceiver playbackStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final ScBaseAdapter adapter = getListAdapter();

            final String action = intent.getAction();
            if (Broadcasts.META_CHANGED.equals(action)
                    || Broadcasts.PLAYSTATE_CHANGED.equals(action)) {

                adapter.notifyDataSetChanged();
            }
        }
    };
    private final DefaultSubscriber<CurrentUserChangedEvent> userEventObserver = new DefaultSubscriber<CurrentUserChangedEvent>() {
        @Override
        public void onNext(CurrentUserChangedEvent args) {
            if (args.getKind() == CurrentUserChangedEvent.USER_REMOVED) {
                stopObservingChanges();
                stopListening();
            }
        }
    };
    protected String nextHref;
    protected EmptyView.Status emptyViewStatus;
    protected PublicApi publicApi;

    @Inject AccountOperations accountOperations;
    @Inject ImageOperations imageOperations;
    @Inject EventBus eventBus;
    @Inject PullToRefreshController pullToRefreshController;
    @Inject FollowingOperations followingOperations;

    @Nullable private ListView listView;
    private ScBaseAdapter<?> adapter;
    @Nullable private EmptyView emptyView;
    private EmptyViewBuilder emptyViewBuilder;
    private Content content;
    private Uri contentUri;
    @Nullable private CollectionTask refreshTask;
    @Nullable private LocalCollection localCollection;
    private ChangeObserver changeObserver;
    private boolean keepGoing, pendingSync;
    private CollectionTask appendTask;
    private SyncStateManager syncStateManager;
    private int retainedListPosition;
    private CompositeSubscription subscription;

    public static ScListFragment newInstance(Content content, Screen screen) {
        return newInstance(content.uri, screen);
    }

    public static ScListFragment newInstance(Uri contentUri, Screen screen) {
        ScListFragment fragment = new ScListFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_CONTENT_URI, contentUri);
        args.putSerializable(EXTRA_SCREEN, screen);
        fragment.setArguments(args);
        return fragment;
    }

    public static ScListFragment newInstance(Uri contentUri, String username, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        ScListFragment fragment = new ScListFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_CONTENT_URI, contentUri);
        args.putString(EXTRA_USERNAME, username);
        args.putSerializable(EXTRA_SCREEN, screen);
        args.putParcelable(EXTRA_QUERY_SOURCE_INFO, searchQuerySourceInfo);
        fragment.setArguments(args);
        return fragment;
    }

    public static ScListFragment newInstance(Uri contentUri, int titleId, Screen screen) {
        ScListFragment fragment = new ScListFragment();
        Bundle args = createArguments(contentUri, titleId, screen);
        fragment.setArguments(args);
        return fragment;
    }

    public static ScListFragment newInstance(Uri contentUri, int titleId, Screen screen, boolean isRetained) {
        ScListFragment fragment = new ScListFragment();
        Bundle args = createArguments(contentUri, titleId, screen);
        args.putBoolean(KEY_IS_RETAINED, isRetained);
        fragment.setArguments(args);
        return fragment;
    }

    protected static Bundle createArguments(Uri contentUri, int titleId, Screen screen) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_CONTENT_URI, contentUri);
        args.putSerializable(EXTRA_SCREEN, screen);
        args.putInt(EXTRA_TITLE_ID, titleId);
        return args;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (contentUri == null) {
            // only should happen once
            contentUri = (Uri) getArguments().get(EXTRA_CONTENT_URI);
            content = Content.match(contentUri);

            if (content.isSyncable()) {
                syncStateManager = new SyncStateManager(activity);
                changeObserver = new ChangeObserver();
            }
        }
        // should happen once per activity lifecycle
        startObservingChanges();
        emptyViewBuilder = new EmptyViewBuilder().forContent(activity, contentUri, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(getArguments().getBoolean(KEY_IS_RETAINED, true));

        SoundCloudApplication.getObjectGraph().inject(this);

        publicApi = PublicApi.getInstance(getActivity());

        keepGoing = true;
        setupListAdapter();
    }

    @Override @SuppressLint("InflateParams")
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        SwipeRefreshLayout pullToRefreshLayout = (SwipeRefreshLayout) inflater.inflate(R.layout.sc_list_fragment, null);
        listView = (ListView) pullToRefreshLayout.findViewById(android.R.id.list);
        listView.setOnScrollListener(imageOperations.createScrollPauseListener(false, true, this));

        emptyView = createEmptyView();
        emptyView.setStatus(emptyViewStatus);
        listView.setEmptyView(emptyView);

        final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        ((ViewGroup) pullToRefreshLayout.findViewById(R.id.list_contents)).addView(emptyView, layoutParams);

        pullToRefreshController.setRefreshListener(this);
        pullToRefreshController.onViewCreated(this, pullToRefreshLayout, savedInstanceState);
        configurePullToRefreshState();

        if (isRefreshing() || waitingOnInitialSync()) {
            final ScBaseAdapter listAdapter = getListAdapter();
            if (listAdapter == null || listAdapter.isEmpty()) {
                configureEmptyView();
            } else if (isRefreshing()) {
                pullToRefreshController.startRefreshing();
            }
        }
        return pullToRefreshLayout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter.onViewCreated();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (syncStateManager != null) {
            localCollection = syncStateManager.fromContentAsync(contentUri, this);
        }

        NetworkConnectivityListener connectivityListener = new NetworkConnectivityListener();
        Handler connectivityHandler = new ConnectivityHandler(this, connectivityListener);
        connectivityListener.registerHandler(connectivityHandler, CONNECTIVITY_MSG);

        IntentFilter playbackFilter = new IntentFilter();
        playbackFilter.addAction(Broadcasts.META_CHANGED);
        playbackFilter.addAction(Broadcasts.PLAYSTATE_CHANGED);
        getActivity().registerReceiver(playbackStatusListener, new IntentFilter(playbackFilter));

        subscription = new CompositeSubscription();
        subscription.add(eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, userEventObserver));

        if (content.shouldListenForPlaylistChanges()) {
            subscription.add(eventBus
                    .queue(EventQueue.ENTITY_STATE_CHANGED)
                    .filter(EntityStateChangedEvent.IS_PLAYLIST_CONTENT_CHANGED_FILTER)
                    .subscribe(new PlaylistChangedSubscriber(adapter)));
        }

        final ScBaseAdapter listAdapter = getListAdapter();
        listAdapter.notifyDataSetChanged();

        if (retainedListPosition > 0) {
            listView.setSelection(retainedListPosition);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopListening();
        retainedListPosition = listView.getFirstVisiblePosition();
    }

    @Override
    @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
    public void onResume() {
        super.onResume();
        if (content != null) {
            switch (content) {
                case ME_SOUND_STREAM:
                case ME_ACTIVITIES:
                    ContentStats.setLastSeen(getActivity(), content, System.currentTimeMillis());
                    break;
            }
        }
        final ScBaseAdapter adapter = getListAdapter();
        if (adapter != null) {
            adapter.onResume(getScActivity());
        }

        if (pendingSync) {
            pendingSync = false;
            requestSync();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pullToRefreshController.onDestroyView(this);

        if (adapter != null) {
            adapter.onDestroyView();
        }
        // null out view references to avoid leaking the current Context in case we detach/re-attach
        listView = null;
        emptyView = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        stopObservingChanges();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final ScBaseAdapter adapter = getListAdapter();
        if (adapter != null) {
            SearchQuerySourceInfo searchQuerySourceInfo = (SearchQuerySourceInfo) getArguments().get(EXTRA_QUERY_SOURCE_INFO);
            adapter.handleListItemClick(getActivity(), position - getListView().getHeaderViewsCount(), id, getScreen(), searchQuerySourceInfo);
        }
    }

    public void setEmptyViewFactory(EmptyViewBuilder factory) {
        emptyViewBuilder = factory;
    }

    @Nullable
    public ScActivity getScActivity() {
        return (ScActivity) getActivity();
    }

    @Override
    @Nullable
    public ScBaseAdapter getListAdapter() {
        return (ScBaseAdapter) super.getListAdapter();
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
            case ApiSyncService.STATUS_SYNC_ERROR:

                final boolean nothingChanged = resultData != null && !resultData.getBoolean(contentUri.toString());
                log("Returned from sync. Change: " + !nothingChanged);
                if (nothingChanged && !isRefreshTaskActive()) {
                    doneRefreshing();
                    checkAllowInitalAppend();
                    final ScBaseAdapter listAdapter = getListAdapter();
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }

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
            default:
                throw new IllegalArgumentException("Unknown resultCode: " + resultCode);
        }
    }

    @Override
    public void onLocalCollectionChanged(LocalCollection localCollection) {
        this.localCollection = localCollection;
        configurePullToRefreshState();
        log("Local collection changed " + this.localCollection);
        // do not autorefresh me_followings based on observing because this would refresh everytime you use the in list toggles
        if (content != Content.ME_FOLLOWINGS || getListAdapter().needsItems()) {
            refreshSyncData();
        } else {
            checkAllowInitalAppend();
        }
    }

    @Override
    public void onPostTaskExecute(ReturnData data) {
        final ScBaseAdapter adapter = getListAdapter();
        if (adapter == null) {
            return;
        }

        if (data.success) {
            nextHref = data.nextHref;
        }

        // this will represent the end append state of the list on an append, or on a successful refresh
        if (!data.wasRefresh || data.success) {
            keepGoing = data.keepGoing;
        }

        if (data.wasRefresh) {
            refreshTask = null; // allows isRefreshing to return false for display purposes
        }

        adapter.handleTaskReturnData(data, getActivity());
        configureEmptyView(data.responseCode);

        final boolean notRefreshing = (data.wasRefresh || !isRefreshing()) && !waitingOnInitialSync();
        if (notRefreshing) {
            doneRefreshing();
        }

        if (adapter.isEmpty() && keepGoing) {
            // this can happen if we manually filter out the entire collection (e.g. all playlists)
            append(true);
        }

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
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
    public void onRefresh() {
        refresh(true);
    }

    protected EmptyView createEmptyView() {
        return emptyViewBuilder.build(getActivity());
    }

    protected String getRelatedUsername() {
        return ScTextUtils.safeToString(getArguments().getString(EXTRA_USERNAME));
    }

    protected Screen getScreen() {
        return (Screen) getArguments().getSerializable(EXTRA_SCREEN);
    }

    protected Request getRequest(boolean isRefresh) {
        if (!isRefresh && !TextUtils.isEmpty(nextHref)) {
            return new Request(nextHref);
        } else if (content != null && content.hasRequest()) {
            return content.request(contentUri);
        } else {
            return null;
        }
    }

    protected boolean canAppend() {
        log("Can Append [keepGoing: " + keepGoing + "]");
        return keepGoing;
    }

    protected void refresh(final boolean userRefresh) {
        log("Refresh [userRefresh: " + userRefresh + "]");

        // this needs to happen regardless of context/adapter availability, it will attach a pending sync if needed
        if (isSyncable()) {
            requestSync();
        }

        final ScBaseAdapter adapter = getListAdapter();
        if (adapter != null && getActivity() != null) {
            if (userRefresh) {
                adapter.refreshCreationStamps(getActivity());
                if (adapter instanceof FollowingOperations.FollowStatusChangedListener) {
                    followingOperations.requestUserFollowings((FollowingOperations.FollowStatusChangedListener) adapter);
                }
            }
            if (!isSyncable()) {
                executeRefreshTask();
                adapter.notifyDataSetChanged();
            }
        }
    }

    protected void reset() {
        nextHref = "";
        keepGoing = true;
        clearRefreshTask();
        clearAppendTask();
        configureEmptyView();

        final ScBaseAdapter adp = getListAdapter();
        if (adp != null) {
            adp.clearData();
            setListAdapter(adp);
            adp.notifyDataSetChanged();
        }

        if (canAppend()) {
            append(false);
        }
    }

    protected void configureEmptyView() {
        configureEmptyView(HttpStatus.SC_OK);
    }

    protected void configureEmptyView(int statusCode) {
        final boolean wait = canAppend() || isRefreshing() || waitingOnInitialSync();
        log("Configure empty view [waiting:" + wait + "]");
        this.emptyViewStatus = wait ? EmptyView.Status.WAITING : emptyViewStatusFromHttpStatus(statusCode);
        if (emptyView != null) {
            emptyView.setStatus(this.emptyViewStatus);
        }
    }

    private EmptyView.Status emptyViewStatusFromHttpStatus(int httpStatus) {
        if (httpStatus >= 400) {
            return EmptyView.Status.SERVER_ERROR;
        } else if (httpStatus >= 200) {
            return EmptyView.Status.OK;
        } else {
            return EmptyView.Status.CONNECTION_ERROR;
        }
    }

    protected void doneRefreshing() {
        if (pullToRefreshController.isAttached()) {
            pullToRefreshController.stopRefreshing();
        }
    }

    protected void onContentChanged() {
        final ScBaseAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof ActivitiesAdapter && !((ActivitiesAdapter) listAdapter).isExpired(localCollection)) {
            log("Activity content has changed, no newer items, skipping refresh");
        } else {
            log("Content changed, adding newer items.");
            executeRefreshTask();
        }
    }

    private void startObservingChanges() {
        if (changeObserver != null) {
            getActivity().getContentResolver().registerContentObserver(contentUri, true, changeObserver);
        }
    }

    private void stopObservingChanges() {
        if (changeObserver != null) {
            getActivity().getContentResolver().unregisterContentObserver(changeObserver);
            changeObserver = null;
        }
    }

    private void stopListening() {
        AndroidUtils.safeUnregisterReceiver(getActivity(), playbackStatusListener);
        subscription.unsubscribe();

        if (syncStateManager != null && localCollection != null) {
            syncStateManager.removeChangeListener(localCollection);
        }
    }

    private void setupListAdapter() {
        if (getListAdapter() == null && content != null) {
            switch (content) {
                case ME_SOUND_STREAM:
                case ME_ACTIVITIES:
                    adapter = new ActivitiesAdapter(contentUri);
                    break;
                case USER_FOLLOWINGS:
                case USER_FOLLOWERS:
                case TRACK_LIKERS:
                case TRACK_REPOSTERS:
                case PLAYLIST_LIKERS:
                case PLAYLIST_REPOSTERS:
                case SUGGESTED_USERS:
                case ME_FOLLOWERS:
                case ME_FOLLOWINGS:
                    adapter = new UserAdapter(contentUri);
                    break;
                case ME_SOUNDS:
                case USER_SOUNDS:
                    adapter = new PostsAdapter(contentUri, getRelatedUsername());
                    break;
                case ME_PLAYLISTS:
                case USER_PLAYLISTS:
                default:
                    adapter = new SoundAdapter(contentUri);
            }
            setListAdapter(adapter);
            configureEmptyView();
            if (canAppend()) {
                append(false);
            } else {
                keepGoing = false;
            }
        }
    }

    /**
     * This will allow the empty screen to be shown, in case
     * {@link this#waitingOnInitialSync())} was true earlier, suppressing it.
     */
    private void checkAllowInitalAppend() {
        log("Should allow initial appending: [waitingOnInitialSync:" + waitingOnInitialSync() + ",keepGoing:" + keepGoing + "]");
        final ScBaseAdapter adapter = getListAdapter();
        if (!keepGoing && !waitingOnInitialSync() && adapter != null && adapter.needsItems()) {
            keepGoing = true;
            append(false);
        }
    }

    /**
     * Set the pull to refresh state based on having a valid local collection (that has finished any async initialization)
     * that is also in idle state. If not in that state, then set the loading state to prevent unwanted refreshes/syncs
     */
    private void configurePullToRefreshState() {
        if (isInLayout() && listView != null && localCollection != null) {
            if (localCollection.isIdle()) {
                pullToRefreshController.stopRefreshing();
            } else {
                pullToRefreshController.startRefreshing();
            }
        }
    }

    private void executeRefreshTask() {
        final Context context = getActivity();
        final ScBaseAdapter adapter = getListAdapter();
        if (context != null && adapter != null) {
            refreshTask = buildTask();
            refreshTask.execute(getTaskParams(adapter, true));
        }

        if (listView != null && !pullToRefreshController.isRefreshing()) {
            configureEmptyView();
        }
    }

    private void onDataConnectionUpdated(boolean isConnected) {
        final ScBaseAdapter adapter = getListAdapter();
        if (isConnected && adapter != null) {
            if (adapter.needsItems() && getScActivity() != null && accountOperations.isUserLoggedIn()) {
                refresh(false);
            }
        }
    }

    private DetachableResultReceiver getReceiver() {
        detachableReceiver.setReceiver(this);
        return detachableReceiver;
    }

    private void requestSync() {

        if (getActivity() != null && content != null) {
            log("Requesting Sync");
            Intent intent = new Intent(getActivity(), ApiSyncService.class)
                    .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver())
                    .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                    .setData(content.uri);
            getActivity().startService(intent);
        } else {
            log("Bypassing sync request, no context");
            pendingSync = true;
        }
    }

    private boolean isRefreshing() {
        if (localCollection != null) {
            return localCollection.sync_state == LocalCollection.SyncState.SYNCING
                    || localCollection.sync_state == LocalCollection.SyncState.PENDING
                    || isRefreshTaskActive();
        } else {
            return isRefreshTaskActive();
        }
    }

    private boolean waitingOnInitialSync() {
        return (localCollection != null && !localCollection.hasSyncedBefore());
    }

    private boolean isRefreshTaskActive() {
        return (refreshTask != null && !AndroidUtils.isTaskFinished(refreshTask));
    }

    private boolean isSyncable() {
        return content != null && content.isSyncable();
    }

    private CollectionTask buildTask() {
        return new CollectionTask(publicApi, this);
    }

    private CollectionParams getTaskParams(@NotNull ScBaseAdapter adapter, final boolean refresh) {
        CollectionParams params = adapter.getParams(refresh);
        params.setRequest(buildRequest(refresh));
        params.refreshPageItems = !isSyncable();
        return params;
    }

    private Request buildRequest(boolean isRefresh) {
        Request request = getRequest(isRefresh);
        if (request != null) {
            request.add(PublicApi.LINKED_PARTITIONING, "1");
            request.add("limit", Consts.LIST_PAGE_SIZE);
        }
        return request;
    }

    private void refreshSyncData() {
        if (isSyncable() && localCollection != null) {
            if (localCollection.shouldAutoRefresh()) {
                log("Auto refreshing content");
                if (!isRefreshing()) {
                    refresh(false);
                    if (pullToRefreshController.isAttached()) {
                        pullToRefreshController.startRefreshing();
                    }
                }
            } else {
                log("Skipping auto refresh");
                checkAllowInitalAppend();
            }
        }
    }

    private void clearRefreshTask() {
        if (refreshTask != null && !AndroidUtils.isTaskFinished(refreshTask)) {
            refreshTask.cancel(true);
        }
        refreshTask = null;
    }

    private void clearAppendTask() {
        if (appendTask != null && !AndroidUtils.isTaskFinished(appendTask)) {
            appendTask.cancel(true);
        }
        appendTask = null;
    }

    private void append(boolean force) {
        final Context context = getActivity();
        final ScBaseAdapter adapter = getListAdapter();
        if (context == null || adapter == null) {
            return;
        } // has been detached

        if (force || isTaskFinished(appendTask)) {
            appendTask = buildTask();
            appendTask.executeOnThreadPool(getTaskParams(adapter, false));
        }
        adapter.setIsLoadingData(true);
    }

    private static void log(String msg) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, msg);
        }
    }

    private static final class ConnectivityHandler extends Handler {
        private final WeakReference<ScListFragment> fragmentRef;
        private final WeakReference<NetworkConnectivityListener> listenerRef;

        private ConnectivityHandler(ScListFragment fragment, NetworkConnectivityListener listener) {
            this.fragmentRef = new WeakReference<>(fragment);
            this.listenerRef = new WeakReference<>(listener);
        }

        @Override
        public void handleMessage(Message msg) {
            final ScListFragment fragment = fragmentRef.get();
            final NetworkConnectivityListener listener = listenerRef.get();
            switch (msg.what) {
                case CONNECTIVITY_MSG:
                    if (fragment != null && listener != null) {
                        final NetworkInfo networkInfo = listener.getNetworkInfo();
                        if (networkInfo != null) {
                            fragment.onDataConnectionUpdated(networkInfo.isConnectedOrConnecting());
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown msg.what: " + msg.what);
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
            if (changeObserver != null) {
                onContentChanged();
            }
        }
    }

}
