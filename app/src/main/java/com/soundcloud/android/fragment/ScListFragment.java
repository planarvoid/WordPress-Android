package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockListFragment;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.CollectionLoader;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.android.utils.NetworkConnectivityListener;
import com.soundcloud.android.view.EmptyCollection;
import com.soundcloud.android.view.ScListView;

import android.content.Context;
import android.graphics.Color;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListAdapter;

import java.util.List;

public class ScListFragment extends SherlockListFragment
        implements DetachableResultReceiver.Receiver, android.support.v4.app.LoaderManager.LoaderCallbacks<List<Parcelable>> {

    private CollectionLoader mItemLoader;

    private int mState;
    private static final int INIT = 0;
    private static final int WAITING = 1; // waiting for interaction
    private static final int LOADING = 2; // refreshing cursor
    private static final int APPENDING = 3;
    private static final int DONE = 4;
    private static final int ERROR = 5;

    private ScListView mListView;
    private ScBaseAdapter mBaseAdapter;
    private EmptyCollection mEmptyCollection;
    private EmptyCollection mDefaultEmptyCollection;
    private String mEmptyCollectionText;

    private DetachableResultReceiver mDetachableReceiver;
    private Content mContent;
    private boolean mIsConnected;
    private NetworkConnectivityListener connectivityListener;
    protected static final int CONNECTIVITY_MSG = 0;
    ;
    protected boolean mAppendable = true;
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
        mContent = Content.byUri((Uri) getArguments().get("contentUri"));
        setRetainInstance(true);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBaseAdapter = new ScBaseAdapter(getActivity(), mContent);
        setListAdapter(mBaseAdapter);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();

        FrameLayout root = new FrameLayout(context);

        mListView = buildList();
        root.addView(mListView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ------------------------------------------------------------------

        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        return root;
    }

    public void setCustomEmptyCollection(EmptyCollection emptyCollection) {
        mEmptyCollection = emptyCollection;
    }

    public void setEmptyViewText(String str) {
        mEmptyCollectionText = str;
    }

    public ScActivity getScActivity() {
        return (ScActivity) getActivity();
    }

    public ScListView getScListView() {
        return mListView;
    }

    public ScListView buildList() {
        return configureList(new ScListView((ScListActivity) getActivity()), false);
    }

    public ScListView buildList(boolean longClickable) {
        return configureList(new ScListView((ScListActivity) getActivity()), longClickable);
    }


    public ScListView configureList(ScListView lv, boolean longClickable) {
        //lv.setId(android.R.id.list);
        lv.getRefreshableView().setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        lv.getRefreshableView().setFastScrollEnabled(false);
        lv.getRefreshableView().setDivider(getResources().getDrawable(R.drawable.list_separator));
        lv.getRefreshableView().setDividerHeight(1);
        lv.getRefreshableView().setCacheColorHint(Color.TRANSPARENT);
        lv.getRefreshableView().setLongClickable(longClickable);
        return lv;
    }

    public void setListAdapter(ListAdapter adapter) {
        getListView().setAdapter(adapter);
    }

    public ScBaseAdapter getListAdapter() {
        return mBaseAdapter;
    }

    public void setAppendable(boolean appendable) {
        if (appendable != mAppendable) {
            mAppendable = appendable;
            //getScListView().toggleFooterVisibility(mAppendable);
        }
    }

    @Override
    public Loader<List<Parcelable>> onCreateLoader(int i, Bundle bundle) {
        mState = LOADING;
        mItemLoader = new CollectionLoader(getActivity(), mContent.uri);
        return mItemLoader;
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<List<Parcelable>> listLoader, List<Parcelable> data) {
        setAppendable(data != null && data.size() == Consts.COLLECTION_PAGE_SIZE);
        mState = mAppendable ? WAITING : DONE;
        getListAdapter().getData().addAll(data);
        getListAdapter().notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<List<Parcelable>> listLoader) {
        mState = LOADING;
    }

    private Uri getCurrentUri() {
        return mContent.uri.buildUpon().appendQueryParameter("limit", String.valueOf(mPageIndex * Consts.COLLECTION_PAGE_SIZE)).build();
    }

    protected DetachableResultReceiver getReceiver() {
        if (mDetachableReceiver == null) mDetachableReceiver = new DetachableResultReceiver(new Handler());
        mDetachableReceiver.setReceiver(this);
        return mDetachableReceiver;
    }


    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_APPEND_ERROR: {
                mState = ERROR;
                // toggle the footer view to show no more items
                break;
            }
            case ApiSyncService.STATUS_APPEND_FINISHED: {
                int itemCount = resultData.getInt("itemCount");
                setAppendable(itemCount == Consts.COLLECTION_PAGE_SIZE);
                if (itemCount > 0) {
                    mPageIndex++;
                    if (mItemLoader != null) {

                        mItemLoader.reset();
                        mItemLoader.pageIndex = mPageIndex;
                        mItemLoader.startLoading();
                    }

                }
                break;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        connectivityListener = new NetworkConnectivityListener();
        connectivityListener.registerHandler(connHandler, CONNECTIVITY_MSG);
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

    protected void onDataConnectionUpdated(boolean isConnected) {
        mIsConnected = isConnected;
        if (isConnected && !isConnected) {
            if (getListAdapter().needsItems() && getScActivity().getApp().getAccount() != null) {

            }
        }
    }


    private void setEmptyView(EmptyCollection emptyCollection) {
        final ViewGroup root = (ViewGroup) getView();
        if (root.findViewById(android.R.id.empty) != null) {
            root.removeView(root.findViewById(android.R.id.empty));
        }
        root.addView(emptyCollection);
        emptyCollection.setId(android.R.id.empty);
    }

    private void applyEmptyView() {
        final boolean error = mState == ERROR;

        if (mEmptyCollection != null && !error) {
            setEmptyView(mEmptyCollection);
        } else {
            if (mDefaultEmptyCollection == null) {
                mDefaultEmptyCollection = new EmptyCollection(getActivity());
            }
            mDefaultEmptyCollection.setImage(error ? R.drawable.empty_connection : R.drawable.empty_collection);
            mDefaultEmptyCollection.setMessageText((!error && !TextUtils.isEmpty(mEmptyCollectionText)) ? mEmptyCollectionText : getEmptyText());
            setEmptyView(mDefaultEmptyCollection);
        }
    }

    private String getEmptyText() {
        final Class loadModel = getListAdapter().getLoadModel();
        final boolean error = mState == ERROR;
        if (Track.class.equals(loadModel)) {
            return !error ? getResources().getString(
                    R.string.tracklist_empty) : getResources().getString(
                    R.string.tracklist_error);
        } else if (User.class.equals(loadModel)) {
            return !error ? getResources().getString(
                    R.string.userlist_empty) : getResources().getString(
                    R.string.userlist_error);
        } else if (Comment.class.equals(loadModel)) {
            return !error ? getResources().getString(
                    R.string.tracklist_empty) : getResources().getString(
                    R.string.commentslist_error);
        } else if (Activity.class.equals(loadModel)) {
            return !error ? getResources().getString(
                    R.string.tracklist_empty) : getResources().getString(
                    R.string.tracklist_error);
        } else {
            return "";
        }
    }

}