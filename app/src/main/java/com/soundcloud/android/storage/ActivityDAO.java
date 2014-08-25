package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ErrorUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Table object for activity model. Do not use outside this package; use {@link ActivitiesStorage} instead.
 */
/* package */ class ActivityDAO extends BaseDAO<Activity> {

    @Inject
    public ActivityDAO(ContentResolver resolver) {
        super(resolver);
    }

    @Override
    public Content getContent() {
        return Content.ME_ALL_ACTIVITIES;
    }

    public int insert(Content content, Activities activities) {
        Set<PublicApiResource> models = new HashSet<PublicApiResource>();
        for (Activity a : activities) {
            try {
                models.addAll(a.getDependentModels());
            } catch (RuntimeException e) {
                // dirty hack by Matthias purely for logging, and I will remove it again, promised.
                // https://github.com/soundcloud/SoundCloud-Android/issues/2099
                final AccountOperations accountOps = SoundCloudApplication.instance.getAccountOperations();
                final Exception exception = new Exception("Failed dependency lookup for Activity " + a + "; content="
                        + content + "; token=" + accountOps.getSoundCloudToken() + "; next_href=" + activities.next_href);
                ErrorUtils.handleSilentException(exception);
                throw e;
            }
        }

        Map<Uri, List<ContentValues>> values = new HashMap<Uri, List<ContentValues>>();
        for (PublicApiResource m : models) {
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

    @Override protected  Activity objFromCursor(Cursor cursor) {
        return Activity.Type.fromString(cursor.getString(cursor.getColumnIndex(TableColumns.Activities.TYPE))).fromCursor(cursor);
    }

}
