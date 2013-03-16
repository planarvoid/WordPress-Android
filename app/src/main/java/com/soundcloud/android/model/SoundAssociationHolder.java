package com.soundcloud.android.model;

import com.soundcloud.android.dao.SoundAssociationsDAO;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SoundAssociationHolder extends CollectionHolder<SoundAssociation> {

    // needed for jackson
    public SoundAssociationHolder() {}

    public SoundAssociationHolder(List<SoundAssociation> collection) {
        super(collection);
    }

    public int insert(ContentResolver resolver) {
        return SoundAssociationsDAO.insert(this, resolver);
    }
}
