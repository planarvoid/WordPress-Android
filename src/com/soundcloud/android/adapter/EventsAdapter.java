
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.IncomingRow;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.NewsRow;

import android.os.Parcelable;

import java.util.ArrayList;

public class EventsAdapter extends TracklistAdapter {

    public static final String TAG = "EventsAdapter";
    private boolean mNews;

    public EventsAdapter(ScActivity context, ArrayList<Parcelable> data, boolean isNews, Class<?> model) {
        super(context, data, model);
        mNews = isNews;
    }

    public boolean isNews(){
        return mNews;
    }

    @Override
    public void setPlayingId(long currentTrackId, boolean isPlaying) {
        if (mNews) return;
        super.setPlayingId(currentTrackId,isPlaying);
    }

    @Override
    protected LazyRow createRow(int position) {
        return mNews ? new NewsRow(mActivity, this) : new IncomingRow(mActivity, this);
    }

    @Override
    public Track getTrackAt(int index) {
        return ((Event) getItem(index)).track;
    }



}
