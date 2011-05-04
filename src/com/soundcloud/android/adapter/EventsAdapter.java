
package com.soundcloud.android.adapter;

import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.Events;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.view.EventsRow;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.api.Request;

import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;

public class EventsAdapter extends TracklistAdapter {

    public static final String TAG = "EventsAdapter";

    private Cursor mCursor;
    private boolean mDataValid;
    private ChangeObserver mChangeObserver;

    public EventsAdapter(ScActivity context, ArrayList<Parcelable> data) {
        super(context, data);
        refreshCursor();
    }

    @Override
    protected LazyRow createRow() {
        return new EventsRow(mActivity, this);
    }

    @Override
    public Track getTrackAt(int index) {
        return ((Event) mData.get(index)).getTrack();
    }

    @Override
    public void addRequestExtra(Request request) {
        if (mData.size() > 0){
            Log.i(TAG,"Adding extra " + ((Event) mData.get(mData.size()-1)).next_cursor);
            request.add("cursor", ((Event) mData.get(mData.size()-1)).next_cursor);
        }
    }

    private void refreshCursor() {
        if (mCursor != null) {
            mCursor.unregisterContentObserver(mChangeObserver);
            mCursor.close();
        }

        mCursor = mActivity.getContentResolver().query(Events.CONTENT_URI, null,
                Events.USER_ID + "='" + mActivity.getUserId() + "'", null,
                Events.CREATED_AT + " DESC");

        mChangeObserver = new ChangeObserver();
        if (mCursor != null) {
            mDataValid = true;
            mCursor.registerContentObserver(mChangeObserver);
            loadEvents(mCursor);
        } else {
            mDataValid = false;
        }
    }


    @Override
    public void reset() {
        mPage = 1;
        submenuIndex = -1;
        animateSubmenuIndex = -1;
        refreshCursor();
    }

    protected void onContentChanged() {
        if (mCursor != null && !mCursor.isClosed()) {
            mDataValid = mCursor.requery();
        }

        submenuIndex = -1;
        animateSubmenuIndex = -1;
        loadEvents(mCursor);
        notifyDataSetChanged();
    }

    private void loadEvents(Cursor cursor) {
        mData = new ArrayList<Parcelable>();
        if (cursor != null && !cursor.isClosed()) {
            while (cursor.moveToNext()) {
                Event e = new Event(cursor);
                e.track = SoundCloudDB.getInstance().resolveTrackById(mActivity.getContentResolver(), e.origin_id, mActivity.getUserId());
                mData.add(e);
                //Log.i(TAG,"Added Track " + mData.size() + " " + ((Event) mData.get(mData.size()-1)).getTrack().id);
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
            onContentChanged();
        }
    }
}
