package com.soundcloud.android.sync.activities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.comments.ApiComment;
import com.soundcloud.android.model.TrackHolder;
import com.soundcloud.android.model.Urn;

import java.util.Date;

public class ApiUserMentionActivity implements TrackHolder, ApiEngagementActivity {

    private final Urn commentUrn;
    private final ApiTrack track;
    private final ApiComment comment;
    private final ApiUser user;
    private final Date createdAt;

    @JsonCreator
    public ApiUserMentionActivity(@JsonProperty("target_urn") Urn commentUrn,
                                  @JsonProperty("track") ApiTrack track,
                                  @JsonProperty("comment") ApiComment comment,
                                  @JsonProperty("user") ApiUser user,
                                  @JsonProperty("created_at") Date createdAt) {
        this.commentUrn = commentUrn;
        this.track = track;
        this.comment = comment;
        this.user = user;
        this.createdAt = createdAt;
    }

    public Urn getCommentUrn() {
        return commentUrn;
    }

    public ApiComment getComment() {
        return comment;
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
