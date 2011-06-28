
package com.soundcloud.android.adapter;


import android.content.Context;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.commonsware.cwac.adapter.AdapterWrapper;
import com.markupartist.android.widget.PullToRefreshListView;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.*;
import com.soundcloud.android.task.AppendTask;
import com.soundcloud.android.task.RefreshTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LazyEndlessAdapter extends AdapterWrapper implements PullToRefreshListView.OnRefreshListener {
    protected View mPendingView = null;
    protected int mPendingPosition = -1;
    private AppendTask mAppendTask;

    protected LazyListView mListView;
    protected int mCurrentPage;
    protected ScActivity mActivity;
    protected AtomicBoolean mKeepOnAppending = new AtomicBoolean(true);
    protected Boolean mError = false;
    private String mEmptyViewText = "";
    protected View mEmptyView;

    protected View mFooterView;
    protected boolean mNeedFooterView;

    private Request mRequest;
    private RefreshTask mRefreshTask;

    public LazyEndlessAdapter(ScActivity activity, LazyBaseAdapter wrapped, Request request) {
        super(wrapped);

        mActivity = activity;
        mCurrentPage = 0;
        mRequest = request;
        wrapped.setWrapper(this);

        LayoutInflater inflater = (LayoutInflater) mActivity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mEmptyView = inflater.inflate(R.layout.empty_list, null);
        mEmptyView.setBackgroundColor(0xFFFFFFFF);

    }


    /**
     * Create an empty view for the list this adapter will control. This is done
     * here because this adapter will control the visibility of the list
     */
    public void configureViews(final LazyListView lv) {
        mListView = lv;
        lv.setEmptyView(mEmptyView);

    }

    public boolean configureFooterView(int extra){
        if (extra > 0) {
            if (mFooterView == null) {
                mFooterView = new FrameLayout(mActivity);
                mFooterView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 0));
                mFooterView.setBackgroundColor(0xFFFFFFFF);
            }


            if (!mNeedFooterView || mFooterView.getLayoutParams().height != extra){
                mFooterView.getLayoutParams().height = extra;
                notifyDataSetChanged();
                mNeedFooterView = true;
            }

        } else {
            if (mNeedFooterView){
                notifyDataSetChanged();
                mNeedFooterView = false;
            }

        }
        return mNeedFooterView;
    }

    public void clearEmptyView() {
        if (mEmptyView != null && mEmptyView.getParent() != null) {
                ((ViewGroup) mEmptyView.getParent()).removeView(mEmptyView);
        }
        mEmptyView = null;
    }

    public void setEmptyViewText(String str) {
        mEmptyViewText = str;
    }

    /**
     * Set the current text of the adapter, based on if we are currently dealing
     * with an error
     */
    public void applyEmptyText() {
        if (!TextUtils.isEmpty(mEmptyViewText) && !mError) {
            ((TextView) mEmptyView.findViewById(R.id.empty_txt)).setText(Html.fromHtml(mEmptyViewText));
            return;
        }

        String textToSet = "";


        if (Track.class.equals(getLoadModel())) {
            textToSet = !mError ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.tracklist_error);

        } else if (User.class.equals(getLoadModel())
                || Friend.class.equals(getLoadModel())) {
            textToSet = !mError ? mActivity.getResources().getString(
                    R.string.userlist_empty) : mActivity.getResources().getString(
                    R.string.userlist_error);
        } else if (Comment.class.equals(getLoadModel())) {
            textToSet = !mError ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.commentslist_error);
        } else if (Event.class.equals(getLoadModel())) {
            textToSet = !mError ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.tracklist_error);
        }


        if (mEmptyView != null) ((TextView) mEmptyView.findViewById(R.id.empty_txt)).setText(textToSet);
    }

    /**
     * Get the wrapped adapter (casted)
     */
    @Override
    public LazyBaseAdapter getWrappedAdapter() {
        return (LazyBaseAdapter) super.getWrappedAdapter();
    }


    public Object saveState(){
        return new Object[] {
                getData(),
                getRefreshTask(),
                getAppendTask(),
                savePagingData(),
                saveExtraData()
        };
    }

    @SuppressWarnings("unchecked")
    public void restoreState(Object[] state){
        if (state[0] != null) getData().addAll((Collection<? extends Parcelable>) state[0]);
        if (state[1] != null) restoreRefreshTask((RefreshTask) state[1]);
        if (state[2] != null) restoreAppendTask((AppendTask) state[2]);
        if (state[3] != null) restorePagingData((int[]) state[3]);
        if (state[4] != null) restoreExtraData((String) state[4]);
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
        ret[1] = mCurrentPage;
        ret[2] = mError ? 1 : 0;

        return ret;

    }

    protected void restorePagingData(int[] restore) {
        mKeepOnAppending.set(restore[0] == 1);
        mCurrentPage = restore[1];
        mError = restore[2] == 1;

        if (!mKeepOnAppending.get()) {
            applyEmptyText();
        }

    }

    /**
     * Save the current extra data
     *
     * @return a string representing any extra data pertaining to this adapter
     */
    protected String saveExtraData() {
        return "";
    }

    /**
     * Restore the extra data
     *
     * @param restore : the string data to restore
     */
    protected void restoreExtraData(String restore) {
    }

    public Class<?> getLoadModel() {
        return getWrappedAdapter().getLoadModel();
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public List<Parcelable> getData() {
        return getWrappedAdapter().getData();
    }

    @Override
    public int getCount() {
        if ((mKeepOnAppending.get() && CloudUtils.isTaskFinished(mRefreshTask)) || (!mKeepOnAppending.get() && mNeedFooterView)) {
            return super.getCount() + 1;
        } else {
            return (super.getCount());
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
        if (position == super.getCount()){
            if (CloudUtils.isTaskFinished(mRefreshTask) && super.getCount() == 0){
                  return mEmptyView;
            } else if (mNeedFooterView && !mKeepOnAppending.get()){
                return mFooterView;
            }
        }

        if (position == super.getCount() && mKeepOnAppending.get() && CloudUtils.isTaskFinished(mRefreshTask)) {
            if (mPendingView == null) {

                mPendingView = getPendingView(parent);
                mPendingPosition = position;

                if (!getWrappedAdapter().isQuerying()
                        && (mAppendTask == null || CloudUtils.isTaskFinished(mAppendTask))) {

                    mAppendTask = new AppendTask(mActivity.getApp());
                    mAppendTask.loadModel = getLoadModel();
                    mAppendTask.pageSize =  getPageSize();
                    mAppendTask.setAdapter(this);

                    mAppendTask.execute(buildRequest(false));
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

    protected int getPageSize() {
        return Math.max(20,Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mActivity).getString(
                "defaultPageSize", "20")));
    }

    /**
     * A load task has just executed, set the current adapter in response
     *
     * @param keepgoing
     */
    public void onPostTaskExecute(Boolean keepgoing) {
        if (keepgoing != null) {
            mKeepOnAppending.set(keepgoing);
        } else {
            mError = true;
        }

        rebindPendingView(mPendingPosition, mPendingView);
        mPendingView = null;
        mPendingPosition = -1;

        // configure the empty view depending on possible exceptions
        applyEmptyText();
        notifyDataSetChanged();

        mActivity.handleException();

        // if (mActivityReference != null)
        // mActivityReference.handleException();
    }

    public void notifyDataSetChanged(){
        super.notifyDataSetChanged();
    }

    /**
     * Create a row for displaying a loading message by getting a row from the
     * wrapped adapter and displaying the loading views of that row
     */
    protected View getPendingView(ViewGroup parent) {
        ViewGroup row = getWrappedAdapter().createRow();
        row.findViewById(R.id.row_holder).setVisibility(View.GONE);
        row.findViewById(R.id.stub_loading).setVisibility(View.VISIBLE);

        ProgressBar list_loader = (ProgressBar) row.findViewById(R.id.list_loading);
        if (list_loader != null) list_loader.setVisibility(View.VISIBLE);

        return row;
    }

    /**
     * Turn the loading view into a real view
     */
    protected void rebindPendingView(int position, View row) {
        if (row == null)
            return;

        row.findViewById(R.id.row_holder).setVisibility(View.VISIBLE);

        if (row.findViewById(R.id.list_loading) != null)
            row.findViewById(R.id.list_loading).setVisibility(View.GONE);

        if (row.findViewById(R.id.row_loader) != null)
            row.findViewById(R.id.row_loader).setVisibility(View.GONE);

    }

    /**
     * Set the url for this adapter
     *
     * @param url : url this adapter will use to get data from
     * @param query : if this adapter is performing a search, this is the user's
     *            search query
     */
    public void setRequest(Request request) {
        mRequest = request;
    }

    /**
     * Get the current url for this adapter
     * @return the url or null
     */
    protected Request getRequest(boolean refresh) {
        return mRequest == null ? null : new Request(mRequest);
    }

    /**
     * A load task is about to be executed, do whatever we have to to get ready
     */
    public void onPreTaskExecute() {
        mError = false;
    }

    /**
     * Handle whatever the last response code was
     *
     * @param mResponseCode : the last response code
     */
    public void handleResponseCode(int mResponseCode) {
        switch (mResponseCode){
            case HttpStatus.SC_OK: // do nothing
                mError = false;
                break;
            case HttpStatus.SC_UNAUTHORIZED :
                mActivity.safeShowDialog(Consts.Dialogs.DIALOG_UNAUTHORIZED);
            default:
                mError = true;
        }
    }

    @SuppressWarnings("unchecked")
    public void refresh(boolean userRefresh) {
        if (userRefresh){
            if (FollowStatus.Listener.class.isAssignableFrom(getWrappedAdapter().getClass())) {
                FollowStatus.get().requestUserFollowings(mActivity.getApp(), (FollowStatus.Listener) getWrappedAdapter(), true);
            }
        } else {
            reset();
        }

        configureFooterView(0);

        mRefreshTask = new RefreshTask(mActivity.getApp());
        mRefreshTask.loadModel = getLoadModel();
        mRefreshTask.pageSize =  getPageSize();
        mRefreshTask.setAdapter(this);
        mRefreshTask.execute(buildRequest(true));
    }


    public void reset() {
        reset(true, true);
    }

    public void resetData(){
        getWrappedAdapter().reset();
    }

    public void reset(boolean keepAppending, boolean notifyChange) {
        resetData();

        mCurrentPage = 0;
        mKeepOnAppending.set(keepAppending);

        cancelCurrentAppendTask();

        if (mPendingView != null) {
            rebindPendingView(mPendingPosition,mPendingView);
            mPendingView = null;
        }

        mPendingPosition = -1;
        if (notifyChange) notifyDataSetChanged();

    }

    private void cancelCurrentAppendTask(){
        if (mAppendTask != null) {
            if (!CloudUtils.isTaskFinished(mAppendTask)) {
                mAppendTask.cancel(true);
            }
            mAppendTask = null;
        }
    }

    /**
     * Increment the current page
     */
    public void incrementPage() {
        mCurrentPage++;
    }

    /**
     * Get the current url for this adapter
     *
     * @return the url
     */
    protected Request buildRequest(boolean refresh) {
        Request request = getRequest(refresh);
        if (request != null) {
            request.add("limit", getPageSize());
            if (!refresh) request.add("offset", getPageSize() * getCurrentPage());
        }
        return request;
    }

    public void onPostQueryExecute() {
        rebindPendingView(mPendingPosition, mPendingView);
        mPendingView = null;
        mPendingPosition = -1;
    }

    @Override
    public void onRefresh() {
        if (!isRefreshing()) refresh(true);
    }

    public void onPostRefresh() {
        notifyDataSetChanged();
        mListView.onRefreshComplete();

        applyEmptyText();
    }

    public boolean isRefreshing() {
        if (mRefreshTask != null && !CloudUtils.isTaskFinished(mRefreshTask)){
            return true;
        }
        return false;
    }

    public boolean isEmpty(){
        return false;
    }

    public void stopAppending() {
        mKeepOnAppending.set(false);
    }

    public boolean needsRefresh() {
        return (super.getCount() == 0 && mKeepOnAppending.get()) && CloudUtils.isTaskFinished(mRefreshTask);
    }
}
