
package com.soundcloud.android.view;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;

import android.os.Parcelable;

public class EventsRow extends TracklistRow {

    public EventsRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);
    }

    @Override
    protected Track getTrackFromParcelable(Parcelable p) {
        return ((Event) p).getTrack();
    }

}
