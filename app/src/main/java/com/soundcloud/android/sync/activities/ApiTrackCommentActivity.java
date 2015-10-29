package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.comments.ApiComment;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;

import java.util.Date;

public class ApiTrackCommentActivity implements TrackHolder, ApiEngagementActivity {

    private final ApiTrack track;
    private final ApiUser commenter;
    private final ApiComment comment;
    private final Date createdAt;

    @JsonCreator
    public ApiTrackCommentActivity(@JsonProperty("track") ApiTrack track,
                                   @JsonProperty("commenter") ApiUser commenter,
                                   @JsonProperty("comment") ApiComment comment,
                                   @JsonProperty("created_at") Date createdAt) {
        this.track = track;
        this.commenter = commenter;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    @Override
    public ApiTrack getTrack() {
        return track;
    }

    @Override
    public UserRecord getUser() {
        return commenter;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    public ApiComment getComment() {
        return comment;
    }

    @Override
    public Urn getTargetUrn() {
        return track.getUrn();
    }

    @Override
    public Urn getUserUrn() {
        return commenter.getUrn();
    }
}
