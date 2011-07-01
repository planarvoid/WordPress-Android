
package com.soundcloud.android.adapter;


import android.content.Context;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
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

import java.util.ArrayList;
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

        if (activity != null) {
          LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);


            mEmptyView = inflater.inflate(R.layout.empty_list, null);
            mEmptyView.setBackgroundColor(0xFFFFFFFF);
        }
    }


    /**
     * Create an empty view for the list this adapter will control. This is done
     * here because this adapter will control the visibility of the list
     */
    public void configureViews(final LazyListView lv) {
        mListView = lv;
        if (lv != null) lv.setEmptyView(mEmptyView);

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
        if (mEmptyView != null) {
            if (!TextUtils.isEmpty(mEmptyViewText) && !mError) {
                ((TextView) mEmptyView.findViewById(R.id.empty_txt)).setText(Html.fromHtml(mEmptyViewText));
            } else {
                // generic model based empty text
                ((TextView) mEmptyView.findViewById(R.id.empty_txt)).setText(getEmptyText());
            }
        }

    }

    private String getEmptyText(){
          if (Track.class.equals(getLoadModel(false))) {
            return !mError ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.tracklist_error);

        } else if (User.class.equals(getLoadModel(false))
                || Friend.class.equals(getLoadModel(false))) {
            return !mError ? mActivity.getResources().getString(
                    R.string.userlist_empty) : mActivity.getResources().getString(
                    R.string.userlist_error);
        } else if (Comment.class.equals(getLoadModel(false))) {
            return !mError ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.commentslist_error);
        } else if (Event.class.equals(getLoadModel(false))) {
            return !mError ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.tracklist_error);
        }
        return "";
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
                saveExtraData(),
                mListView == null ? null : mListView.getLastUpdated(),
                mListView == null ? null : mListView.getFirstVisiblePosition() == 0 ? 1 : mListView.getFirstVisiblePosition(),
                mListView == null ? null : mListView.getChildAt(0) == null ? 0 : mListView.getChildAt(0).getTop()
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
        if (state[6] != null) mListView.post(new Runnable() {
            @Override
            public void run() {
                mListView.setSelectionFromTop(Integer.valueOf(state[6].toString()),Integer.valueOf(state[7].toString()));
            }
        });
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

    public String saveExtraData() {
        return getWrappedAdapter().nextCursor;
    }

    public void restoreExtraData(String restore) {
        getWrappedAdapter().nextCursor = restore;
    }

    public Class<?> getLoadModel(boolean isRefresh) {
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
                    mAppendTask.loadModel = getLoadModel(false);
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

    protected void handleResponseCode(int responseCode) {
        switch (responseCode) {
            case HttpStatus.SC_OK: // do nothing
                mError = false;
                break;

            case HttpStatus.SC_UNAUTHORIZED:
                mActivity.safeShowDialog(Consts.Dialogs.DIALOG_UNAUTHORIZED);
            default:
                mError = true;
                mKeepOnAppending.set(false);
                if (getWrappedAdapter().getCount() != 0) Toast.makeText(mActivity, getEmptyText(), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * A load task has just executed, set the current adapter in response
     *
     * @param newItems
     * @param nextHref
     * @param responseCode
     * @param keepgoing
     */
    public void onPostTaskExecute(ArrayList<Parcelable> newItems, String nextHref, int responseCode, Boolean keepgoing) {

        if (responseCode == HttpStatus.SC_OK){
            mKeepOnAppending.set(keepgoing);
            incrementPage();
        } else {
            handleResponseCode(responseCode);
        }

        if (newItems != null && newItems.size() > 0) {
            for (Parcelable newitem : newItems) {
                getData().add(newitem);
            }
        }

        if (!TextUtils.isEmpty(nextHref)) {
            getWrappedAdapter().onNextHref(nextHref);
        }

        rebindPendingView(mPendingPosition, mPendingView);
        mPendingView = null;
        mPendingPosition = -1;

        // configure the empty view depending on possible exceptions
        applyEmptyText();
        notifyDataSetChanged();
    }

    public void onPostRefresh(ArrayList<Parcelable> newItems, String nextHref, int responseCode, boolean success) {
        if (responseCode != HttpStatus.SC_OK){
            handleResponseCode(responseCode);
        } else if (newItems != null && newItems.size() > 0){
                // false for notify of change, we can only notify after resetting listview
                reset(true, false);
                onPostTaskExecute(newItems,nextHref,responseCode,success);
            }


        applyEmptyText();
        notifyDataSetChanged();
        mListView.onRefreshComplete(responseCode == HttpStatus.SC_OK);
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
        mRefreshTask.loadModel = getLoadModel(false);
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

        clearAppendTask();
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
        mPendingPosition = -1;
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
    public boolean onRefresh() {
        if (mActivity.isConnected()){
            if (!isRefreshing()) refresh(true);
            return true;
        } else {
            onPostRefresh(null,null,HttpStatus.SC_SERVICE_UNAVAILABLE,false);
            return false;
        }

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
