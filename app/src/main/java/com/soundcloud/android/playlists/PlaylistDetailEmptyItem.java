package com.soundcloud.android.playlists;

import com.soundcloud.android.view.EmptyStatus;

class PlaylistDetailEmptyItem extends PlaylistDetailItem {

    private final EmptyStatus emptyStatus;
    private final boolean isOwner;

    PlaylistDetailEmptyItem(EmptyStatus emptyStatus, boolean isOwner) {
        super(Kind.EmptyItem);
        this.emptyStatus = emptyStatus;
        this.isOwner = isOwner;
    }


    public EmptyStatus getEmptyStatus() {
        return emptyStatus;
    }

    public boolean isOwner() {
        return isOwner;
    }
}
