package com.soundcloud.android.stream;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

@AutoValue
public abstract class StreamEntity implements UrnHolder {

    public abstract Date createdAt();

    public abstract Optional<String> reposter();

    public abstract Optional<Urn> reposterUrn();

    public abstract Optional<String> avatarUrl();

    public abstract Optional<PromotedProperties> promotedProperties();

    public static StreamEntity.Builder builder(Urn urn, Date createdAt, Optional<String> reposter, Optional<Urn> reposterUrn, Optional<String> avatarUrl) {
        return new AutoValue_StreamEntity.Builder().urn(urn).createdAt(createdAt).reposter(reposter).reposterUrn(reposterUrn).avatarUrl(avatarUrl).promotedProperties(Optional.absent());
    }

    public boolean isPromoted() {
        return promotedProperties().isPresent();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        abstract Builder urn(Urn urn);

        abstract Builder createdAt(Date createdAt);

        abstract Builder reposter(Optional<String> reposter);

        abstract Builder reposterUrn(Optional<Urn> reposterUrn);

        abstract Builder avatarUrl(Optional<String> avatarUrl);

        public abstract Builder promotedProperties(Optional<PromotedProperties> promotedProperties);

        public abstract StreamEntity build();
    }
}
