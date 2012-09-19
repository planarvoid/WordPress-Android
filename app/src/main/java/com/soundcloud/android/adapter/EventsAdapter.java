
package com.soundcloud.android.adapter;

import android.os.Parcelable;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.ActivityRow;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.TrackInfoBar;

import java.util.ArrayList;

public class EventsAdapter extends TracklistAdapter {
    private boolean mIsActivityFeed;

    public EventsAdapter(ScListActivity context, ArrayList<Parcelable> data, boolean isActivityFeed, Class<?> model) {
        super(context, data, model);
        mIsActivityFeed = isActivityFeed;
    }

    @Override
    public void setPlayingId(long currentTrackId, boolean isPlaying) {
        if (!isActivityFeed()) {
            super.setPlayingId(currentTrackId,isPlaying);
        }
    }

    @Override
    protected LazyRow createRow(int position) {
        return isActivityFeed() ? new ActivityRow(mContext, this) : new TrackInfoBar(mContext, this);
    }

    @Override
    public Track getTrackAt(int index) {
        return ((Activity) getItem(index)).getTrack();
    }

    public boolean isActivityFeed() {
        return mIsActivityFeed;
    }
}
