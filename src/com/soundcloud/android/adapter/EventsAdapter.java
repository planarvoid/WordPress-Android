
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.provider.DatabaseHelper.Content;
import com.soundcloud.android.provider.DatabaseHelper.Events;
import com.soundcloud.android.task.DashboardQueryTask;
import com.soundcloud.android.task.QueryTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.view.EventsRow;
import com.soundcloud.android.view.LazyRow;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class EventsAdapter extends TracklistAdapter {

    public static final String TAG = "EventsAdapter";
    public String nextCursor;

    private boolean mExclusive;
    private QueryTask mQueryTask;

    private ChangeObserver  mChangeObserver;

    public EventsAdapter(ScActivity context, ArrayList<Parcelable> data, boolean isExclusive, Class<?> model) {
        super(context, data, model);
        mExclusive = isExclusive;
        //mChangeObserver = new ChangeObserver();
        //addLocalEvents();
    }

    @Override
    protected LazyRow createRow() {
        return new EventsRow(mActivity, this);
    }

    @Override
    public Track getTrackAt(int index) {
        return ((Event) getItem(index)).getTrack();
    }

    /*
    @Override
    public boolean isQuerying(){
        return !CloudUtils.isTaskFinished(mQueryTask);
    }*/

    @Override
    public void onPostQueryExecute() {
        mQueryTask = null;
        super.onPostQueryExecute();
        if (mData.size() > 0) {
            if (mExclusive){
                //mActivity.getSoundCloudApplication().requestRecentExclusive(this);
            } else {
                //mActivity.getSoundCloudApplication().requestRecentIncoming(this);
            }

            if (TextUtils.isEmpty(nextCursor)){
                // we must be at the end of the dashboard list, dont allow appending
                mWrapper.keepOnAppending.set(false);
            }
        }
        mActivity.getContentResolver().registerContentObserver(Events.CONTENT_URI, true, mChangeObserver);
        notifyDataSetChanged();
        submenuIndex = -1;
        animateSubmenuIndex = -1;

    }


    private void addLocalEvents() {

        // pre SDK 8 doesn't allow auto syncing, so with our current list loading UI
        // it is easier to just enabling remote pulling only for now
        if (Build.VERSION.SDK_INT < 8) return;

        mActivity.getContentResolver().unregisterContentObserver(mChangeObserver);

        mData = new ArrayList<Parcelable>();
        if (CloudUtils.isTaskFinished(mQueryTask)){
            mQueryTask = new DashboardQueryTask(mActivity.getSoundCloudApplication());
            mQueryTask.setAdapter(this);
            mQueryTask.setQuery((mExclusive ? Content.EXCLUSIVE_TRACKS : Content.INCOMING_TRACKS),
                    null, Events.ALIAS_USER_ID + "= ? AND " + Events.ALIAS_EXCLUSIVE + " = ?",
                    new String[] {
                            Long.toString(mActivity.getUserId()), (mExclusive ? "1" : "0")
                    }, Events.ALIAS_ID + " DESC");
            mQueryTask.execute();
        } else
            mQueryTask.setAdapter(this);

    }

    @Override
    public void refresh(boolean userRefresh) {
        mData.clear();
        reset();
    }

    @Override
    public void reset() {
        mPage = 1;
        submenuIndex = -1;
        animateSubmenuIndex = -1;
        nextCursor = "";
        //addLocalEvents();
    }

    public void onNextEventsParam(String nextEventsHref) {
        List<NameValuePair> params = URLEncodedUtils.parse(URI.create(nextEventsHref),"UTF-8");
        for (NameValuePair param : params){
            if (param.getName().equalsIgnoreCase("cursor")) nextCursor = param.getValue();
        }
    }

    public void onNextEventsCursor(String mNextCursor) {
        nextCursor = mNextCursor;
    }

    protected void onContentChanged() {
        addLocalEvents();
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
            onContentChanged();
        }
    }
}
