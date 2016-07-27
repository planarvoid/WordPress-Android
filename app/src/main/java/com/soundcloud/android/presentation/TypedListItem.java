package com.soundcloud.android.presentation;

import com.soundcloud.android.api.model.Timestamped;

public interface TypedListItem extends ListItem, Timestamped {
    enum Kind {PLAYABLE, PROMOTED, NOTIFICATION, UPSELL}

    Kind getKind();
}
