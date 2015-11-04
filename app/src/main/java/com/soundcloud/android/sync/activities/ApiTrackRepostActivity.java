package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;

import java.util.Date;

public class ApiTrackRepostActivity implements TrackHolder, ApiEngagementActivity {

    private final ApiTrack track;
    private final ApiUser reposter;
    private final Date createdAt;

    @JsonCreator
    public ApiTrackRepostActivity(@JsonProperty("track") ApiTrack track,
                                  @JsonProperty("reposter") ApiUser reposter,
                                  @JsonProperty("created_at") Date createdAt) {
        this.track = track;
        this.reposter = reposter;
        this.createdAt = createdAt;
    }

    @Override
    public ApiTrack getTrack() {
        return track;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public UserRecord getUser() {
        return reposter;
    }

    @Override
    public Urn getTargetUrn() {
        return track.getUrn();
    }

    @Override
    public Urn getUserUrn() {
        return reposter.getUrn();
    }
}
