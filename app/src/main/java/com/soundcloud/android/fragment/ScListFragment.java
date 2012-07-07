package com.soundcloud.android.fragment;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.actionbarsherlock.app.SherlockListFragment;
import com.commonsware.cwac.endless.EndlessAdapter;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Refreshable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.CollectionLoader;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.android.task.UpdateCollectionTask;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScListFragment extends SherlockListFragment
        implements PullToRefreshBase.OnRefreshListener, DetachableResultReceiver.Receiver, LocalCollection.OnChangeListener, RemoteCollectionTask.Callback {

    private CollectionLoader mItemLoader;

    private ScListView mListView;
    private ScEndlessAdapter mEndlessAdapter;
    private ScBaseAdapter mBaseAdapter;
    private EmptyCollection mEmptyCollection;
    private EmptyCollection mDefaultEmptyCollection;
    private String mEmptyCollectionText;

    private DetachableResultReceiver mDetachableReceiver;
    private Content mContent;
    private Uri mContentUri;
    private boolean mIsConnected;
    private NetworkConnectivityListener connectivityListener;

    private RemoteCollectionTask mRefreshTask;
    private UpdateCollectionTask mUpdateCollectionTask;

    private Boolean mIsSyncable;
    protected LocalCollection mLocalCollection;
    private ChangeObserver mChangeObserver;
    private boolean mContentInvalid, mObservingContent;

    protected String mNextHref;


    protected static final int CONNECTIVITY_MSG = 0;
    ;
    protected boolean mAppenwdable = true;
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
        if (mBaseAdapter == null){
            mBaseAdapter = new ScBaseAdapter(getActivity().getApplicationContext(), mContent);
            mEndlessAdapter = new ScEndlessAdapter(getActivity(), mBaseAdapter, R.layout.list_loading_item);
            setListAdapter(mEndlessAdapter);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();

        FrameLayout root = new FrameLayout(context);

        mListView = buildList();
        mListView.setOnRefreshListener(this);

        mEmptyCollection = EmptyCollection.fromContent(context, mContent);
        mEmptyCollection.setHasSyncedBefore(mLocalCollection.hasSyncedBefore());
        mListView.setEmptyView(mEmptyCollection);

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
        lv.getRefreshableView().setDivider(getResources().getDrawable(R.drawable.list_separator));
        lv.getRefreshableView().setDividerHeight(1);
        lv.getRefreshableView().setCacheColorHint(Color.TRANSPARENT);
        return lv;
    }

    public ScBaseAdapter getBaseAdapter() {
        return mBaseAdapter;
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


    protected void onDataConnectionUpdated(boolean isConnected) {
        mIsConnected = isConnected;
        if (isConnected && !isConnected) {
            if (getBaseAdapter().needsItems() && getScActivity().getApp().getAccount() != null) {

            }
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


    protected RemoteCollectionTask buildTask() {
        return new RemoteCollectionTask(SoundCloudApplication.fromContext(getActivity()), this);
    }


    protected RemoteCollectionTask.CollectionParams getTaskParams(final boolean refresh) {
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
            executeRefreshTask();
            getBaseAdapter().notifyDataSetChanged();
        }
    }

    public void reset() {
        mPageIndex = 0;
        getBaseAdapter().clearData();
        setListAdapter(getBaseAdapter());
        clearRefreshTask();
        clearUpdateTask();
        getBaseAdapter().notifyDataSetChanged();
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
        mEmptyCollection.setHasSyncedBefore(mLocalCollection.hasSyncedBefore());
    }

    protected void addNewItems(List<Parcelable> newItems) {
        if (newItems == null || newItems.size() == 0) return;
        for (Parcelable newItem : newItems) {
            getBaseAdapter().addItem(newItem);
        }
        checkForStaleItems(newItems);
    }

    protected void checkForStaleItems(List<? extends Parcelable> newItems) {
        if (!(IOUtils.isWifiConnected(getActivity())) || newItems == null || newItems.size() == 0 || !(newItems.get(0) instanceof Refreshable))
            return;

        Map<Long, ScModel> toUpdate = new HashMap<Long, ScModel>();
        for (Parcelable newItem : newItems) {
            if (newItem instanceof Refreshable) {
                ScModel resource = ((Refreshable) newItem).getRefreshableResource();
                if (resource != null) {
                    if (((Refreshable) newItem).isStale()) {
                        toUpdate.put(resource.id, resource);
                    }
                }
            }
        }

        if (toUpdate.size() > 0) {
            mUpdateCollectionTask = new UpdateCollectionTask(SoundCloudApplication.fromContext(getActivity()), mContent.resourceType);
            mUpdateCollectionTask.setAdapter(getBaseAdapter());
            mUpdateCollectionTask.execute(toUpdate);
        }

    }

    protected boolean handleResponseCode(int responseCode) {
        switch (responseCode) {
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
    public void onPostTaskExecute(RemoteCollectionTask.ReturnData data) {
        if (data.success){
            if (data.wasRefresh){
                reset();
                if (mListView != null && mContentUri != null) setListLastUpdated();
            }

            mNextHref = data.nextHref;
            addNewItems(data.newItems);
            mPageIndex++;
        } else {
            handleResponseCode(data.responseCode);
        }

        // reset refresh header
        if (data.wasRefresh && (getBaseAdapter().getData().size() > 0 || !isRefreshing())) {
            doneRefreshing();
        }
        getBaseAdapter().notifyDataSetChanged();
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

    class ScEndlessAdapter extends EndlessAdapter {

        RemoteCollectionTask.ReturnData lastReturn;

        public ScEndlessAdapter(Context context, ListAdapter wrapped, int pendingResource) {
            super(context, wrapped, pendingResource);
        }

        @Override
        protected boolean cacheInBackground() throws Exception {
            Thread.sleep(3000);
            if (this != mEndlessAdapter) return false;
            lastReturn = new RemoteCollectionTask(SoundCloudApplication.fromContext(getContext()),null).execute(getTaskParams(false)).get();
            return lastReturn.keepGoing;
        }

        @Override
        protected void appendCachedData() {
            if (this != mEndlessAdapter) return;
            onPostTaskExecute(lastReturn);
        }
    }
    /*
    private ScListView.LazyListListener mLazyListListener = new ScListView.LazyListListener() {
            @Override
            public void onEventClick(EventsAdapterWrapper wrapper, int position) {
                final Activity e = (Activity) wrapper.getItem(position);
                if (e.type == Activity.Type.FAVORITING) {
                    SoundCloudApplication.TRACK_CACHE.put(e.getTrack(), false);
                    startActivity(new Intent(ScListActivity.this, TrackFavoriters.class)
                        .putExtra("track_id", e.getTrack().id));
                } else {
                    playTrack(wrapper.getPlayInfo(position));
                }
            }

            @Override
            public void onTrackClick(LazyEndlessAdapter wrapper, int position) {
                playTrack(wrapper.getPlayInfo(position));
            }

            @Override
            public void onUserClick(User user) {
                Intent i = new Intent(ScListActivity.this, UserBrowser.class);
                i.putExtra("user", user);
                startActivity(i);
            }

            @Override
            public void onCommentClick(Comment comment) {
                Intent i = new Intent(ScListActivity.this, UserBrowser.class);
                i.putExtra("user", comment.user);
                startActivity(i);
            }

            @Override
            public void onRecordingClick(final Recording recording) {
                handleRecordingClick(recording);
            }
        }; */

}