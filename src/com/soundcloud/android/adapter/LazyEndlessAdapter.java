
package com.soundcloud.android.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.json.JSONObject;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.commonsware.cwac.adapter.AdapterWrapper;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.LazyActivity;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.EventsWrapper;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.view.LazyList;

/**
 * A background task that will be run when there is a need to append more data.
 * Mostly, this code delegates to the subclass, to append the data in the
 * background thread and rebind the pending view once that is done.
 */

public class LazyEndlessAdapter extends AdapterWrapper {

    private static final String TAG = "LazyEndlessAdapter";

    private View pendingView = null;

    private int pendingPosition = -1;

    private AppendTask appendTask;

    private String mUrl;

    private String mQuery;

    private CloudUtils.Model mLoadModel;

    private String mCollectionKey = "";

    private View mEmptyView;

    private View mListView;

    private int mCurrentPage;

    protected LazyActivity mActivity;

    protected AtomicBoolean keepOnAppending = new AtomicBoolean(true);

    protected Boolean mException = false;

    private String mEmptyViewText = "";

    /**
     * Constructors
     * 
     * @param activity : context
     * @param wrapped : the adapter list-backing adapter that this adapter is
     *            being wrapped around
     */

    public LazyEndlessAdapter(LazyActivity activity, LazyBaseAdapter wrapped) {
        super(wrapped);

        mActivity = activity;
        mCurrentPage = 0;
        mQuery = "";

    }

    /**
     * @param activity : context
     * @param wrapped : the adapter list-backing adapter that this adapter is
     *            being wrapped around
     * @param url : the base url that this adapter will receive data from
     */
    public LazyEndlessAdapter(LazyActivity activity, LazyBaseAdapter wrapped, String url) {
        this(activity, wrapped);

        mUrl = url;
    }

    /**
     * @param activity : context
     * @param wrapped : the adapter list-backing adapter that this adapter is
     *            being wrapped around
     * @param url : the base url that this adapter will receive data from
     * @param loadModel : what type of data this adapter will receive, for
     *            parsing purposes
     */
    public LazyEndlessAdapter(LazyActivity activity, LazyBaseAdapter wrapped, String url,
            CloudUtils.Model loadModel) {
        this(activity, wrapped, url);

        mLoadModel = loadModel;
    }

    /**
     * @param activity : context
     * @param wrapped : the adapter list-backing adapter that this adapter is
     *            being wrapped around
     * @param url : the base url that this adapter will receive data from
     * @param loadModel : what type of data this adapter will receive, for
     *            parsing purposes
     * @param collectionKey : if the return data is not an array, this key in
     *            the object will hold the array
     */
    public LazyEndlessAdapter(LazyActivity activity, LazyBaseAdapter wrapped, String url,
            CloudUtils.Model loadModel, String collectionKey) {
        this(activity, wrapped, url, loadModel);

        mCollectionKey = collectionKey;
    }

