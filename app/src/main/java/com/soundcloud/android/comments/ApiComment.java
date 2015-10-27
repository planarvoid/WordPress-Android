package com.soundcloud.android.comments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.Urn;

import java.util.Date;

public class ApiComment {

    private final Urn urn;
    private final Urn trackUrn;
    private final long trackTime;
    private final String body;
    private final Date createdAt;

    @JsonCreator
    public ApiComment(@JsonProperty("urn") String urn,
                      @JsonProperty("track_urn") String trackUrn,
                      @JsonProperty("track_time") long trackTime,
                      @JsonProperty("body") String body,
                      @JsonProperty("created_at") Date createdAt) {
        this.urn = new Urn(urn);
        this.trackUrn = new Urn(trackUrn);
        this.trackTime = trackTime;
        this.body = body;
        this.createdAt = createdAt;
    }

    public Urn getUrn() {
        return urn;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    public String getBody() {
        return body;
    }

    public long getTrackTime() {
        return trackTime;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}
