package com.soundcloud.android.adapter;


import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.LoadCollectionTask;
import com.soundcloud.android.task.SyncedCollectionTask;
import com.soundcloud.android.task.LoadRemoteCollectionTask;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class RemoteCollectionAdapter extends LazyEndlessAdapter {

    private Uri mSyncDataUri;
    private boolean mWaitingOnSync;

    public RemoteCollectionAdapter(ScActivity activity, LazyBaseAdapter wrapped, Uri contentUri, Request request, boolean autoAppend) {
        super(activity,wrapped,contentUri,request,autoAppend);
    }

    @Override
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

    @Override
    public void append() {
        startAppendTask();
    }



    protected void startAppendTask(){
        mState = APPENDING;
        mAppendTask = new LoadRemoteCollectionTask(mActivity.getApp(), buildAppendParams());
        mAppendTask.setAdapter(this);
        mAppendTask.execute();
    }

    protected void startRefreshTask(final boolean userRefresh) {
        mState = REFRESHING;
        mRefreshTask = new LoadRemoteCollectionTask(mActivity.getApp(), buildRefreshParams());
        if (!userRefresh) {
            ((LoadRemoteCollectionTask) mRefreshTask).setLastRefresh(LocalCollection.getLastSync(mActivity.getContentResolver(), getContentUri(true)));
        }
        mRefreshTask.setAdapter(this);
        mRefreshTask.execute();
    }

    protected LoadRemoteCollectionTask.RemoteCollectionParams buildAppendParams() {
        return new LoadRemoteCollectionTask.RemoteCollectionParams() {
            {
                loadModel = getLoadModel(false);
                contentUri = getContentUri(false);
                pageIndex = getPageIndex(false);
                request = buildRequest(false);
            }
        };
    }

    protected LoadRemoteCollectionTask.RemoteCollectionParams buildRefreshParams(){
        return new LoadRemoteCollectionTask.RemoteCollectionParams(){
            {
                loadModel = getLoadModel(true);
                contentUri = getContentUri(true);
                pageIndex = getPageIndex(true);
                request = buildRequest(true);
                refresh = true;
            }
        };
    }

    public void onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing) {
        if ((newItems != null && newItems.size() > 0) || responseCode == HttpStatus.SC_OK) {
            mState = keepGoing ? WAITING : DONE;
            mNextHref = nextHref;
            increasePageIndex();
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
        mAppendTask = null;
        notifyDataSetChanged();
    }

    public void onPostRefresh(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing) {
        if (handleResponseCode(responseCode) || (newItems != null && newItems.size() > 0)) {
            reset(false);
            mNextHref = nextHref;
            getData().addAll(newItems);
            increasePageIndex();
        } else {
            onEmptyRefresh();
        }

        if (mState < ERROR) mState = keepGoing ? WAITING : DONE;
        if (mListView != null) {
            mListView.onRefreshComplete(false);
            setListLastUpdated();
        }

        applyEmptyView();
        mPendingView = null;
        mRefreshTask = null;
        mAppendTask = null;

        notifyDataSetChanged();
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


}
