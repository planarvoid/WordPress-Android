
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
    private boolean mIsActivityFeed;

    public EventsAdapter(ScActivity context, ArrayList<Parcelable> data, boolean isActivityFeed, Class<?> model) {
        super(context, data, model);
        mIsActivityFeed = isActivityFeed;
    }

    @Override
    public void setPlayingId(long currentTrackId, boolean isPlaying) {
        if (!mIsActivityFeed) {
            super.setPlayingId(currentTrackId,isPlaying);
        }
    }

    @Override
    protected LazyRow createRow(int position) {
        return mIsActivityFeed ? new NewsRow(mActivity, this) : new IncomingRow(mActivity, this);
    }

    @Override
    public Track getTrackAt(int index) {
        return ((Event) getItem(index)).track;
    }
}
