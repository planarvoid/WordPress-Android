
package com.soundcloud.android.view;

import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.Track;

import android.content.Context;
import android.os.Parcelable;

public class IncomingRow extends TracklistRow {
    public IncomingRow(Context _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);
    }

    @Override
    protected Track getTrackFromParcelable(Parcelable p) {
        return ((Activity) p).getTrack();
    }
}
