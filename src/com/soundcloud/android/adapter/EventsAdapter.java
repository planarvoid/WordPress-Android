
package com.soundcloud.android.adapter;

import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.Events;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.view.EventsRow;
import com.soundcloud.android.view.LazyRow;

import android.database.Cursor;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;

public class EventsAdapter extends TracklistAdapter {

    public static final String TAG = "EventsAdapter";

    private Cursor mCursor;
    private boolean mDataValid;
    private ArrayList<Event> mLocalData;

  //private ChangeObserver mChangeObserver;

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
        return ((Event) getItem(index)).getTrack();
    }

    public int getLocalDataCount(){
        return mLocalData == null ? 0 : mLocalData.size();
    }


    @Override
    public int getCount() {
        if (mDataValid && mLocalData != null) {
            return mLocalData.size() + super.getCount();
        } else {
            return super.getCount();
        }
    }
    @Override
    public Object getItem(int position) {
        if (mDataValid && mLocalData != null) {
            if (position < mLocalData.size()){
                return mLocalData.get(position);
            } else
                return super.getItem(position - mLocalData.size());
        } else {
            return super.getItem(position);
        }
    }

    @Override
    public long getItemId(int position) {
        if (mDataValid && mLocalData != null) {
            if (position < mLocalData.size()){
                return mLocalData.get(position).id;
            } else {
                return super.getItemId(position - mLocalData.size());
            }
        } else {
            return super.getItemId(position);
        }
    }

    public String getNextCursor() {
        if (mLocalData.size() > 0){
            Log.i(TAG,"Adding extra " + (mLocalData.get(mLocalData.size()-1)).next_cursor);
            return (mLocalData.get(mLocalData.size()-1)).next_cursor;
        }
        return "";
}

    private void refreshCursor() {
        if (mCursor != null) {
            //mCursor.unregisterContentObserver(mChangeObserver);
            mCursor.close();
        }

        mCursor = mActivity.getContentResolver().query(Events.CONTENT_URI, null,
                Events.USER_ID + "='" + mActivity.getUserId() + "'", null,
                Events.CREATED_AT + " DESC");


        if (mCursor != null) {
            mDataValid = true;
            loadEvents(mCursor);

            //mChangeObserver = new ChangeObserver();
            //mCursor.registerContentObserver(mChangeObserver);
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


    private void loadEvents(Cursor cursor) {
        mLocalData = new ArrayList<Event>();
        if (cursor != null && !cursor.isClosed()) {
            while (cursor.moveToNext()) {
                Event e = new Event(cursor);
                e.track = SoundCloudDB.getInstance().resolveTrackById(mActivity.getContentResolver(), e.origin_id, mActivity.getUserId());
                mLocalData.add(e);
            }
        }

    }

    /*

    protected void onContentChanged() {
        if (mCursor != null && !mCursor.isClosed()) {
            mDataValid = mCursor.requery();
        }

        submenuIndex = -1;
        animateSubmenuIndex = -1;
        loadEvents(mCursor);
        notifyDataSetChanged();
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

*/


}
