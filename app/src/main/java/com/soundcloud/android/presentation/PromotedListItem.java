package com.soundcloud.android.presentation;

import com.soundcloud.android.api.model.Timestamped;
import com.soundcloud.android.stream.PromotedProperties;
import com.soundcloud.java.optional.Optional;

public interface PromotedListItem extends ListItem, Timestamped {

    Optional<PromotedProperties> promotedProperties();
}
