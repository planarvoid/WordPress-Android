package com.soundcloud.android.dao;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.service.sync.SyncStateManager;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

public class ActivitiesStorage {
    private SyncStateManager mSyncStateManager;
    private ActivityDAO mActivitiesDAO;
    private final ContentResolver mResolver;

    public ActivitiesStorage() {
        mResolver = SoundCloudApplication.instance.getContentResolver();
        mSyncStateManager = new SyncStateManager();
        mActivitiesDAO = new ActivityDAO(mResolver);
    }

    public Activities getSince(Uri contentUri, long since)  {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "Activities.getSince("+contentUri+", since="+since+")");

        Activities activities = new Activities();
        LocalCollection lc = mSyncStateManager.fromContent(contentUri);
        activities.future_href = lc.extra;

        BaseDAO.QueryBuilder query = mActivitiesDAO.buildQuery(contentUri);
        if (since > 0) {
            query.where(DBHelper.ActivityView.CREATED_AT + "> ?", String.valueOf(since));
        }

        activities.collection = query.queryAll();

        return activities;
    }

    public Activities getSince(Content content, long before)  {
        return getSince(content.uri, before);
    }

    public Activities get(Content content) {
        return getSince(content, 0);
    }


    public @Nullable Activity getLastActivity(Content content) {
        return mActivitiesDAO.buildQuery(content.uri)
                .where(DBHelper.ActivityView.CONTENT_ID + " = ?", String.valueOf(content.id))
                .order(DBHelper.ActivityView.CREATED_AT + " ASC")
                .first();
    }

    public @Nullable Activity getFirstActivity(Content content) {
        return mActivitiesDAO.buildQuery(content.uri)
                .where(DBHelper.ActivityView.CONTENT_ID + " = ?", String.valueOf(content.id))
                .order(DBHelper.ActivityView.CREATED_AT + " DESC")
                .first();
    }

    public Activities getBefore(Uri contentUri, long before)  {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "Activities.getBefore("+contentUri+", before="+before+")");

        BaseDAO.QueryBuilder query = mActivitiesDAO.buildQuery(contentUri);
        if (before > 0) {
            query.where(DBHelper.ActivityView.CREATED_AT + "< ?", String.valueOf(before));
        }

        Activities activities = new Activities();
        activities.collection = query.queryAll();

        return activities;
    }

    public int getCountSince(long since, Content content) {
        String selection = DBHelper.ActivityView.CONTENT_ID + " = ? AND " + DBHelper.ActivityView.CREATED_AT + "> ?";
        return mActivitiesDAO.count(selection, String.valueOf(content.id), String.valueOf(since));
    }

    public int clear(@Nullable Content content) {
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
                mSyncStateManager.delete(c);
            }
        } else {
            mSyncStateManager.delete(contentToDelete);
        }
        return mResolver.delete(contentToDelete.uri, null, null);
    }

    public int insert(Content content, Activities activities) {
        return mActivitiesDAO.insert(content, activities);
    }

}
