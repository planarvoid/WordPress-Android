package com.soundcloud.android.presentation;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.java.optional.Optional;

import java.util.List;

public interface PromotedListItem extends StreamItem {

    String getAdUrn();

    boolean hasPromoter();

    Optional<String> getPromoterName();

    Optional<Urn> getPromoterUrn();

    Optional<String> getAvatarUrlTemplate();

    List<String> getPlayUrls();

    List<String> getImpressionUrls();

    List<String> getPromoterClickUrls();

    List<String> getClickUrls();
}
