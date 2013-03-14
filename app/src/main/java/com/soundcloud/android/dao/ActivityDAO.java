package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActivityDAO extends BaseDAO<Activity> {

    public ActivityDAO(ContentResolver resolver) {
        super(resolver);
    }

    @Override
    public Content getContent() {
        return Content.ME_ALL_ACTIVITIES;
    }


    public static int insert(Content content, ContentResolver resolver, Activities activities) {
        Set<ScResource> models = new HashSet<ScResource>();
        for (Activity a : activities) {
            models.addAll(a.getDependentModels());
        }

        Map<Uri, List<ContentValues>> values = new HashMap<Uri, List<ContentValues>>();
        for (ScResource m : models) {
            final Uri uri = m.getBulkInsertUri();
            if (values.get(uri) == null) {
                values.put(uri, new ArrayList<ContentValues>());
            }
            values.get(uri).add(m.buildContentValues());
        }

        for (Map.Entry<Uri, List<ContentValues>> entry : values.entrySet()) {
            resolver.bulkInsert(entry.getKey(), entry.getValue().toArray(new ContentValues[entry.getValue().size()]));
        }

        return resolver.bulkInsert(content.uri, activities.buildContentValues(content.id));
    }

    public Activities queryAll() {
        Cursor c = mResolver.query(getContent().uri, null, null, null, null);
        return SoundCloudApplication.MODEL_MANAGER.getActivitiesFromCursor(c);
    }
}
