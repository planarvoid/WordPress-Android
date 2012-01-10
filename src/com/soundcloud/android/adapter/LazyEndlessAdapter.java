
package com.soundcloud.android.adapter;


import com.commonsware.cwac.adapter.AdapterWrapper;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.api.Request;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class LazyEndlessAdapter extends AdapterWrapper implements ScListView.OnRefreshListener, DetachableResultReceiver.Receiver {
    protected RemoteCollectionTask mAppendTask;
    protected RemoteCollectionTask mRefreshTask;

    protected ScListView mListView;
    protected ScActivity mActivity;
    protected View mPendingView = null;

    protected Content mContent;
    protected Uri mContentUri;

    protected Request mRequest;
    protected String mNextHref;
    private String mFirstPageEtag;
    protected int mPageIndex;

    private EmptyCollection mEmptyView;
    private EmptyCollection mDefaultEmptyView;
    private String mEmptyViewText = "";
    protected boolean mWaitingOnSync;


    protected int mState;
    int INITIALIZED     = 0; // no loading yet
    int READY           = 1; // ready for initial load (considered a refresh)
    int REFRESHING      = 2; // currently refreshing
    int WAITING         = 3; // idle with next href available, append on user scroll to end
    int APPENDING       = 4; // currently appending
    int DONE            = 5; // idle with no next href, no more appends
    int ERROR           = 6; // idle with error, no more appends
    private DetachableResultReceiver mDetachableReceiver;

    public LazyEndlessAdapter(ScActivity activity, LazyBaseAdapter wrapped, Uri contentUri, Request request, boolean autoAppend) {
        super(wrapped);

        mActivity = activity;
        mRequest = request;
        mContentUri = contentUri;
        mContent = Content.match(contentUri);
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

    public void setListLastUpdated() {
        if (mListView != null) {
            final long lastUpdated = LocalCollection.getLastSync(mActivity.getContentResolver(), getContentUri());
            if (lastUpdated > 0) mListView.setLastUpdated(lastUpdated);
        }
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
        final Class loadModel = getLoadModel();
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
            return Consts.ITEM_TYPE_LOADING;
        }
        return getWrappedAdapter().getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        return getWrappedAdapter().getViewTypeCount() + 1; // + 1 for loading item
    }


    public Object saveState(){
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
                saveResultReceiver()
        };
    }

    @SuppressWarnings("unchecked")
    public void restoreState(final Object[] state){
        if (state[0] != null) getData().addAll((Collection<? extends Parcelable>) state[0]);
        if (state[1] != null) restoreRefreshTask((RemoteCollectionTask) state[1]);
        if (state[2] != null) restoreAppendTask((RemoteCollectionTask) state[2]);
        if (state[3] != null) restorePagingData((int[]) state[3]);
        if (state[4] != null) restoreExtraData((String) state[4]);
        if (state[5] != null) mListView.setLastUpdated(Long.valueOf(state[5].toString()));
        if (state[6] != null) mListView.postSelect(Math.max(mState == REFRESHING ? 0 : 1, Integer.valueOf(state[6].toString())),Integer.valueOf(state[7].toString()), true);
        if (state[8] != null) {restoreResultReceiver((DetachableResultReceiver) state[8]);

        }
    }

    public long[] getTrackIds() {
        List<Long> idList = new ArrayList<Long>();
        for (Parcelable p : getData()) {
            if (p instanceof Playable) {
                idList.add(((Playable) p).getTrack().id);
             }
        }
        long[] ids = new long[idList.size()];
        for (int i=0; i<idList.size(); i++) {
            ids[i] = idList.get(i);
        }
        return ids;
    }

    /**
     * Restore a possibly still running task that could have been passed in on
     * creation
     */
    public void restoreAppendTask(RemoteCollectionTask ap) {
        if (ap != null) {
            mAppendTask = ap;
            ap.setAdapter(this);
        }
    }

    public void restoreRefreshTask(RemoteCollectionTask rt) {
        if (rt != null) {
            mRefreshTask = rt;
            rt.setAdapter(this);
        }
    }

    public RemoteCollectionTask getAppendTask() {
        return mAppendTask;
    }

    public RemoteCollectionTask getRefreshTask() {
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
        if (mState >= DONE) {
            applyEmptyView();
        }
    }

    public String saveExtraData() {
        return mNextHref;
    }

    public void restoreExtraData(String restore) {
        mNextHref = restore;
    }

    public Class<?> getLoadModel() {
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

        if (position >= Math.max(0,super.getCount() - Consts.ROW_APPEND_BUFFER) && mState == WAITING) {
            append();
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

    protected boolean canShowEmptyView(){
       return mState >= DONE && super.getCount() == 0;
    }

    protected void onEmptyRefresh(){
        if (mState < DONE) mState = DONE;
    }

    public void setRequest(Request request) {
        mRequest = request;
    }

    protected Request getRequest() {
        if (mRequest == null) return null;
        return !(mState == REFRESHING) && !TextUtils.isEmpty(mNextHref) ? new Request(mNextHref) : new Request(mRequest);
    }

    public Uri getContentUri() {
        return mContentUri;
    }

    public int getPageIndex() {
        return mState == REFRESHING ? 0 : mPageIndex;
    }

    protected void increasePageIndex() {
        mPageIndex++;
    }


    public void reset() {
        reset(true);
    }

    public void resetData(){
        getWrappedAdapter().reset();
    }

    public void reset(boolean notifyChange) {
        resetData();
        mPageIndex = 0;
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
     * @return the url
     */
    protected Request buildRequest() {
        Request request = getRequest();
        if (request != null) {
            request.add("linked_partitioning", "1");
            request.add("limit", Consts.PAGE_SIZE);
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
        return (mState == READY && getWrappedAdapter().needsItems());
    }

    public void onConnected() {
       if (mState == ERROR){
           mState = getWrappedAdapter().getCount() == 0 ? READY : WAITING;
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

    public void refresh(final boolean userRefresh){
        if (userRefresh) {
            if (getWrappedAdapter() instanceof FollowStatus.Listener) {
                FollowStatus.get().requestUserFollowings(mActivity.getApp(),
                        (FollowStatus.Listener) getWrappedAdapter(), true);
            }
        } else {
            reset();
        }
        mState = REFRESHING;
    }
    protected abstract RemoteCollectionTask buildTask();

    protected void requestSync(){
        final Intent intent = new Intent(mActivity, ApiSyncService.class);
        intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver());
        intent.setData(mContent.uri);
        intent.putExtra(ApiSyncService.EXTRA_CHECK_PERFORM_LOOKUPS,false);
        mActivity.startService(intent);
    }

    public void append() {
        mState = APPENDING;
        mAppendTask = buildTask();
        mAppendTask.execute(getCollectionParams());
    }

    protected RemoteCollectionTask.CollectionParams getCollectionParams(){
        return new RemoteCollectionTask.CollectionParams() {{
                loadModel = getLoadModel();
                contentUri = mContentUri;
                pageIndex = getPageIndex();
                request = buildRequest();
                isRefresh = isRefreshing();
                refreshPageItems = !isSyncable();
            }};
    }

    protected boolean isStale(){
        long lastsync = LocalCollection.getLastSync(mActivity.getContentResolver(), getContentUri());
        return (getPageIndex() == 0 && System.currentTimeMillis() - lastsync > Consts.DEFAULT_REFRESH_MINIMUM);
    }

    protected boolean isSyncable(){
        return mContent == null ? false : mContent.isSyncable();
    }

    protected DetachableResultReceiver getReceiver(){
        if (mDetachableReceiver == null) mDetachableReceiver = new DetachableResultReceiver(new Handler());
        mDetachableReceiver.setReceiver(this);
        return mDetachableReceiver;
    }

    public void restoreResultReceiver(DetachableResultReceiver receiver){
        mDetachableReceiver = (DetachableResultReceiver) receiver;
        mDetachableReceiver.setReceiver(this);

    }

    public DetachableResultReceiver saveResultReceiver() {
        if (mDetachableReceiver != null) mDetachableReceiver.clearReceiver();
        return mDetachableReceiver;
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_RUNNING: {
                break;
            }
            case ApiSyncService.STATUS_SYNC_FINISHED: {
                break;
            }
            case ApiSyncService.STATUS_SYNC_ERROR: {
                break;
            }
            case ApiSyncService.STATUS_REFRESH_ERROR: {
                break;
            }
            case ApiSyncService.STATUS_REFRESH_FINISHED: {
                break;
            }
        }
    }


}
