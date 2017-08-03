package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class StationMetadata {

    public abstract Urn urn();

    public abstract String title();

    public abstract String type();

    public abstract Optional<String> permalink();

    public abstract Optional<String> imageUrlTemplate();

    public static Builder builder() {
        return new AutoValue_StationMetadata.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder urn(Urn urn);

        public abstract Builder title(String title);

        public abstract Builder type(String type);

        public abstract Builder permalink(Optional<String> permalink);

        public abstract Builder imageUrlTemplate(Optional<String> imageUrlTemplate);

        public abstract StationMetadata build();
    }
}
