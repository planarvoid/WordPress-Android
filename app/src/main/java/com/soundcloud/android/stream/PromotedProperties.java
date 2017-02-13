package com.soundcloud.android.stream;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
public abstract class PromotedProperties {
    public abstract String adUrn();

    public abstract List<String> trackClickedUrls();

    public abstract List<String> trackImpressionUrls();

    public abstract List<String> trackPlayedUrls();

    public abstract List<String> promoterClickedUrls();

    public abstract Optional<Urn> promoterUrn();

    public abstract Optional<String> promoterName();

    public static PromotedProperties create(String adUrn,
                                            List<String> trackClickedUrls,
                                            List<String> trackImpressionUrls,
                                            List<String> trackPlayedUrls,
                                            List<String> promoterClickedUrls,
                                            Optional<Urn> promoterUrn,
                                            Optional<String> promoterName) {
        return new AutoValue_PromotedProperties(adUrn, trackClickedUrls, trackImpressionUrls, trackPlayedUrls, promoterClickedUrls, promoterUrn, promoterName);
    }
}
