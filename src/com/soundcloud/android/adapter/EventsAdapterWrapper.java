
package com.soundcloud.android.adapter;

import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.LoadActivitiesTask;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.List;

public class EventsAdapterWrapper extends RemoteCollectionAdapter {
    private boolean mVisible;
    private long mSetLastSeenTo;

    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, Content content) {
        super(activity, wrapped, content.uri, Request.to(content.remoteUri), true);
        mAutoAppend = mLocalCollection.last_sync > 0;
    }

     @Override
    public EventsAdapter getWrappedAdapter() {
        return (EventsAdapter) super.getWrappedAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        mVisible = true;
        if (mSetLastSeenTo != -1){
            setLastSeen(mSetLastSeenTo);
            mSetLastSeenTo = -1;
        }
    }

    public void onPause() {
        mVisible = false;
    }

    public void setContent(Content c){
        mContent = c;
        mContentUri = c.uri;
        mRequest = Request.to(c.remoteUri);
        setListLastUpdated();
    }


    @Override
    protected Request getRequest(boolean isRefresh) {
        if (mRequest == null) return null;
        return !TextUtils.isEmpty(mNextHref) && !isRefresh ? new Request(mNextHref) : null;
    }

    public boolean onNewEvents(List<Parcelable> newItems, String nextHref, int responseCode, boolean keepGoing, boolean wasRefresh) {
        if (wasRefresh) {
            doneRefreshing();
            if (mListView != null && mContentUri != null) setListLastUpdated();
        }
        boolean success = (newItems != null && !newItems.isEmpty());
        if (success && wasRefresh) {
            if (!getData().isEmpty() && newItems.contains(getData().get(0))) {
                // merge and notify
                int i = 0;
                for (Parcelable a : newItems) {
                    if (getData().contains(a)) {
                        break;
                    } else {
                        getData().add(i, newItems.get(i));
                        i++;
                    }
                }
                setLastSeen(((Activity) getData().get(0)).created_at.getTime());
                notifyDataSetChanged();
                return true; //done

            } else {
                reset();
            }
        }

        if (responseCode == 0) {
            // local load, but for the sake of the super class :
            keepGoing = true;
            responseCode = HttpStatus.SC_OK;
            // TODO : Change this ^^ stop using status codes and figure out another way to handle errors

            if (newItems.size() < Consts.COLLECTION_PAGE_SIZE) {
                // end of local storage, construct a request for appending
                Request nextRequest = new Request(mRequest);
                final Activity lastItem = success ? ((Activity) newItems.get(newItems.size() - 1))
                        : (Activity) (getData().isEmpty() ? null : getData().get(getData().size() - 1));

                if (lastItem != null) {
                    nextRequest.add("cursor", lastItem.toGUID());
                }
                nextHref = nextRequest.toUrl();
            }
        }

        if (getData().isEmpty() && success) setLastSeen(((Activity) newItems.get(0)).created_at.getTime());
        return super.onPostTaskExecute(newItems, nextHref, responseCode, keepGoing, wasRefresh);
    }

    private void setLastSeen(long time) {
        final String lastSeenKey = getWrappedAdapter().isActivityFeed() ?
                User.DataKeys.LAST_OWN_SEEN : User.DataKeys.LAST_INCOMING_SEEN;

        SoundCloudApplication app = mActivity.getApp();
        final long lastSeen = app.getAccountDataLong(lastSeenKey);

        if (lastSeen < time) {
            if (mVisible) {
                mActivity.getApp().setAccountData(lastSeenKey, time);
            } else {
                mSetLastSeenTo = time;
            }
        }
    }

    @Override
    protected RemoteCollectionTask buildTask() {
        return new LoadActivitiesTask(mActivity.getApp(), this);
    }

    protected void onContentChanged() {
        allowInitialLoading();
        executeRefreshTask();
    }
}
