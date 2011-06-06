
package com.soundcloud.android.adapter;


import com.commonsware.cwac.adapter.AdapterWrapper;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LazyEndlessAdapter extends AdapterWrapper {
    private View pendingView = null;
    private int pendingPosition = -1;
    private AppendTask appendTask;
    private View mEmptyView;
    private LazyListView mListView;
    private int mCurrentPage;
    protected ScActivity mActivity;
    protected AtomicBoolean keepOnAppending = new AtomicBoolean(true);
    protected Boolean mException = false;
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
    public void createListEmptyView(LazyListView lv) {

        mListView = lv;

        if (mEmptyView != null) {
            if (mEmptyView.getParent() != null) {
                ((ViewGroup) mEmptyView.getParent()).removeView(mEmptyView);
            }
            mEmptyView = null;
        }

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

    public void setEmptyViewText(String str) {
        mEmptyViewText = str;
    }

    /**
     * Set the current text of the adapter, based on if we are currently dealing
     * with an exception
     */
    public void setEmptyviewText() {
        if (!TextUtils.isEmpty(mEmptyViewText) && !mException) {
            ((TextView) mEmptyView).setText(Html.fromHtml(mEmptyViewText));

            // does not work properly
            /*
            CloudUtils.clickify(((TextView)mEmptyView), "Suggested Users", new ClickSpan.OnClickListener() {
                @Override
                public void onClick() {
                    mActivity.startActivity(new Intent(mActivity, Main.class).putExtra("userBrowserIndex", 5));
                }
            }, true);
            */
            return;
        }

        String textToSet = "";


        if (Track.class.equals(getWrappedAdapter().getLoadModel())) {
            textToSet = !mException ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.tracklist_error);

        } else if (User.class.equals(getWrappedAdapter().getLoadModel())) {
            textToSet = !mException ? mActivity.getResources().getString(
                    R.string.userlist_empty) : mActivity.getResources().getString(
                    R.string.userlist_error);
        } else if (Comment.class.equals(getWrappedAdapter().getLoadModel())) {
            textToSet = !mException ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.commentslist_error);
        } else if (Event.class.equals(getWrappedAdapter().getLoadModel())) {
            textToSet = !mException ? mActivity.getResources().getString(
                    R.string.tracklist_empty) : mActivity.getResources().getString(
                    R.string.tracklist_error);
        }

        ((TextView) mEmptyView).setText(textToSet);
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
        if (state[0] != null) this.getData().addAll((Collection<? extends Parcelable>) state[0]);
        if (state[1] != null) this.restoreTask((AppendTask) state[1]);
        if (state[2] != null) this.restorePagingData((int[]) state[2]);
        if (state[3] != null) this.restoreExtraData((String) state[3]);
    }


    /**
     * Restore a possibly still running task that could have been passed in on
     * creation
     */
    public void restoreTask(AppendTask ap) {
        if (ap != null) {
            appendTask = ap;
            ap.setAdapter(this);
        }
    }

    public AppendTask getTask() {
        return appendTask;
    }

    /**
     * Save the current paging data
     *
     * @return an integer list {whether to keep retrieving data, the current
     *         page the adapter is on}
     */
    public int[] savePagingData() {

        int[] ret = new int[3];
        ret[0] = (keepOnAppending.get()) ? 1 : 0;
        ret[1] = mCurrentPage;
        ret[2] = mException ? 1 : 0;

        return ret;

    }

    public void restorePagingData(int[] restore) {
        keepOnAppending.set(restore[0] == 1);
        mCurrentPage = restore[1];
        mException = restore[2] == 1;

        if (!keepOnAppending.get()) {
            setEmptyviewText();
            mListView.setEmptyView(mEmptyView);
        }

    }

    /**
     * Save the current extra data
     *
     * @return a string representing any extra data pertaining to this adapter
     */
    public String saveExtraData() {
        return "";
    }

    /**
     * Restore the extra data
     *
     * @param restore : the string data to restore
     */
    public void restoreExtraData(String restore) {
    }

    public Class<?> getLoadModel() {
        return getWrappedAdapter().getLoadModel();
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public List<Parcelable> getData() {
        return (this.getWrappedAdapter()).getData();
    }

    @Override
    public int getCount() {
        if (keepOnAppending.get()) {
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

        if (position == super.getCount() && keepOnAppending.get()) {
            if (pendingView == null) {
                pendingView = getPendingView(parent);
                pendingPosition = position;
                if (!getWrappedAdapter().isQuerying()
                        && (appendTask == null || CloudUtils.isTaskFinished(appendTask))) {
                    appendTask = new AppendTask(mActivity.getSoundCloudApplication());
                    appendTask.loadModel = getLoadModel();
                    appendTask.pageSize =  getPageSize();
                    appendTask.setAdapter(this);

                    appendTask.execute(buildRequest());
                }

            }

            return pendingView;
        } else if (convertView == pendingView) {
            // if we're not at the bottom, and we're getting the
            // pendingView back for recycling, skip the recycle
            // process
            return (super.getView(position, null, parent));
        }

        return (super.getView(position, convertView, parent));
    }

    private int getPageSize() {
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mActivity).getString(
                "defaultPageSize", "20"));
    }

    /**
     * A load task has just executed, set the current adapter in response
     *
     * @param keepgoing
     */
    public void onPostTaskExecute(Boolean keepgoing) {
        if (keepgoing != null) {
            keepOnAppending.set(keepgoing);
        } else {
            mException = true;
        }

        rebindPendingView(pendingPosition, pendingView);
        pendingView = null;
        pendingPosition = -1;

        // configure the empty view depending on possible exceptions
        setEmptyviewText();
        mListView.setEmptyView(mEmptyView);
        notifyDataSetChanged();

        mActivity.handleException();
        mActivity.handleError();

        // if (mActivityReference != null)
        // mActivityReference.handleException();
    }

    /**
     * Create a row for displaying a loading message by getting a row from the
     * wrapped adapter and displaying the loading views of that row
     */
    protected View getPendingView(ViewGroup parent) {
        ViewGroup row = (this.getWrappedAdapter()).createRow();
        row.findViewById(R.id.row_holder).setVisibility(View.GONE);
        row.findViewById(R.id.stub_loading).setVisibility(View.VISIBLE);

        ProgressBar list_loader = (ProgressBar) row.findViewById(R.id.list_loading);
        list_loader.setVisibility(View.VISIBLE);

        return (row);
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
        return mRequest;
    }

    /**
     * A load task is about to be executed, do whatever we have to to get ready
     */
    public void onPreTaskExecute() {
        mException = false;
    }

    /**
     * There was an exception during the load task
     *
     * @param e : the exception
     */
    public void setException(Exception e) {
        mException = true;
    }

    /**
     * Clear and reset this adapter of any data. Primarily used for refreshing
     */
    @SuppressWarnings("unchecked")
    public void refresh() {
        if (mEmptyView != null)
            mEmptyView.setVisibility(View.GONE);
        if (mListView != null)
            mListView.setEmptyView(null);

        mCurrentPage = 0;
        keepOnAppending.set(true);

        getWrappedAdapter().refresh();

        if (appendTask != null) {
            if (!CloudUtils.isTaskFinished(appendTask)) {
                appendTask.cancel(true);
            }
            appendTask = null;
        }

        this.notifyDataSetChanged();
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
    private Request buildRequest() {
        Request request = getRequest();
        request.add("limit", getPageSize());
        request.add("offset", getPageSize() * getCurrentPage());
        return request;
    }

    public void onPostQueryExecute() {
        rebindPendingView(pendingPosition, pendingView);
        pendingView = null;
        pendingPosition = -1;
    }
}
