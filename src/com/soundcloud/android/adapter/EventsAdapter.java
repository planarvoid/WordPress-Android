
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.EventsRow;
import com.soundcloud.android.view.LazyRow;

import android.os.Parcelable;

import java.util.ArrayList;

public class EventsAdapter extends TracklistAdapter {

    public static final String TAG = "EventsAdapter";
    private boolean mExclusive;

    public EventsAdapter(ScActivity context, ArrayList<Parcelable> data, boolean isExclusive, Class<?> model) {
        super(context, data, model);
        mExclusive = isExclusive;
    }

    public boolean isExclusive(){
        return mExclusive;
    }

    @Override
    protected LazyRow createRow() {
        return new EventsRow(mActivity, this);
    }

    @Override
    public Track getTrackAt(int index) {
        return ((Event) getItem(index)).getTrack();
    }



}
