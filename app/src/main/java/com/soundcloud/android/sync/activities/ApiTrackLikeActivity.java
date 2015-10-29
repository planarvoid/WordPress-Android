package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.Urn;

import java.util.Date;

public class ApiTrackLikeActivity implements TrackHolder, ApiEngagementActivity {

    private final ApiTrack track;
    private final ApiUser user;
    private final Date createdAt;

    @JsonCreator
    public ApiTrackLikeActivity(@JsonProperty("track") ApiTrack track,
                                @JsonProperty("user") ApiUser user,
                                @JsonProperty("created_at") Date createdAt) {
        this.track = track;
        this.user = user;
        this.createdAt = createdAt;
    }

    @Override
    public ApiTrack getTrack() {
        return track;
    }

    @Override
    public ApiUser getUser() {
        return user;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public Urn getTargetUrn() {
        return track.getUrn();
    }

    @Override
    public Urn getUserUrn() {
        return user.getUrn();
    }
}
