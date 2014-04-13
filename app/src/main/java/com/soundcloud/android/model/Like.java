package com.soundcloud.android.model;

import com.soundcloud.android.storage.CollectionStorage;

import android.database.Cursor;


/**
 * This class would not exist were it not for API inconsistencies
 * (/e1/sounds vs /e1/likes).
 */
public class Like extends SoundAssociation {
    public Like() {
        super();
        associationType = CollectionStorage.CollectionItemTypes.LIKE;
    }

    public Like(Cursor cursor) {
        super(cursor);
        associationType = CollectionStorage.CollectionItemTypes.LIKE;
    }
}
