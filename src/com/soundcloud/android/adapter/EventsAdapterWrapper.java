
package com.soundcloud.android.adapter;

import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.task.LoadActivitiesTask;
import com.soundcloud.android.task.RemoteCollectionTask;
import com.soundcloud.android.utils.DetachableResultReceiver;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public class EventsAdapterWrapper extends RemoteCollectionAdapter {
    public EventsAdapterWrapper(ScActivity activity, LazyBaseAdapter wrapped, Content content) {
        super(activity, wrapped, content.uri, Request.to(content.remoteUri), true);
    }

     @Override
    public EventsAdapter getWrappedAdapter() {
        return (EventsAdapter) super.getWrappedAdapter();
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

    public boolean onNewEvents(List<Parcelable> newItems, String nextHref, String nextCursor, int responseCode, boolean keepGoing, boolean wasRefresh) {
        final String lastSeenKey = getWrappedAdapter().isActivityFeed() ?
                User.DataKeys.LAST_OWN_SEEN : User.DataKeys.LAST_INCOMING_SEEN;

        if (wasRefresh) {
            doneRefreshing();
            if (mListView != null && mContentUri != null) setListLastUpdated();
        }

        boolean success = (newItems != null && !newItems.isEmpty());
        if (success) {
            SoundCloudApplication app = mActivity.getApp();

            final Activity first = (Activity) newItems.get(0);
            final long lastSeen = app.getAccountDataLong(lastSeenKey);

            if (lastSeen < first.created_at.getTime()) {
                app.setAccountData(lastSeenKey, first.created_at.getTime());
            }
            if (wasRefresh) {
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
                    notifyDataSetChanged();
                    return true; //done

                } else {
                    reset();
                }
            }
        }

        if (responseCode == 0 || !TextUtils.isEmpty(nextCursor)) {
            keepGoing = true;
            Request nextRequest = new Request(mRequest);
                if (!TextUtils.isEmpty(nextCursor)){
                nextRequest.add("cursor",nextCursor);
            }
            nextHref = nextRequest.toUrl();
        }
        return super.onPostTaskExecute(newItems, nextHref, responseCode, keepGoing, wasRefresh);
    }

    @Override
    protected RemoteCollectionTask buildTask() {
        return new LoadActivitiesTask(mActivity.getApp(), this);
    }

    protected void onContentChanged() {
        executeRefreshTask();
    }
}
