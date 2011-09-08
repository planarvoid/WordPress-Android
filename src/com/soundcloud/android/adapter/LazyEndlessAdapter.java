
package com.soundcloud.android.adapter;


import static com.soundcloud.android.SoundCloudApplication.TAG;

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
import com.soundcloud.android.task.RefreshTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.ScListView;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import android.content.Context;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LazyEndlessAdapter extends AdapterWrapper implements ScListView.OnRefreshListener {
    protected View mPendingView = null;
    private AppendTask mAppendTask;

    protected ScListView mListView;
    protected ScActivity mActivity;
    protected AtomicBoolean mKeepOnAppending = new AtomicBoolean(true);
    protected Boolean mError = false;
    private String mEmptyViewText = "";

    private Request mRequest;
    private String mNextHref;
    private RefreshTask mRefreshTask;
    private boolean mAllowInitialLoading;
    private String mFirstPageEtag;

    private static final int ITEM_TYPE_LOADING = -1;

    public LazyEndlessAdapter(ScActivity activity, LazyBaseAdapter wrapped, Request request) {
        this(activity,wrapped,request,true);
    }

    public LazyEndlessAdapter(ScActivity activity, LazyBaseAdapter wrapped, Request request, boolean autoAppend) {
        super(wrapped);

        mActivity = activity;
        mRequest = request;
        wrapped.setWrapper(this);

        mAllowInitialLoading = autoAppend;
        mKeepOnAppending.set(autoAppend);
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

    /**
     * Set the current text of the adapter, based on if we are currently dealing
     * with an error
     */
    public void applyEmptyText() {
        if (mListView != null) {
            if (!TextUtils.isEmpty(mEmptyViewText) && !mError) {
                mListView.setEmptyText(Html.fromHtml(mEmptyViewText));
            } else {
                mListView.setEmptyText(getEmptyText());
            }
        }

    }

    private String getEmptyText(){
        final Class loadModel = getLoadModel(false);
        if (Track.class.equals(loadModel)) {
            return !mError ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.tracklist_error);
        } else if (User.class.equals(loadModel)
                || Friend.class.equals(loadModel)) {
            return !mError ? mActivity.getResources().getString(
                    R.string.userlist_empty) : mActivity.getResources().getString(
                    R.string.userlist_error);
        } else if (Comment.class.equals(loadModel)) {
            return !mError ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.commentslist_error);
        } else if (Event.class.equals(loadModel)) {
            return !mError ? mActivity.getResources().getString(
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
        return new Object[] {
                getData(),
                getRefreshTask(),
                getAppendTask(),
                savePagingData(),
                saveExtraData(),
                mListView == null ? null : mListView.getLastUpdated(),
                mListView == null ? null : mListView.getFirstVisiblePosition() == 0 ? 1 : mListView.getFirstVisiblePosition(),
                mListView == null ? null : mListView.getChildAt(0) == null ||
                        mListView.getFirstVisiblePosition() == 0 ? 0 : mListView.getChildAt(0).getTop()
        };
    }

    @SuppressWarnings("unchecked")
    public void restoreState(final Object[] state){
        if (state[0] != null) getData().addAll((Collection<? extends Parcelable>) state[0]);
        if (state[1] != null) restoreRefreshTask((RefreshTask) state[1]);
        if (state[2] != null) restoreAppendTask((AppendTask) state[2]);
        if (state[3] != null) restorePagingData((int[]) state[3]);
        if (state[4] != null) restoreExtraData((String) state[4]);
        if (state[5] != null) mListView.setLastUpdated(Long.valueOf(state[5].toString()));
        if (state[6] != null) mListView.postSelect(Integer.valueOf(state[6].toString()),Integer.valueOf(state[7].toString()), true);
    }


    /**
     * Restore a possibly still running task that could have been passed in on
     * creation
     */
    public void restoreAppendTask(AppendTask ap) {
        if (ap != null) {
            mAppendTask = ap;
            ap.setAdapter(this);
        }
    }

    public void restoreRefreshTask(RefreshTask rt) {
        if (rt != null) {
            mRefreshTask = rt;
            rt.setAdapter(this);
        }
    }

    public AppendTask getAppendTask() {
        return mAppendTask;
    }

    public RefreshTask getRefreshTask() {
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
        ret[0] = (mKeepOnAppending.get()) ? 1 : 0;
        ret[1] = mError ? 1 : 0;

        return ret;

    }

    protected void restorePagingData(int[] restore) {
        mKeepOnAppending.set(restore[0] == 1);
        mError = restore[1] == 1;

        if (!mKeepOnAppending.get()) {
            applyEmptyText();
        }

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
        if (mAllowInitialLoading && mRefreshTask == null && (mKeepOnAppending.get() || getWrappedAdapter().getCount() == 0)) {
            return super.getCount() + 1;
        } else {
            return super.getCount();
        }
    }

    /**
     * Get a View that displays the data at the specified position in the data
     * set. In this case, if we are at the end of the list and we are still in
     * append mode, we ask for a pending view and return it, plus kick off the
     * background task to append more data to the wrapped adapter.
     *
     * @param position Position of the item whose data we want
     * @param convertView View to recycle, if not null
     * @param parent ViewGroup containing the returned View
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position == super.getCount() && canShowEmptyView()){
            return mListView.getEmptyView();
        }

        if (position == super.getCount() && mKeepOnAppending.get() && CloudUtils.isTaskFinished(mRefreshTask)) {
            if (mPendingView == null) {
                if (convertView == null){
                    mPendingView = ((LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.list_loading_item,null,false);
                    if (CloudUtils.isTaskFinished(mAppendTask)) {
                        mAppendTask = new AppendTask(mActivity.getApp());
                        mAppendTask.loadModel = getLoadModel(false);
                        mAppendTask.pageSize =  getPageSize();
                        mAppendTask.setAdapter(this);
                        mAppendTask.execute(buildRequest(false));
                    }
                } else {
                    mPendingView = convertView;
                }
            }
            return mPendingView;
        } else if (convertView == mPendingView) {
            // if we're not at the bottom, and we're getting the
            // pendingView back for recycling, skip the recycle
            // process
            return (super.getView(position, null, parent));
        }

        return (super.getView(position, convertView, parent));
    }

    protected boolean canShowEmptyView(){
       return mRefreshTask == null && super.getCount() == 0 && !mKeepOnAppending.get();
    }

    protected int getPageSize() {
        return Math.max(20,Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mActivity).getString(
                "defaultPageSize", "20")));
    }

    protected boolean handleResponseCode(int responseCode) {
        switch (responseCode) {
            case HttpStatus.SC_OK: // do nothing
            case HttpStatus.SC_NOT_MODIFIED:
                mError = false;
                break;
            case HttpStatus.SC_UNAUTHORIZED:
                mActivity.safeShowDialog(Consts.Dialogs.DIALOG_UNAUTHORIZED);
            default:
                Log.w(TAG, "unexpected responseCode "+responseCode);

                mError = true;
                mKeepOnAppending.set(false);
                break;
        }
        return !mError;
    }

    public void onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing) {
        if (responseCode == HttpStatus.SC_OK){
            mKeepOnAppending.set(keepGoing);
            mNextHref = nextHref;
        } else {
            handleResponseCode(responseCode);
        }

        if (newItems != null && newItems.size() > 0) {
            for (Parcelable newitem : newItems) {
                getData().add(newitem);
            }
        }
        mPendingView = null;
        // configure the empty view depending on possible exceptions
        applyEmptyText();
        notifyDataSetChanged();
    }

    public void onPostRefresh(List<Parcelable> newItems, String nextHref, int responseCode, Boolean keepGoing, String eTag) {
        if (handleResponseCode(responseCode)) {
            if (newItems != null && newItems.size() > 0) {
                setNewEtag(eTag);
                reset(true, false);
                onPostTaskExecute(newItems, nextHref, responseCode, keepGoing);
            } else if (eTag != null){
                onEmptyRefresh();
            }
        }

        applyEmptyText();
        mRefreshTask = null;
        notifyDataSetChanged();

        if (mListView != null) {
            mListView.onRefreshComplete(responseCode == HttpStatus.SC_OK);
        }
    }

    protected void setNewEtag(String eTag){
        mFirstPageEtag = eTag;
    }

    protected String getCurrentEtag(){
        return mFirstPageEtag;
    }

    protected void onEmptyRefresh(){
      mKeepOnAppending.set(false);
    }

    public void setRequest(Request request) {
        mRequest = request;
    }

    protected Request getRequest(boolean refresh) {
        if (mRequest == null) return null;
        return (!refresh && !TextUtils.isEmpty(mNextHref)) ? new Request(mNextHref) : new Request(mRequest);
    }

    /**
     * A load task is about to be executed, do whatever we have to to get ready
     */
    public void onPreTaskExecute() {
        mError = false;
    }

    @SuppressWarnings("unchecked")
    public void refresh(boolean userRefresh) {
        if (userRefresh) {
            if (FollowStatus.Listener.class.isAssignableFrom(getWrappedAdapter().getClass())) {
                FollowStatus.get().requestUserFollowings(mActivity.getApp(), (FollowStatus.Listener) getWrappedAdapter(), true);
            }
        } else {
            reset();
        }

        mRefreshTask = new RefreshTask(mActivity.getApp()) {
            {
                loadModel = getLoadModel(false);
                pageSize  = getPageSize();
                setAdapter(LazyEndlessAdapter.this);
                execute(buildRequest(true));
            }
        };
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
        mKeepOnAppending.set(keepAppending);
        clearAppendTask();
        clearRefreshTask();
         if (notifyChange) notifyDataSetChanged();
    }

    public void cleanup() {
        mKeepOnAppending.set(false);
        getWrappedAdapter().setData(new ArrayList<Parcelable>());
        clearAppendTask();
        notifyDataSetChanged();
    }

    private void clearAppendTask() {
        if (mAppendTask != null && !CloudUtils.isTaskFinished(mAppendTask)) mAppendTask.cancel(true);
        mAppendTask = null;
        mPendingView = null;
    }

    private void clearRefreshTask() {
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
            request.add("limit", getPageSize());
            if (refresh && getCurrentEtag() != null) request.ifNoneMatch(getCurrentEtag());
        }
        return request;
    }

    public void onPostQueryExecute() {
        mPendingView = null;
    }

    @Override
    public void onRefresh() {
        if (!isRefreshing()) refresh(true);
    }

    public boolean isRefreshing() {
        return mRefreshTask != null && !CloudUtils.isTaskFinished(mRefreshTask);
    }

    public boolean isEmpty(){
        return false;
    }

    public void allowInitialLoading(){
        mAllowInitialLoading = true;
        if (mRefreshTask == null && !mKeepOnAppending.get()){
            mKeepOnAppending.set(true);
        }
    }

    public boolean needsRefresh() {
        return (getWrappedAdapter().getCount() == 0 && mKeepOnAppending.get()) && CloudUtils.isTaskFinished(mRefreshTask);
    }

    public void onConnected() {
       if (mError && !mKeepOnAppending.get()){
           mKeepOnAppending.set(true);
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
                ", mKeepOnAppending=" + mKeepOnAppending +
                ", mError=" + mError +
                ", mAppendTask=" + mAppendTask +
                ", mPendingView=" + mPendingView +
                ", mActivity=" + mActivity +
                ", mListView=" + mListView +
                '}';
    }

    public boolean isAllowingLoading() {
        return mAllowInitialLoading;
    }
}
