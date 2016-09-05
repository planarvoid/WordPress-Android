package com.soundcloud.android.collection.recentlyplayed;

import com.soundcloud.android.R;
import com.soundcloud.android.collection.SimpleHeaderRenderer;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.res.Resources;

import javax.inject.Inject;

class RecentlyPlayedHeaderRenderer extends SimpleHeaderRenderer<RecentlyPlayedHeader> {

    private final Resources resources;

    @Inject
    RecentlyPlayedHeaderRenderer(Resources resources,
                                 PopupMenuWrapper.Factory popupMenuFactory) {
        super(popupMenuFactory);
        this.resources = resources;
    }

    @Override
    public String getTitle(RecentlyPlayedHeader header) {
        final int contextCount = header.contextCount();

        return resources.getQuantityString(R.plurals.collections_recently_played_header_items,
                                           contextCount,
                                           contextCount);
    }

    @Override
    public String getMenuActionText() {
        return resources.getString(R.string.collections_recently_played_clear_action);
    }
}
