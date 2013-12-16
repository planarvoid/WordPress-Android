package com.soundcloud.android.model.activities;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.model.behavior.Refreshable;
import com.soundcloud.android.storage.provider.DBHelper;

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
        track = SoundCloudApplication.sModelManager.getCachedTrackFromCursor(c, DBHelper.ActivityView.SOUND_ID);
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
    public void cacheDependencies() {
        this.track = SoundCloudApplication.sModelManager.cache(track);
    }

    @Override
    public Refreshable getRefreshableResource() {
        return track;
    }

    @Override
    public boolean isIncomplete() {
        return track == null || track.isIncomplete();
    }
}
