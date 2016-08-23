package com.soundcloud.android.collection.playhistory;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.collection.SimpleHeaderRenderer;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.res.Resources;
import android.support.annotation.Nullable;

@AutoFactory
class PlayHistoryHeaderRenderer extends SimpleHeaderRenderer<PlayHistoryItemHeader> {

    private final Resources resources;

    PlayHistoryHeaderRenderer(@Nullable MenuClickListener listener,
                              @Provided Resources resources,
                              @Provided PopupMenuWrapper.Factory popupMenuFactory) {
        super(listener, popupMenuFactory);
        this.resources = resources;
    }

    @Override
    public String getTitle(PlayHistoryItemHeader item) {
        final int trackCount = item.trackCount();
        return resources.getQuantityString(R.plurals.number_of_sounds, trackCount, trackCount);
    }

    @Override
    public String getMenuActionText() {
        return resources.getString(R.string.collections_play_history_clear_action);
    }

}
