
package com.soundcloud.android.adapter;

import android.os.Parcelable;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.ActivityRow;
import com.soundcloud.android.view.IncomingRow;
import com.soundcloud.android.view.LazyRow;

import java.util.ArrayList;

public class EventsAdapter extends TracklistAdapter {
    private boolean mIsActivityFeed;

    public EventsAdapter(ScActivity context, ArrayList<Parcelable> data, boolean isActivityFeed, Class<?> model) {
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
        return isActivityFeed() ? new ActivityRow(mActivity, this) : new IncomingRow(mActivity, this);
    }

    @Override
    public Track getTrackAt(int index) {
        return ((Event) getItem(index)).getTrack();
    }

    public boolean isActivityFeed() {
        return mIsActivityFeed;
    }
}
