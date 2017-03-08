package com.soundcloud.android.presentation;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.java.optional.Optional;

import java.util.List;

public interface PromotedListItem extends TypedListItem {

    Optional<PromotedProperties> promotedProperties();

    String getAdUrn();

    boolean hasPromoter();

    Optional<String> promoterName();

    Optional<Urn> promoterUrn();

    Optional<String> getAvatarUrlTemplate();

    List<String> playUrls();

    List<String> impressionUrls();

    List<String> promoterClickUrls();

    List<String> clickUrls();
}
