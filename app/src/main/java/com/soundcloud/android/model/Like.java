package com.soundcloud.android.model;

import com.soundcloud.android.provider.ScContentProvider;

import android.database.Cursor;


/**
 * This class would not exist were it not for API inconsistencies
 * (/e1/sounds vs /e1/likes).
 */
public class Like extends SoundAssociation {
    public Like() {
        super();
        associationType = ScContentProvider.CollectionItemTypes.LIKE;
    }

    public Like(Cursor cursor) {
        super(cursor);
        associationType = ScContentProvider.CollectionItemTypes.LIKE;
    }
}
