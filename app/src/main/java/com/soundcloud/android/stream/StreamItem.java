package com.soundcloud.android.stream;

import com.soundcloud.android.api.model.Timestamped;
import com.soundcloud.android.presentation.ListItem;

public interface StreamItem extends ListItem, Timestamped {
    enum Kind {PLAYABLE, PROMOTED, NOTIFICATION, UPSELL}

    Kind getKind();

    boolean isUpsellable();
}
