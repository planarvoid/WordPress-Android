
package com.soundcloud.android.view;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;

import android.os.Parcelable;

public class IncomingRow extends TracklistRow {
    public IncomingRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);
    }

    @Override
    protected Track getTrackFromParcelable(Parcelable p) {
        return ((Event) p).track;
    }

    @Override
    protected long getTrackTime(Parcelable p) {
        return ((Event)p).created_at.getTime();
    }
}
