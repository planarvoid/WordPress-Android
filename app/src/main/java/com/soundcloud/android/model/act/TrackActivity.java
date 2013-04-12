package com.soundcloud.android.model.act;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableHolder;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;

import android.database.Cursor;
import android.os.Parcel;

public class TrackActivity extends Activity implements PlayableHolder {
    @JsonProperty public Track track;

    // for deserialization
    public TrackActivity() {
        super();
    }

    public TrackActivity(Cursor c) {
        super(c);
        track = new Track(c);
    }

    public TrackActivity(Parcel in) {
        super(in);
        track = in.readParcelable(Track.class.getClassLoader());
    }

    @Override
    public Type getType() {
        return Type.TRACK;
    }

    @Override
    public Playable getPlayable() {
        return track;
    }

    @Override
    public User getUser() {
        return track.user;
    }

    @Override
    public ScResource getRefreshableResource() {
        return track;
    }

    @Override
    public boolean isIncomplete() {
        return track == null || track.isIncomplete();
    }
}
