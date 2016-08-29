package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.R;
import com.soundcloud.android.collection.SimpleHeaderRenderer;
import com.soundcloud.android.view.menu.PopupMenuWrapper;

import android.content.res.Resources;

import javax.inject.Inject;

class PlayHistoryHeaderRenderer extends SimpleHeaderRenderer<PlayHistoryItemHeader> {

    private final Resources resources;

    @Inject
    PlayHistoryHeaderRenderer(Resources resources,
                              PopupMenuWrapper.Factory popupMenuFactory) {
        super(popupMenuFactory);
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
