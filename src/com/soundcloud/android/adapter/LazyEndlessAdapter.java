
package com.soundcloud.android.adapter;


import com.commonsware.cwac.adapter.AdapterWrapper;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.AppendTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.LazyListView;
import com.soundcloud.api.Request;

import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.apache.http.HttpStatus;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LazyEndlessAdapter extends AdapterWrapper {
    protected View mPendingView = null;
    protected int mPendingPosition = -1;
    private AppendTask mAppendTask;
    protected TextView mEmptyView;
    protected LazyListView mListView;
    protected int mCurrentPage;
    protected ScActivity mActivity;
    protected AtomicBoolean mKeepOnAppending = new AtomicBoolean(true);
    protected Boolean mError = false;
    private String mEmptyViewText = "";
    private Request mRequest;

    public LazyEndlessAdapter(ScActivity activity, LazyBaseAdapter wrapped, Request request) {
        super(wrapped);

        mActivity = activity;
        mCurrentPage = 0;
        mRequest = request;
        wrapped.setWrapper(this);
    }

    /**
     * Create an empty view for the list this adapter will control. This is done
     * here because this adapter will control the visibility of the list
     */
    // XXX make sur emEmptyView is initialized in ctor
    public void createListEmptyView(LazyListView lv) {
        mListView = lv;

        clearEmptyView();

        TextView emptyView = new TextView(mActivity);
        emptyView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        emptyView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        emptyView.setVisibility(View.GONE);
        emptyView.setPadding(5, 5, 5, 5);
        emptyView.setTextAppearance(mActivity, R.style.txt_empty_view);
        // emptyView.setBackgroundColor(mActivityReference.getResources().getColor(R.color.cloudProgressBackgroundCenter));
        mEmptyView = emptyView;

        ((ViewGroup) mListView.getParent()).addView(emptyView);

    }

    public void clearEmptyView() {
        if (mEmptyView != null) {
            if (mEmptyView.getParent() != null) {
                ((ViewGroup) mEmptyView.getParent()).removeView(mEmptyView);
            }
            mEmptyView = null;
        }

    }

    public void setEmptyViewText(String str) {
        mEmptyViewText = str;
    }

    /**
     * Set the current text of the adapter, based on if we are currently dealing
     * with an error
     */
    public void setEmptyviewText() {
        if (!TextUtils.isEmpty(mEmptyViewText) && !mError) {
            mEmptyView.setText(Html.fromHtml(mEmptyViewText));
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


        if (mEmptyView != null) mEmptyView.setText(textToSet);
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
                getTask(),
                savePagingData(),
                saveExtraData()
        };
    }

    @SuppressWarnings("unchecked")
    public void restoreState(Object[] state){
        if (state[0] != null) getData().addAll((Collection<? extends Parcelable>) state[0]);
        if (state[1] != null) restoreTask((AppendTask) state[1]);
        if (state[2] != null) restorePagingData((int[]) state[2]);
        if (state[3] != null) restoreExtraData((String) state[3]);
    }


    /**
     * Restore a possibly still running task that could have been passed in on
     * creation
     */
    protected void restoreTask(AppendTask ap) {
        if (ap != null) {
            mAppendTask = ap;
            ap.setAdapter(this);
        }
    }

    protected AppendTask getTask() {
        return mAppendTask;
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
            setEmptyviewText();
            mListView.setEmptyView(mEmptyView);
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
        if (mKeepOnAppending.get()) {
            return (super.getCount() + 1); // one more for "pending"
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
        if (position == super.getCount() && mKeepOnAppending.get()) {
            if (mPendingView == null) {

                mPendingView = getPendingView(parent);
                mPendingPosition = position;

                if (!getWrappedAdapter().isQuerying()
                        && (mAppendTask == null || CloudUtils.isTaskFinished(mAppendTask))) {

                    mAppendTask = new AppendTask(mActivity.getApp());
                    mAppendTask.loadModel = getLoadModel();
                    mAppendTask.pageSize =  getPageSize();
                    mAppendTask.setAdapter(this);

                    mAppendTask.execute(buildRequest());
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

        // configure the empty view depending on possible error
        setEmptyviewText();
        mListView.setEmptyView(mEmptyView);
        notifyDataSetChanged();

        mActivity.handleException();

        // if (mActivityReference != null)
        // mActivityReference.handleException();
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
     *
     * @return the url
     */
    protected Request getRequest() {
        return new Request(mRequest);
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

    /**
     * Clear and reset this adapter of any data. Primarily used for refreshing
     */
    @SuppressWarnings("unchecked")
    public void refresh(boolean userRefresh) {
        if (mEmptyView != null)
            mEmptyView.setVisibility(View.GONE);
        if (mListView != null)
            mListView.setEmptyView(null);

        mCurrentPage = 0;
        mKeepOnAppending.set(true);

        getWrappedAdapter().refresh(userRefresh);

        if (mAppendTask != null) {
            if (!CloudUtils.isTaskFinished(mAppendTask)) {
                mAppendTask.cancel(true);
            }
            mAppendTask = null;
        }

        mPendingView = null;
        mPendingPosition = -1;
        notifyDataSetChanged();
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
    protected Request buildRequest() {
        Request request = getRequest();
        if (request != null) {
            request.add("limit", getPageSize());
            request.add("offset", getPageSize() * getCurrentPage());
        }
        return request;
    }

    public void onPostQueryExecute() {
        rebindPendingView(mPendingPosition, mPendingView);
        mPendingView = null;
        mPendingPosition = -1;
    }


    public void stopAppending() {
        mKeepOnAppending.set(false);
    }
}