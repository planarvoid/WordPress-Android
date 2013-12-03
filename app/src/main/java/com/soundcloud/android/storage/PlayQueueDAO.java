package com.soundcloud.android.storage;

import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.storage.provider.Content;

import android.content.ContentResolver;

/**
 * Table object for storing playqueue items. {@link com.soundcloud.android.storage.provider.DBHelper.PlayQueue}
 */
/* package */ class PlayQueueDAO extends BaseDAO<PlayQueueItem> {
    public PlayQueueDAO(ContentResolver contentResolver) {
        super(contentResolver);
    }

    @Override
    public Content getContent() {
        return Content.PLAY_QUEUE;
    }
}
