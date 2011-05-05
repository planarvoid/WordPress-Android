
package com.soundcloud.android.adapter;

import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.Events;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.view.EventsRow;
import com.soundcloud.android.view.LazyRow;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.database.Cursor;
import android.os.Parcelable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class EventsAdapter extends TracklistAdapter {

    public static final String TAG = "EventsAdapter";
    public String nextCursor;

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


    private void refreshCursor() {
        mData = new ArrayList<Parcelable>();
        Cursor cursor = mActivity.getContentResolver().query(Events.CONTENT_URI, null,
                Events.USER_ID + "='" + mActivity.getUserId() + "'", null,
                Events.CREATED_AT + " DESC");

        if (cursor != null && !cursor.isClosed()) {
            Event e = null;
            while (cursor.moveToNext()) {
                e = new Event(cursor);
                e.track = SoundCloudDB.getInstance().resolveTrackById(
                        mActivity.getContentResolver(), e.origin_id, mActivity.getUserId());
                mData.add(e);
            }
            nextCursor = e != null ? e.next_cursor : "";
            cursor.close();
        }
    }


    @Override
    public void reset() {
        nextCursor = "";
        mPage = 1;
        submenuIndex = -1;
        animateSubmenuIndex = -1;
        refreshCursor();
    }

    public void onNextEventsParam(String nextEventsHref) {
        List<NameValuePair> params = URLEncodedUtils.parse(URI.create(nextEventsHref),"UTF-8");
        for (NameValuePair param : params){
            if (param.getName().equalsIgnoreCase("cursor")) nextCursor = param.getValue();
        }
    }
}
