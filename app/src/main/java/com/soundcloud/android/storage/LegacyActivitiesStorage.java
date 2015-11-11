package com.soundcloud.android.storage;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.sync.timeline.TimelineStorage;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import android.content.Context;
import android.net.Uri;

import javax.inject.Inject;
import java.util.List;

@Deprecated
public class LegacyActivitiesStorage implements TimelineStorage {
    private SyncStateManager syncStateManager;
    private ActivityDAO activitiesDAO;

    public LegacyActivitiesStorage() {
        this(SoundCloudApplication.instance);
    }

    public LegacyActivitiesStorage(Context context) {
        this(new SyncStateManager(context), new ActivityDAO(context.getContentResolver()));
    }

    @Inject
    public LegacyActivitiesStorage(SyncStateManager syncStateManager,
                                   ActivityDAO activitiesDAO) {
        this.syncStateManager = syncStateManager;
        this.activitiesDAO = activitiesDAO;
    }

    @Override
    public Observable<PropertySet> timelineItems(int limit) {
        // this is unused, we'll delete this class soon
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<PropertySet> timelineItemsBefore(long timestamp, int limit) {
        // this is unused, we'll delete this class soon
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PropertySet> timelineItemsSince(long timestamp, int limit) {
        final Activities collectionSince = getCollectionSince(Content.ME_ACTIVITIES.uri, timestamp, limit);
        return Lists.transform(collectionSince.collection, PropertySets.toPropertySet());
    }

    @Deprecated
    private Activities getCollectionSince(final Uri contentUri, final long since, final int limit)  {
        Activities activities = new Activities();
        LocalCollection lc = syncStateManager.fromContent(contentUri);
        activities.future_href = lc.extra;

        BaseDAO.QueryBuilder query = activitiesDAO.buildQuery(contentUri);
        if (since > 0) {
            query.where(TableColumns.ActivityView.CREATED_AT + "> ?", String.valueOf(since));
        }
        if (limit > 0) {
            query.limit(limit);
        }

        final List<Activity> result = query.queryAll();
        if (result.isEmpty()) {
            return Activities.EMPTY;
        } else {
            activities.collection = result;
            return activities;
        }
    }

    @Deprecated
    public Activities getCollectionSince(final Uri contentUri, final long since)  {
        return getCollectionSince(contentUri, since, 0);
    }

    @Deprecated
    @Nullable
    public Activity getOldestActivity(final Content content) {
        return activitiesDAO.buildQuery(content.uri)
                .where(TableColumns.ActivityView.CONTENT_ID + " = ?", String.valueOf(content.id))
                .order(TableColumns.ActivityView.CREATED_AT + " ASC")
                .first();
    }

    @Deprecated
    @Nullable
    public Activity getLatestActivity(final Content content) {
        return activitiesDAO.buildQuery(content.uri)
                .where(TableColumns.ActivityView.CONTENT_ID + " = ?", String.valueOf(content.id))
                .order(TableColumns.ActivityView.CREATED_AT + " DESC")
                .first();
    }

    @Deprecated
    public Activities getCollectionBefore(final Uri contentUri, final long before)  {
        BaseDAO.QueryBuilder query = activitiesDAO.buildQuery(contentUri);
        if (before > 0) {
            query.where(TableColumns.ActivityView.CREATED_AT + "< ?", String.valueOf(before));
        }

        Activities activities = new Activities();
        final List<Activity> result = query.queryAll();
        if (result.isEmpty()) {
            return Activities.EMPTY;
        } else {
            activities.collection = result;
            return activities;
        }
    }

    @Deprecated
    public int insert(Content content, Activities activities) {
        return activitiesDAO.insert(content, activities);
    }

}
