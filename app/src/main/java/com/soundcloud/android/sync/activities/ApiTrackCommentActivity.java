package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.comments.ApiComment;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;

import java.util.Date;

public class ApiTrackCommentActivity implements TrackHolder, ApiEngagementActivity {

    private final Urn targetUrn;
    private final ApiTrack track;
    private final ApiComment comment;
    private final Date createdAt;

    @JsonCreator
    public ApiTrackCommentActivity(@JsonProperty("target_urn") String targetUrn,
                                   @JsonProperty("track") ApiTrack track,
                                   @JsonProperty("comment") ApiComment comment,
                                   @JsonProperty("created_at") Date createdAt) {
        this.targetUrn = new Urn(targetUrn);
        this.track = track;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    @Override
    public Urn getTargetUrn() {
        return targetUrn;
    }

    @Override
    public ApiTrack getTrack() {
        return track;
    }

    public ApiComment getComment() {
        return comment;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public UserRecord getUser() {
        return comment.getUser();
    }

    @Override
    public Urn getUserUrn() {
        return getUser().getUrn();
    }
}
