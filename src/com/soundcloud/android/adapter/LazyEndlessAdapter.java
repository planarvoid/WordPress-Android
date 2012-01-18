
package com.soundcloud.android.adapter;


import com.commonsware.cwac.adapter.AdapterWrapper;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.android.task.UpdateCollectionTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.api.Request;

import android.content.Context;
import android.net.Uri;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class LazyEndlessAdapter extends AdapterWrapper implements ScListView.OnRefreshListener, DetachableResultReceiver.Receiver {
    protected RemoteCollectionTask mAppendTask;
    protected RemoteCollectionTask mRefreshTask;
    protected UpdateCollectionTask mUpdateCollectionTask;

    protected ScListView mListView;
    protected ScActivity mActivity;
    protected View mPendingView = null;

    protected Content mContent;
    protected Uri mContentUri;

    protected Request mRequest;
    protected int mPageIndex;

    private boolean mAutoAppend;
    private EmptyCollection mEmptyView;
    private EmptyCollection mDefaultEmptyView;
    private String mEmptyViewText = "";

    protected boolean mRefreshing;

    protected boolean mKeepGoing;

    protected int mState;
    int INITIALIZED     = 0; // no loading yet
    int IDLE           = 1; // ready for initial executeAppendTask (considered a executeRefreshTask)
    int APPENDING = 3; // currently appending
    int ERROR           = 4; // idle with error, no more appends

    public LazyEndlessAdapter(ScActivity activity, LazyBaseAdapter wrapped, Uri contentUri, Request request, boolean autoAppend) {
        super(wrapped);

        mActivity = activity;
        mRequest = request;
        mContentUri = contentUri;
        mContent = Content.match(contentUri);
        mAutoAppend = autoAppend;
        wrapped.setWrapper(this);
    }

    public void onResume() {
        if (mAutoAppend && mState == INITIALIZED) {
            mState = IDLE;
            mKeepGoing = true;
        }
        notifyDataSetChanged();
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
        final Class loadModel = getLoadModel(true);
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
        } else if (Activity.class.equals(loadModel)) {
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
                getAppendTask(),
                getRefreshTask(),
                getUpdateTask(),
                savePagingData(),
                saveExtraData(),
                mListView == null ? null : mListView.getLastUpdated(),
                mListView == null ? null : mListView.getFirstVisiblePosition(),
                mListView == null ? null : mListView.getFirstVisiblePosition() == 0 && isRefreshing() ? 1 : mListView.getFirstVisiblePosition(),
        };
    }

    @SuppressWarnings("unchecked")
    public void restoreState(final Object[] state){
        if (state[0] != null) getData().addAll((Collection<? extends Parcelable>) state[0]);
        if (state[1] != null) restoreAppendTask((RemoteCollectionTask) state[1]);
        if (state[2] != null) restoreRefreshTask((RemoteCollectionTask) state[2]);
        if (state[3] != null) restoreUpdateTask((UpdateCollectionTask) state[3]);
        if (state[4] != null) restorePagingData((int[]) state[4]);
        if (state[5] != null) restoreExtraData((Object[]) state[5]);
        if (state[6] != null) mListView.setLastUpdated(Long.valueOf(state[6].toString()));
        if (state[7] != null) mListView.postSelect(Math.max(isRefreshing() ? 0 : 1, Integer.valueOf(state[7].toString())),Integer.valueOf(state[8].toString()), true);
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

    public void restoreUpdateTask(UpdateCollectionTask ut) {
        if (ut != null) {
            mUpdateCollectionTask = ut;
            ut.setAdapter(this);
        }
    }

    public RemoteCollectionTask getAppendTask() {
        return mAppendTask;
    }
    public RemoteCollectionTask getRefreshTask() {
        return mRefreshTask;
    }
    public UpdateCollectionTask getUpdateTask() {
        return mUpdateCollectionTask;
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
        ret[2] = mKeepGoing ? 1 : 0;
        return ret;
    }

    protected void restorePagingData(int[] restore) {
        mState = restore[0];
        mPageIndex = restore[1];
        mKeepGoing = restore[2] == 1;
        if (!mKeepGoing) {
            applyEmptyView();
        }
    }

    abstract public Object[] saveExtraData();

    abstract public void restoreExtraData(Object[] restore);

    public Class<?> getLoadModel(boolean refresh) {
        return getWrappedAdapter().getLoadModel();
    }

    public List<Parcelable> getData() {
        return getWrappedAdapter().getData();
    }

    @Override
    public int getCount() {
        if (canAppend() || mState == APPENDING || canShowEmptyView()) {
            return super.getCount() + 1; // extra row for an executeAppendTask row or an empty view
        } else {
            return super.getCount();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position == super.getCount() && canShowEmptyView()){
            return mListView.getEmptyView();
        }

        if (position >= Math.max(0,super.getCount() - Consts.ROW_APPEND_BUFFER) && canAppend()) {
            executeAppendTask();
        }

        if (position == super.getCount() && (canAppend() || mState == APPENDING)) {
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

    protected boolean canAppend() {
        return (mState == IDLE && mKeepGoing);
    }

    protected boolean canShowEmptyView(){
       return (mState >= IDLE && !mKeepGoing) && super.getCount() == 0;
    }

    protected void onEmptyRefresh(){
        //if (!mKeepGoing) mState = DONE;
    }

    public void setRequest(Request request) {
        mRequest = request;
    }

    abstract protected Request getRequest(boolean isRefresh);

    public Uri getContentUri() {
        return getContentUri(false);
    }

    public Uri getContentUri(boolean isRefresh) {
        return mContentUri;
    }

    public int getPageIndex(boolean isRefresh) {
        return isRefresh ? 0 : mPageIndex;
    }

    protected void increasePageIndex() {
        mPageIndex++;
    }


    public void resetData(){
        getWrappedAdapter().reset();
    }

    public void reset() {
        resetData();
        mPageIndex = 0;
        clearAppendTask();
        clearRefreshTask();
        clearUpdateTask();
        mKeepGoing = mAutoAppend;
        mState = mAutoAppend ? IDLE : INITIALIZED;
         notifyDataSetChanged();
    }

    public void cleanup() {
        mState = INITIALIZED;
        mKeepGoing = false;
        getWrappedAdapter().setData(new ArrayList<Parcelable>());
        clearAppendTask();
        notifyDataSetChanged();
    }

    protected void clearAppendTask() {
        if (mAppendTask != null && !CloudUtils.isTaskFinished(mAppendTask)) mAppendTask.cancel(true);
        mAppendTask = null;
        mPendingView = null;
    }

    protected void clearRefreshTask() {
        if (mRefreshTask != null && !CloudUtils.isTaskFinished(mRefreshTask)) mRefreshTask.cancel(true);
        mRefreshTask = null;
    }

    protected void clearUpdateTask() {
        if (mUpdateCollectionTask != null && !CloudUtils.isTaskFinished(mUpdateCollectionTask)) mUpdateCollectionTask.cancel(true);
        mUpdateCollectionTask = null;
    }

    /**
     * Get the current url for this adapter
     *
     * @return the url
     */
    protected Request buildRequest(boolean isRefresh) {
        Request request = getRequest(isRefresh);
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
        return mRefreshing;
    }

    public boolean isEmpty(){
        return false;
    }

    public void allowInitialLoading(){
        if (mState == INITIALIZED){
            mState = IDLE;
            mAutoAppend = true;
            mKeepGoing = true;
        }
    }


    public void onConnected() {
       if (mState == ERROR){
           mState = IDLE;
           notifyDataSetChanged();
       }
    }

    @Override
    public String toString() {
        return "LazyEndlessAdapter{" +
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
        mRefreshing = true;
        if (userRefresh) {
            if (getWrappedAdapter() instanceof FollowStatus.Listener) {
                FollowStatus.get().requestUserFollowings(mActivity.getApp(),
                        (FollowStatus.Listener) getWrappedAdapter(), true);
            }
        } else {
            reset();
        }
    }

    protected abstract RemoteCollectionTask buildTask();

    public void executeRefreshTask() {
        mRefreshTask = buildTask();
        mRefreshTask.execute(getCollectionParams(true));
    }

    public void executeAppendTask() {
        mState = APPENDING;
        mAppendTask = buildTask();
        mAppendTask.execute(getCollectionParams(false));
    }

    protected abstract RemoteCollectionTask.CollectionParams getCollectionParams(boolean refresh);
}
