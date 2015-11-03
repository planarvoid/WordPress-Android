package com.soundcloud.android.playback;

import com.soundcloud.java.collections.PropertySet;

public class VideoQueueItem extends PlayQueueItem {

    public VideoQueueItem(PropertySet metaData) {
      super.setMetaData(metaData);
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    @Override
    public Kind getKind() {
        return Kind.VIDEO;
    }
}
