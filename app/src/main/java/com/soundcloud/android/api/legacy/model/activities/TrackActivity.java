package com.soundcloud.android.api.legacy.model.activities;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.api.legacy.model.behavior.Refreshable;
import com.soundcloud.android.storage.TableColumns;

import android.database.Cursor;

public abstract class TrackActivity extends Activity implements PlayableHolder {
    @JsonProperty public PublicApiTrack track;

    // for deserialization
    public TrackActivity() {
        super();
    }

    public TrackActivity(Cursor c) {
        super(c);
        track = SoundCloudApplication.sModelManager.getCachedTrackFromCursor(c, TableColumns.ActivityView.SOUND_ID);
    }

    @Override
    public Playable getPlayable() {
        return track;
    }

    @Override
    public PublicApiUser getUser() {
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
