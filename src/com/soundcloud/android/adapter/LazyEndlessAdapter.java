
package com.soundcloud.android.adapter;


import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.net.Uri;
import com.commonsware.cwac.adapter.AdapterWrapper;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.AppendTask;
import com.soundcloud.android.task.AsyncApiTask;
import com.soundcloud.android.task.LoadCollectionTask;
import com.soundcloud.android.task.RefreshTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.FriendFinderView;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LazyEndlessAdapter extends AdapterWrapper implements ScListView.OnRefreshListener, DetachableResultReceiver.Receiver {
    public static final int ROW_APPEND_BUFFER = 3;

    protected LoadCollectionTask mAppendTask;
    protected LoadCollectionTask mRefreshTask;

    protected ScListView mListView;
    protected ScActivity mActivity;
    protected View mPendingView = null;
    protected DetachableResultReceiver mDetachableReceiver;

    protected Uri mContentUri;
    protected Request mRequest;
    protected String mNextHref;
    private String mFirstPageEtag;
    private int mPageIndex;

    private static final int ITEM_TYPE_LOADING = -1;
    private EmptyCollection mEmptyView;
    private EmptyCollection mDefaultEmptyView;
    private String mEmptyViewText = "";

    private static final int PAGE_SIZE = 50;

    protected int mState;
    int INITIALIZED     = 0; // no loading yet
    int READY           = 1; // ready for initial load (considered a refresh)
    int REFRESHING      = 2; // currently refreshing
    int WAITING         = 3; // idle with next href available, append on user scroll to end
    int APPENDING       = 4; // currently appending
    int DONE            = 5; // idle with no next href, no more appends
    int ERROR           = 6; // idle with error, no more appends



    public LazyEndlessAdapter(ScActivity activity, LazyBaseAdapter wrapped, Request request, Uri contentUri) {
        this(activity,wrapped,request,contentUri, true);
    }

    public LazyEndlessAdapter(ScActivity activity, LazyBaseAdapter wrapped, Request request, Uri contentUri, boolean autoAppend) {
        super(wrapped);

        mActivity = activity;

        mRequest = request;
        mContentUri = contentUri;
        wrapped.setWrapper(this);

        if (autoAppend) mState = READY;
    }

    /**
     * Create an empty view for the list this adapter will control. This is done
     * here because this adapter will control the visibility of the list
     */
    public void configureViews(final ScListView lv) {
        mListView = lv;
    }

    public void setEmptyViewText(String str) {
        mEmptyViewText = str;
    }

    public void setEmptyView(EmptyCollection emptyView) {
        mEmptyView = emptyView;
    }

    /**
     * Set the current text of the adapter, based on if we are currently dealing
     * with an error
     */
    public void applyEmptyView() {
        final boolean error = mState == ERROR;
        if (mListView != null) {
            if (mEmptyView != null && !error){
                mListView.setEmptyView(mEmptyView);
            } else {
                if (mDefaultEmptyView == null){
                    mDefaultEmptyView = new EmptyCollection(mActivity);
                }
                mDefaultEmptyView.setImage(error ? R.drawable.empty_connection : R.drawable.empty_collection);
                mDefaultEmptyView.setMessageText((!error && !TextUtils.isEmpty(mEmptyViewText)) ? mEmptyViewText : getEmptyText());
                mListView.setEmptyView(mDefaultEmptyView);
            }
        }

    }

    private String getEmptyText(){
        final Class loadModel = getLoadModel(false);
        final boolean error = mState == ERROR;
        if (Track.class.equals(loadModel)) {
            return !error ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.tracklist_error);
        } else if (User.class.equals(loadModel)
                || Friend.class.equals(loadModel)) {
            return !error ? mActivity.getResources().getString(
                    R.string.userlist_empty) : mActivity.getResources().getString(
                    R.string.userlist_error);
        } else if (Comment.class.equals(loadModel)) {
            return !error ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.commentslist_error);
        } else if (Event.class.equals(loadModel)) {
            return !error ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.tracklist_error);
        } else {
            return "";
        }
    }

    /**
     * Get the wrapped adapter (casted)
     */
    @Override
    public LazyBaseAdapter getWrappedAdapter() {
        return (LazyBaseAdapter) super.getWrappedAdapter();
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= getWrappedAdapter().getCount()){
            return ITEM_TYPE_LOADING;
        }
        return getWrappedAdapter().getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        return getWrappedAdapter().getViewTypeCount() + 1; // + 1 for loading item
    }


    public Object saveState(){
        if (mDetachableReceiver != null) mDetachableReceiver.clearReceiver();
        return new Object[] {
                getData(),
                getRefreshTask(),
                getAppendTask(),
                savePagingData(),
                saveExtraData(),
                mListView == null ? null : mListView.getLastUpdated(),
                mListView == null ? null : mListView.getFirstVisiblePosition() == 0 && mState != REFRESHING ? 1 : mListView.getFirstVisiblePosition(),
                mListView == null ? null : mListView.getChildAt(0) == null ||
                        mListView.getFirstVisiblePosition() == 0 ? 0 : mListView.getChildAt(0).getTop(),
                mDetachableReceiver
        };
    }

    @SuppressWarnings("unchecked")
    public void restoreState(final Object[] state){
        if (state[0] != null) getData().addAll((Collection<? extends Parcelable>) state[0]);
        if (state[1] != null) restoreRefreshTask((LoadCollectionTask) state[1]);
        if (state[2] != null) restoreAppendTask((LoadCollectionTask) state[2]);
        if (state[3] != null) restorePagingData((int[]) state[3]);
        if (state[4] != null) restoreExtraData((String) state[4]);
        if (state[5] != null) mListView.setLastUpdated(Long.valueOf(state[5].toString()));
        if (state[6] != null) mListView.postSelect(Integer.valueOf(state[6].toString()),Integer.valueOf(state[7].toString()), true);
        if (state[8] != null) {
            mDetachableReceiver = (DetachableResultReceiver) state[8];
            mDetachableReceiver.setReceiver(this);
        }
    }

    private static class State {
        public DetachableResultReceiver mReceiver;
        public Uri mNowPlayingUri = null;
        public boolean mNowPlayingGotoWifi = false;
        public boolean mSyncing = false;
        public boolean mNoResults = false;

        private State() {
            mReceiver = new DetachableResultReceiver(new Handler());
        }
    }


    /**
     * Restore a possibly still running task that could have been passed in on
     * creation
     */
    public void restoreAppendTask(LoadCollectionTask ap) {
        if (ap != null) {
            mAppendTask = ap;
            ap.setAdapter(this);
        }
    }

    public void restoreRefreshTask(LoadCollectionTask rt) {
        if (rt != null) {
            mRefreshTask = rt;
            rt.setAdapter(this);
        }
    }

    public LoadCollectionTask getAppendTask() {
        return mAppendTask;
    }

    public LoadCollectionTask getRefreshTask() {
        return mRefreshTask;
    }

    /**
     * Save the current paging data
     *
     * @return an integer list {whether to keep retrieving data, the current
     *         page the adapter is on}
     */
    protected int[] savePagingData() {
        int[] ret = new int[3];
        ret[0] = mState;
        ret[1] = mPageIndex;
        return ret;
    }

    protected void restorePagingData(int[] restore) {
        mState = restore[0];
        mPageIndex = restore[1];
        if (mState > DONE) applyEmptyView();
    }

    public String saveExtraData() {
        return mNextHref;
    }

    public void restoreExtraData(String restore) {
        mNextHref = restore;
    }

    public Class<?> getLoadModel(boolean isRefresh) {
        return getWrappedAdapter().getLoadModel();
    }

    public List<Parcelable> getData() {
        return getWrappedAdapter().getData();
    }

    @Override
    public int getCount() {
        if (mState == WAITING || mState == APPENDING || (mState >= DONE && getWrappedAdapter().getCount() == 0)) {
            return super.getCount() + 1; // extra row for an append row or an empty view
        } else {
            return super.getCount();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position == super.getCount() && canShowEmptyView()){
            return mListView.getEmptyView();
        }

        if (position >= Math.max(0,super.getCount() - ROW_APPEND_BUFFER) && mState == WAITING) {
            startAppendTask();
        }

        if (position == super.getCount() && (mState == WAITING || mState == APPENDING)) {
            if (mPendingView == null) {
                mPendingView = (convertView != null) ? convertView :
                            ((LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                    .inflate(R.layout.list_loading_item,null,false);
            }
            return mPendingView;

        } else if (convertView == mPendingView) {
            return (super.getView(position, null, parent));
        }

        return (super.getView(position, convertView, parent));
    }

    protected void startAppendTask(){
        mState = APPENDING;
        mAppendTask = new AppendTask(mActivity.getApp()) {
            {
                loadModel = getLoadModel(false);
                pageSize = PAGE_SIZE;
                contentUri = mContentUri;
                pageIndex = mPageIndex;
                setAdapter(LazyEndlessAdapter.this);
                request = buildRequest(false);
                refresh = false;
                execute();
            }
        };
    }

    protected void startRefreshTask(final boolean userRefresh){
        mState = REFRESHING;
       mRefreshTask = new RefreshTask(mActivity.getApp()) {
            {
                loadModel = getLoadModel(false);
                pageSize  = PAGE_SIZE;
                contentUri = mContentUri;
                pageIndex = mPageIndex;
                setAdapter(LazyEndlessAdapter.this);
                request = buildRequest(true);
                refresh = userRefresh;
                execute();
            }
        };
    }

    protected boolean canShowEmptyView(){
       return mState >= DONE && super.getCount() == 0;
    }

    protected boolean handleResponseCode(int responseCode) {
        switch (responseCode) {
            case HttpStatus.SC_OK: // do nothing
            case HttpStatus.SC_NOT_MODIFIED:
                return true;

            case HttpStatus.SC_UNAUTHORIZED:
                mActivity.safeShowDialog(Consts.Dialogs.DIALOG_UNAUTHORIZED);
            default:
                Log.w(TAG, "unexpected responseCode "+responseCode);
                mState = ERROR;
            return false;
        }
    }

    public void onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing) {
        if ((newItems != null && newItems.size() > 0) || responseCode == HttpStatus.SC_OK){
            mState = keepGoing ? WAITING : DONE;
            mNextHref = nextHref;
            mPageIndex++;
        } else {
            handleResponseCode(responseCode);
        }

        if (newItems != null && newItems.size() > 0) {
            for (Parcelable newitem : newItems) {
                getWrappedAdapter().addItem(newitem);
            }
        }

        // configure the empty view depending on possible exceptions
        applyEmptyView();
        mPendingView = null;
        mRefreshTask = null;
        mAppendTask = null;
        notifyDataSetChanged();
    }

    public void onPostRefresh(List<Parcelable> newItems, String nextHref, int responseCode, Boolean keepGoing) {
        if (handleResponseCode(responseCode) || (newItems != null && newItems.size() > 0)) {
            reset(true, false);
            onPostTaskExecute(newItems, nextHref, responseCode, keepGoing);
            //} else if (eTag != null){
        } else {
            onEmptyRefresh();
        }

        applyEmptyView();
        notifyDataSetChanged();

        if (mListView != null) {
            mListView.onRefreshComplete(responseCode == HttpStatus.SC_OK);
        }
    }

    protected void onEmptyRefresh(){
        mState = DONE;
    }

    public void setRequest(Request request) {
        mRequest = request;
    }

    protected Request getRequest(boolean refresh) {
        if (mRequest == null) return null;
        return (!refresh && !TextUtils.isEmpty(mNextHref)) ? new Request(mNextHref) : new Request(mRequest);
    }

    @SuppressWarnings("unchecked")
    public void refresh(final boolean userRefresh) {
        if (userRefresh) {
            if (FollowStatus.Listener.class.isAssignableFrom(getWrappedAdapter().getClass())) {
                FollowStatus.get().requestUserFollowings(mActivity.getApp(), (FollowStatus.Listener) getWrappedAdapter(), true);
            }
        } else {
            reset();
        }

        startRefreshTask(userRefresh);
        notifyDataSetChanged();
    }

    public void reset() {
        reset(true, true);
    }

    public void resetData(){
        getWrappedAdapter().reset();
    }

    public void reset(boolean keepAppending, boolean notifyChange) {
        resetData();
        mNextHref = "";
        mState = READY;
        clearAppendTask();
//        clearRefreshTask();
         if (notifyChange) notifyDataSetChanged();
    }

    public void cleanup() {
        mState = DONE;
        getWrappedAdapter().setData(new ArrayList<Parcelable>());
        clearAppendTask();
        notifyDataSetChanged();
    }

    private void clearAppendTask() {
        if (mAppendTask != null && !CloudUtils.isTaskFinished(mAppendTask)) mAppendTask.cancel(true);
        mAppendTask = null;
        mPendingView = null;
    }

    public void clearRefreshTask() {
        if (mRefreshTask != null && !CloudUtils.isTaskFinished(mRefreshTask)) mRefreshTask.cancel(true);
        mRefreshTask = null;
    }

    /**
     * Get the current url for this adapter
     *
     * @param refresh
     * @return the url
     */
    protected Request buildRequest(boolean refresh) {
        Request request = getRequest(refresh);
        if (request != null) {
            request.add("linked_partitioning", "1");
            request.add("limit", PAGE_SIZE);
        }
        return request;
    }

    public void onPostQueryExecute() {
        mPendingView = null;
    }

    @Override
    public void onRefresh(boolean manual) {
        if (!isRefreshing()) refresh(manual);
    }

    public boolean isRefreshing() {
        return mState == REFRESHING;
    }

    public boolean isEmpty(){
        return false;
    }

    public void allowInitialLoading(){
        if (mState == INITIALIZED){
            mState = READY;
        }
    }

    public boolean needsRefresh() {
        return (mState == READY && getWrappedAdapter().getCount() == 0);
    }

    public void onConnected() {
       if (mState == ERROR){
           mState = WAITING;
           notifyDataSetChanged();
       }
    }

    @Override
    public String toString() {
        return "LazyEndlessAdapter{" +
                "mRefreshTask=" + mRefreshTask +
                ", mNextHref='" + mNextHref + '\'' +
                ", mRequest=" + mRequest +
                ", mEmptyViewText='" + mEmptyViewText + '\'' +
                ", mState=" + mState +
                ", mAppendTask=" + mAppendTask +
                ", mPendingView=" + mPendingView +
                ", mActivity=" + mActivity +
                ", mListView=" + mListView +
                '}';
    }

    public boolean isAllowingLoading() {
        return mState != INITIALIZED;
    }

    protected DetachableResultReceiver getReceiver(){
        if (mDetachableReceiver == null) mDetachableReceiver = new DetachableResultReceiver(new Handler());
        mDetachableReceiver.setReceiver(this);
        return mDetachableReceiver;
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {

    }
}
