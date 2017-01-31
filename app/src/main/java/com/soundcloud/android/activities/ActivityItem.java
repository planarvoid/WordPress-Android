package com.soundcloud.android.activities;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.Timestamped;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

@AutoValue
public abstract class ActivityItem implements Timestamped {
    abstract ActivityKind getKind();

    abstract String getUserName();

    abstract String getPlayableTitle();

    abstract Optional<Urn> getCommentedTrackUrn();

    abstract Urn getUrn();

    public static ActivityItem create(Date createdAt,
                                      ActivityKind kind,
                                      String userName,
                                      String playableTitle,
                                      Optional<Urn> commentedTrackUrn,
                                      Urn urn) {
        return new AutoValue_ActivityItem.Builder()
                .createdAt(createdAt)
                .kind(kind)
                .userName(userName)
                .playableTitle(playableTitle)
                .commentedTrackUrn(commentedTrackUrn)
                .urn(urn).build();
    }

    public static Builder builder() {
        return new AutoValue_ActivityItem.Builder().commentedTrackUrn(Optional.absent());
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder createdAt(Date date);

        public abstract Builder kind(ActivityKind kind);

        public abstract Builder userName(String userName);

        public abstract Builder playableTitle(String playableTitle);

        public abstract Builder commentedTrackUrn(Optional<Urn> commentedTrackUrn);

        public abstract Builder urn(Urn urn);

        public abstract ActivityItem build();
    }
}
