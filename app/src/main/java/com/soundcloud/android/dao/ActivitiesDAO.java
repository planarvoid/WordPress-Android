package com.soundcloud.android.dao;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActivitiesDAO
{
    // TODO: doesn't belong here
    public static int clear(@Nullable Content content, ContentResolver resolver) {
        Content contentToDelete = Content.ME_ALL_ACTIVITIES;
        if (content != null) {
            contentToDelete = content;
        }
        if (!Activity.class.isAssignableFrom(contentToDelete.modelType)) {
            throw new IllegalArgumentException("specified content is not an activity");
        }
        // make sure to delete corresponding collection
        if (contentToDelete == Content.ME_ALL_ACTIVITIES) {
            for (Content c : Content.ACTIVITIES) {
                LocalCollectionDAO.deleteUri(c.uri, resolver);
            }
        } else {
            LocalCollectionDAO.deleteUri(contentToDelete.uri, resolver);
        }

        return resolver.delete(contentToDelete.uri, null, null);
    }

    public static @Nullable Activity getLastActivity(Content content, ContentResolver resolver) {
        Activity a = null;
        Cursor c = resolver.query(content.uri,
                    null,
                DBHelper.ActivityView.CONTENT_ID+" = ?",
                new String[] { String.valueOf(content.id) },
                    DBHelper.ActivityView.CREATED_AT + " ASC LIMIT 1");
        if (c != null && c.moveToFirst()){
            a = SoundCloudApplication.MODEL_MANAGER.getActivityFromCursor(c);
        }
        if (c != null) c.close();
        return a;
    }

    public static @Nullable Activity getFirstActivity(Content content, ContentResolver resolver) {
        Activity a = null;
        Cursor c = resolver.query(content.uri,
                null,
                DBHelper.ActivityView.CONTENT_ID+" = ?",
                new String[] { String.valueOf(content.id) },
                DBHelper.ActivityView.CREATED_AT + " DESC LIMIT 1");
        if (c != null && c.moveToFirst()) {
            a = SoundCloudApplication.MODEL_MANAGER.getActivityFromCursor(c);
        }
        if (c != null) c.close();
        return a;
    }

    public static Activities get(Content content, ContentResolver contentResolver) {
        return getSince(content, contentResolver, 0);
    }

    public static Activities getSince(Content content, ContentResolver resolver, long before)  {
        return getSince(content.uri, resolver, before);
    }

    public static Activities getSince(Uri contentUri, ContentResolver resolver, long since)  {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "Activities.getSince("+contentUri+", since="+since+")");

        Activities activities = new Activities();
        LocalCollection lc = LocalCollectionDAO.fromContentUri(contentUri, resolver, false);
        if (lc != null) {
            activities.future_href = lc.extra;
        }
        Cursor c;
        if (since > 0) {
            c = resolver.query(contentUri,
                    null,
                    DBHelper.ActivityView.CREATED_AT+"> ?",
                    new String[] { String.valueOf(since) },
                    null);
        } else {
            c = resolver.query(contentUri, null, null, null, null);
        }
        return SoundCloudApplication.MODEL_MANAGER.getActivitiesFromCursor(c);
    }

    public static Activities getBefore(Uri contentUri, ContentResolver resolver, long before)  {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "Activities.getBefore("+contentUri+", before="+before+")");
        Cursor c;
        if (before > 0) {
            c = resolver.query(contentUri,
                    null,
                    DBHelper.ActivityView.CREATED_AT+"< ?",
                    new String[] { String.valueOf(before) },
                    null);
        } else {
            c = resolver.query(contentUri, null, null, null, null);
        }

        return SoundCloudApplication.MODEL_MANAGER.getActivitiesFromCursor(c);
    }

    public static int getCountSince(ContentResolver contentResolver, long since, Content content){
        Cursor c = contentResolver.query(content.uri,
                    new String[]{"Count("+ BaseColumns._ID+") as unseen"},
                    DBHelper.ActivityView.CONTENT_ID + " = ? AND " + DBHelper.ActivityView.CREATED_AT + "> ?",
                    new String[]{String.valueOf(content.id), String.valueOf(since)},
                    null);
        return c != null && c.moveToFirst() ? c.getInt(c.getColumnIndex("unseen")) : 0;
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
}
