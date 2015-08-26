package com.soundcloud.android.stream;

import com.soundcloud.android.presentation.ListItem;

public interface StreamItem extends ListItem {
    enum Kind {PLAYABLE, PROMOTED, NOTIFICATION}

    Kind getKind();
}
