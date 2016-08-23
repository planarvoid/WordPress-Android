package com.soundcloud.android.collection.recentlyplayed;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.SimpleHeaderRenderer;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.res.Resources;
import android.support.annotation.Nullable;

@AutoFactory
class RecentlyPlayedHeaderRenderer extends SimpleHeaderRenderer<RecentlyPlayedHeader> {

    private final Resources resources;

    RecentlyPlayedHeaderRenderer(@Nullable MenuClickListener listener,
                                 @Provided Resources resources,
                                 @Provided PopupMenuWrapper.Factory popupMenuFactory) {
        super(listener, popupMenuFactory);
        this.resources = resources;
    }

    @Override
    public String getTitle(RecentlyPlayedHeader header) {
        final int contextCount = header.contextCount();

        return resources.getQuantityString(R.plurals.collections_recently_played_header,
                                           contextCount,
                                           contextCount);
    }

    @Override
    public String getMenuActionText() {
        return resources.getString(R.string.collections_recently_played_clear_action);
    }
}