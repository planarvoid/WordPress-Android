package com.soundcloud.android.stream;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

@AutoValue
public abstract class StreamEntity implements UrnHolder {

    public abstract Date createdAt();

    public abstract Optional<String> avatarUrlTemplate();

    public abstract Optional<RepostedProperties> repostedProperties();

    public abstract Optional<PromotedProperties> promotedProperties();

    public static StreamEntity.Builder builder(Urn urn, Date createdAt) {
        return new AutoValue_StreamEntity.Builder().urn(urn).createdAt(createdAt).avatarUrlTemplate(Optional.absent()).promotedProperties(Optional.absent()).repostedProperties(Optional.absent());
    }

    public boolean isPromoted() {
        return promotedProperties().isPresent();
    }

    public boolean isReposted() {
        return repostedProperties().isPresent();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        abstract Builder urn(Urn urn);

        abstract Builder createdAt(Date createdAt);

        abstract Builder avatarUrlTemplate(Optional<String> avatarUrl);

        abstract Builder repostedProperties(Optional<RepostedProperties> repostedProperties);

        public Builder repostedProperties(RepostedProperties repostedProperties) {
            return repostedProperties(Optional.of(repostedProperties));
        }

        abstract Builder promotedProperties(Optional<PromotedProperties> promotedProperties);

        public Builder promotedProperties(PromotedProperties promotedProperties) {
            return promotedProperties(Optional.of(promotedProperties));
        }

        public abstract StreamEntity build();
    }
}
