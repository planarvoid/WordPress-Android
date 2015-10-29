package com.soundcloud.android.comments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;

import java.util.Date;

@AutoValue
public abstract class ApiComment implements CommentRecord {

    @JsonCreator
    public static ApiComment create(@JsonProperty("urn") String urn,
                                    @JsonProperty("track_urn") String trackUrn,
                                    @JsonProperty("track_time") long trackTime,
                                    @JsonProperty("body") String body,
                                    @JsonProperty("created_at") Date createdAt,
                                    @JsonProperty("commenter") ApiUser commenter) {
        return builder()
                .urn(new Urn(urn))
                .trackUrn(new Urn(trackUrn))
                .trackTime(trackTime)
                .body(body)
                .createdAt(createdAt)
                .user(commenter)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_ApiComment.Builder();
    }

    @Override
    public abstract Urn getUrn();

    @Override
    public abstract Urn getTrackUrn();

    @Override
    public abstract String getBody();

    @Override
    public abstract long getTrackTime();

    @Override
    public abstract Date getCreatedAt();

    @Override
    public abstract ApiUser getUser();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder urn(Urn urn);

        public abstract Builder trackUrn(Urn urn);

        public abstract Builder body(String body);

        public abstract Builder trackTime(long trackTime);

        public abstract Builder createdAt(Date createdAt);

        public abstract Builder user(ApiUser commenter);

        public abstract ApiComment build();

    }
}