    /**
     * Create an empty view for the list this adapter will control. This is done
     * here because this adapter will control the visibility of the list
     * 
     * @param lv
     */
    public void createListEmptyView(LazyList lv) {

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
        emptyView.setTextAppearance(mActivity, R.style.txt_empty_view);
        // emptyView.setBackgroundColor(mActivity.getResources().getColor(R.color.cloudProgressBackgroundCenter));
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
        if (!CloudUtils.stringNullEmptyCheck(mEmptyViewText) && !mException) {
            ((TextView) mEmptyView).setText(Html.fromHtml(mEmptyViewText));
            return;
        }

        String textToSet = "";
        switch (getLoadModel()) {
            case track:
                textToSet = mException == false ? mActivity.getResources().getString(
                        R.string.tracklist_empty) : mActivity.getResources().getString(
                        R.string.tracklist_error);
                break;
            case user:
                textToSet = mException == false ? mActivity.getResources().getString(
                        R.string.userlist_empty) : mActivity.getResources().getString(
                        R.string.userlist_error);
                break;
            case comment:
                textToSet = mException == false ? mActivity.getResources().getString(
                        R.string.tracklist_empty) : mActivity.getResources().getString(
                        R.string.commentslist_error);
                break;
            case event:
                textToSet = mException == false ? mActivity.getResources().getString(
                        R.string.tracklist_empty) : mActivity.getResources().getString(
                        R.string.tracklist_error);
                break;
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

    /**
     * Restore a possibily still running task that could have been passed in on
     * creation
     * 
     * @param ap
     */
    public void restoreTask(AppendTask ap) {
        if (ap != null) {
            appendTask = ap;
            ap.setContext(this, mActivity);
        }
    }

    /**
     * Get a possibly currently running task to save
     * 
     * @return
     */
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

    /**
     * Restore the current paging data
     * 
     * @return an integer list {whether to keep retrieving data, the current
     *         page the adapter is on}
     */
    public void restorePagingData(int[] restore) {
        keepOnAppending.set(restore[0] == 1 ? true : false);
        mCurrentPage = restore[1];
        mException = restore[2] == 1 ? true : false;

        if (!keepOnAppending.get()) {
            setEmptyviewText();
            ((AdapterView<ListAdapter>) mListView).setEmptyView(mEmptyView);
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

    /**
     * Get the collection key associated with this adapter
     * 
     * @return
     */
    public String getCollectionKey() {
        return mCollectionKey;
    }

    /**
     * Get the Load Model associated with this adapter
     * 
     * @return
     */
    public CloudUtils.Model getLoadModel() {
        return mLoadModel;
    }

    /**
     * Get the current page of this adapter
     * 
     * @return
     */
    public int getCurrentPage() {
        return mCurrentPage;
    }

    /**
     * Get the data of this adapter by getting the wrapped data
     * 
     * @return
     */
    public List<Parcelable> getData() {

        return (this.getWrappedAdapter()).getData();
    }

    /**
     * Get the current number of items in this adapter, accounting for a loading
     * view
     */
    @Override
    public int getCount() {
        if (keepOnAppending.get()) {
            return (super.getCount() + 1); // one more for "pending"
        }

        return (super.getCount());
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
                if (appendTask == null || CloudUtils.isTaskFinished(appendTask)) {
                    appendTask = new AppendTask();
                    appendTask.loadModel = getLoadModel();
                    appendTask.setContext(this, mActivity);
                    appendTask.execute(this.buildRequest());
                }

            }

            return (pendingView);
        } else if (convertView == pendingView) {
            // if we're not at the bottom, and we're getting the
            // pendingView back for recycling, skip the recycle
            // process
            return (super.getView(position, null, parent));
        }

        return (super.getView(position, convertView, parent));
    }

    /**
     * A load task has just executed, set the current adapter in response
     * 
     * @param keepgoing
     */
    @SuppressWarnings("unchecked")
    public void onPostTaskExecute(Boolean keepgoing) {
        keepOnAppending.set(keepgoing);
        rebindPendingView(pendingPosition, pendingView);
        pendingView = null;
        pendingPosition = -1;

        // configure the empty view depending on possible exceptions
        setEmptyviewText();
        ((AdapterView<ListAdapter>) mListView).setEmptyView(mEmptyView);
        notifyDataSetChanged();

        // if (mActivity != null)
        // mActivity.handleException();
    }

    /**
     * Create a row for displaying a loading message by getting a row from the
     * wrapped adapter and displaying the loading views of that row
     * 
     * @param parent
     * @return
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
     * 
     * @param position
     * @param row
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
     * We just got the data, so if there is any data that needs to be captured
     * from it, do so This will be overridden by extending classes as necessary
     * ({ @link com.soundcloud.android.adapter.EventsAdapter })
     * 
     * @param data : the data node that was just retrieved
     */
    public void onDataNode(JSONObject data) {
        // TODO Auto-generated method stub
        return;
    }

    /**
     * Set the url for this adapter
     * 
     * @param url : url this adapter will use to get data from
     */
    protected void setPath(String url) {
        setPath(url, "");
    }

    /**
     * Set the url for this adapter
     * 
     * @param url : url this adapter will use to get data from
     * @param query : if this adapter is performing a search, this is the user's
     *            search query
     */
    public void setPath(String url, String query) {
        mUrl = url;
        mQuery = query;
    }

    /**
     * Set the current load model for this adapter
     * 
     * @param loadModel : the load model
     */
    protected void setLoadModel(CloudUtils.Model loadModel) {
        mLoadModel = loadModel;
    }

    /**
     * Get the current url for this adapter
     * 
     * @return the url
     */
    protected String getUrl() {
        return mUrl;
    }

    /**
     * Get the current search query for this adapter
     * 
     * @return the query
     */
    protected String getQuery() {
        return mQuery;
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
        e.printStackTrace();
        mException = true;
    }

    /**
     * Clear and reset this adapter of any data. Primarily used for refreshing
     */
    @SuppressWarnings("unchecked")
    public void clear() {

        if (mEmptyView != null)
            mEmptyView.setVisibility(View.GONE);
        if (mListView != null)
            ((AdapterView<ListAdapter>) mListView).setEmptyView(null);

        mCurrentPage = 0;
        keepOnAppending.set(true);

        getWrappedAdapter().clear();

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
    private HttpUriRequest buildRequest() {
        String baseUrl = getUrl();
        String query = getQuery();

        // build the querystring
        Uri u = Uri.parse(baseUrl);
        Uri.Builder builder = u.buildUpon();

        if (!query.contentEquals(""))
            builder.appendQueryParameter("q", query);
        if (baseUrl.indexOf("limit") == -1)
            builder.appendQueryParameter("limit", String.valueOf(mActivity.getPageSize()));

        builder.appendQueryParameter("offset", String.valueOf(mActivity.getPageSize()
                * (getCurrentPage())));
        builder.appendQueryParameter("consumer_key", mActivity.getResources().getString(
                R.string.consumer_key));

        HttpUriRequest req;
        try {
            req = mActivity.getSoundCloudApplication().getPreparedRequest(
                    builder.build().toString());
            return req;
        } catch (OAuthMessageSignerException e) {
            setException(e);
            e.printStackTrace();
        } catch (OAuthExpectationFailedException e) {
            setException(e);
            e.printStackTrace();
        } catch (OAuthCommunicationException e) {
            setException(e);
            e.printStackTrace();
        } catch (IllegalStateException e) {
            setException(e);
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            setException(e);
            e.printStackTrace();
        } catch (IOException e) {
            setException(e);
            e.printStackTrace();
        }

        return null;

    }

    /**
     * A background task that will be run when there is a need to append more
     * data. Mostly, this code delegates to the subclass, to append the data in
     * the background thread and rebind the pending view once that is done.
     */
    public class AppendTask extends AsyncTask<HttpUriRequest, Parcelable, Boolean> {
        private static final String TAG = "AppendTask";

        private WeakReference<LazyEndlessAdapter> mAdapterReference;

        private WeakReference<LazyActivity> mActivityReference;

        private Boolean keepGoing = true;

        private ArrayList<Parcelable> newItems;

        public CloudUtils.Model loadModel;

        /**
         * Set the activity and adapter that this task now belong to. This will
         * be set as new context is destroyed and created in response to
         * orientation changes
         * 
         * @param lazyEndlessAdapter
         * @param activity
         */
        public void setContext(LazyEndlessAdapter lazyEndlessAdapter, LazyActivity activity) {
            mAdapterReference = new WeakReference<LazyEndlessAdapter>(lazyEndlessAdapter);
            mActivityReference = new WeakReference<LazyActivity>(activity);
        }

        /**
         * Do any task preparation we need to on the UI thread
         */
        @Override
        protected void onPreExecute() {
            if (mAdapterReference.get() != null)
                mAdapterReference.get().onPreTaskExecute();
        }

        /**
         * Add all new items that have been retrieved, now that we are back on a
         * UI thread
         */
        @Override
        protected void onPostExecute(Boolean keepGoing) {
            if (mAdapterReference.get() != null) {
                if (newItems != null && newItems.size() > 0) {
                    for (Parcelable newitem : newItems) {
                        mAdapterReference.get().getData().add(newitem);
                    }
                }

                mAdapterReference.get().onPostTaskExecute(keepGoing);
            }
            if (mActivityReference.get() != null) {
                mActivityReference.get().handleError();
                mActivityReference.get().handleException();
            }
        }

        /**
         * Perform our background loading
         */
        @SuppressWarnings("unchecked")
        @Override
        protected Boolean doInBackground(HttpUriRequest... params) {

            // make sure we have a valid url
            HttpUriRequest req = params[0];
            if (req == null)
                return false;

            Boolean keep_appending = true;

            try {

                InputStream is = mActivityReference.get().getSoundCloudApplication()
                        .executeRequest(req);

                ObjectMapper mapper = new ObjectMapper();

                if (newItems != null)
                    newItems.clear();
                switch (mAdapterReference.get().getLoadModel()) {
                    case track:
                        newItems = mapper.readValue(is, TypeFactory.collectionType(ArrayList.class,
                                Track.class));
                        break;
                    case user:
                        newItems = mapper.readValue(is, TypeFactory.collectionType(ArrayList.class,
                                User.class));
                        break;
                    /*
                     * case comment: newItems = mapper.readValue(is,
                     * TypeFactory.collectionType(ArrayList.class,
                     * Comment.class)); break;
                     */
                    case event:
                        EventsWrapper evtWrapper = mapper.readValue(is, EventsWrapper.class);
                        newItems = new ArrayList<Parcelable>(evtWrapper.getCollection().size());
                        for (Event evt : evtWrapper.getCollection())
                            newItems.add(evt);

                        if (mAdapterReference.get() != null && evtWrapper.getNext_href() != null) // set
                                                                                                  // the
                                                                                                  // params
                                                                                                  // of
                                                                                                  // the
                                                                                                  // next
                                                                                                  // url
                            ((EventsAdapterWrapper) mAdapterReference.get())
                                    .onNextEventsParam(evtWrapper.getNext_href());
                        break;
                }

                // resolve data
                for (Parcelable p : newItems)
                    if (mActivityReference.get() != null)
                        mActivityReference.get().resolveParcelable(p);

                // we have less than the requested number of items, so we are
                // done grabbing items for this list
                if (mActivityReference.get() != null)
                    if (newItems == null
                            || newItems.size() < mActivityReference.get().getPageSize())
                        keep_appending = false;

                // we were successful, so increment the adapter
                if (mAdapterReference.get() != null)
                    mAdapterReference.get().incrementPage();

                return keep_appending;

            } catch (Exception e) {
                e.printStackTrace();
                mException = true;
                // if (mActivity != null) mActivity.setException(e);
                if (mAdapterReference.get() != null)
                    mAdapterReference.get().setException(e);
            }

            // there was an exception of some kind, return failure
            return false;

        }

    }
}
