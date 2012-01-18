package com.soundcloud.android.adapter;


import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.Consts;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Resource;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.android.task.UpdateCollectionTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class RemoteCollectionAdapter extends LazyEndlessAdapter {

    private ChangeObserver mChangeObserver;
    private DetachableResultReceiver mDetachableReceiver;
    private Boolean mIsSyncable;
    protected String mNextHref;
    protected long mLastUpdated = -1;

    public RemoteCollectionAdapter(ScActivity activity, LazyBaseAdapter wrapped, Uri contentUri, Request request, boolean autoAppend) {
        super(activity, wrapped, contentUri, request, autoAppend);

        if (contentUri != null){
            mChangeObserver = new ChangeObserver();
            activity.getContentResolver().registerContentObserver(contentUri, true, mChangeObserver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isSyncable()) {
            setListLastUpdated();
            if (isStale(false)) {
                refresh(false);
            }
        }
    }

    @Override
    public Object[] saveExtraData() {
        return new Object[]{
                mNextHref,
                mLastUpdated,
                saveResultReceiver()
        };
    }

    @Override
    public void restoreExtraData(Object[] state) {
        mNextHref = (String) state[0];
        mLastUpdated = Long.parseLong(String.valueOf(state[1]));
        if (state[2] != null) {
            restoreResultReceiver((DetachableResultReceiver) state[2]);
        }
    }

    @Override
    protected Request getRequest(boolean isRefresh) {
        if (mRequest == null) return null;
        return !(isRefresh) && !TextUtils.isEmpty(mNextHref) ? new Request(mNextHref) : new Request(mRequest);
    }

    @Override
    public void refresh(final boolean userRefresh) {
        super.refresh(userRefresh);

        if (isSyncable()) {
            requestSync();
        } else {
            clearAppendTask();
            load(true);
        }

        notifyDataSetChanged();
    }

    @Override
    public void reset() {
        super.reset();
        mNextHref = "";
    }

    @Override
    protected boolean canShowEmptyView(){
       return (!isSyncable() || mLastUpdated > 0) && super.canShowEmptyView();
    }

    protected void setNextHref(String nextHref) {
       mNextHref = nextHref;
    }

    protected RemoteCollectionTask buildTask() {
        return new RemoteCollectionTask(mActivity.getApp(), this);
    }

    public boolean onPostTaskExecute(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing, boolean wasRefresh) {
        mKeepGoing = keepGoing;
        boolean success = (newItems != null && newItems.size() > 0) || responseCode == HttpStatus.SC_OK;
        if (success) {
            if (wasRefresh){
                reset();
                if (mListView != null && mContentUri != null) setListLastUpdated();
            }
            setNextHref(nextHref);
            addNewItems(newItems);
            mState = IDLE;
            increasePageIndex();
        } else {
            handleResponseCode(responseCode);
        }


        if (wasRefresh || !mRefreshing){
            doneRefreshing();
        }

        applyEmptyView();
        mPendingView = null;
        mAppendTask = null;
        notifyDataSetChanged();
        return success;
    }

    protected void addNewItems(List<Parcelable> newItems){
        if (newItems == null || newItems.size() == 0)  return;
        for (Parcelable newItem : newItems) {
            getWrappedAdapter().addItem(newItem);
        }
        checkForStaleItems(newItems);
    }

    public void setListLastUpdated() {
        if (mListView != null) {
            final long lastUpdated = LocalCollection.getLastSync(getContentUri(true), mActivity.getContentResolver());
            mLastUpdated = lastUpdated;
            if (lastUpdated > 0) mListView.setLastUpdated(lastUpdated);
        }
    }

    protected void checkForStaleItems(List<Parcelable> newItems){
        if (!(CloudUtils.isWifiConnected(mActivity)) || newItems == null || newItems.size() == 0 || !(newItems.get(0) instanceof Resource))
            return;

        final long stale = System.currentTimeMillis() - ((Resource) newItems.get(0)).getStaleTime();

        Map<Long, Resource> toUpdate = new HashMap<Long, Resource>();
        for (Parcelable newItem : newItems) {
            if (newItem instanceof Resource){
                Resource resource = (Resource) newItem;
                if (resource.getLastUpdated() < stale) {
                    toUpdate.put(resource.getResourceId(), resource);
                }
            }
        }

        if (toUpdate.size() > 0){
            mUpdateCollectionTask =new UpdateCollectionTask(mActivity.getApp(),getLoadModel(false));
            mUpdateCollectionTask.setAdapter(this);
            mUpdateCollectionTask.execute(toUpdate);
        }

    }

    protected void clearUpdateTask() {
        if (mUpdateCollectionTask != null && !CloudUtils.isTaskFinished(mUpdateCollectionTask)) mUpdateCollectionTask.cancel(true);
        mUpdateCollectionTask = null;
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

    protected RemoteCollectionTask.CollectionParams getCollectionParams(final boolean refresh){
        return new RemoteCollectionTask.CollectionParams() {{
                loadModel = getLoadModel(refresh);
                contentUri = getContentUri(refresh);
                pageIndex = getPageIndex(refresh);
                request = buildRequest(refresh);
                isRefresh = refresh;
                refreshPageItems = !isSyncable();
            }};
    }

    protected boolean isStale(boolean refresh){
        if (mLastUpdated == -1){
            mLastUpdated = LocalCollection.getLastSync(getContentUri(refresh), mActivity.getContentResolver());
        }
        return (getPageIndex(refresh) == 0 && System.currentTimeMillis() - mLastUpdated > Consts.DEFAULT_REFRESH_MINIMUM);
    }

    protected boolean isSyncable(){
        if (mIsSyncable == null){
            mIsSyncable = mContent != null && mContent.isSyncable();
        }
        return mIsSyncable;
    }

    protected DetachableResultReceiver getReceiver(){
        if (mDetachableReceiver == null) mDetachableReceiver = new DetachableResultReceiver(new Handler());
        mDetachableReceiver.setReceiver(this);
        return mDetachableReceiver;
    }

    public void restoreResultReceiver(DetachableResultReceiver receiver){
        mDetachableReceiver = receiver;
        mDetachableReceiver.setReceiver(this);

    }

    public DetachableResultReceiver saveResultReceiver() {
        if (mDetachableReceiver != null) mDetachableReceiver.clearReceiver();
        return mDetachableReceiver;
    }

    protected void requestSync(){
        final Intent intent = new Intent(mActivity, ApiSyncService.class);
        intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, getReceiver());
        intent.setData(mContent.uri);
        intent.putExtra(ApiSyncService.EXTRA_CHECK_PERFORM_LOOKUPS,false);
        mActivity.startService(intent);
    }

    protected void doneRefreshing(){
        if (isSyncable()) setListLastUpdated();
        if  (mListView != null) mListView.onRefreshComplete(false);;
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case ApiSyncService.STATUS_SYNC_FINISHED:
            case ApiSyncService.STATUS_SYNC_ERROR: {
                mRefreshing = false;
                if (!resultData.getBoolean(mContentUri.toString())){
                    doneRefreshing(); // nothing changed
                }
                break;
            }
        }
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            reset();
        }
    }
}
