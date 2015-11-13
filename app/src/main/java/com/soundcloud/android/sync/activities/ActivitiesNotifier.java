package com.soundcloud.android.sync.activities;

import com.soundcloud.android.Actions;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.activities.ActivityKind;
import com.soundcloud.android.activities.ActivityProperty;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncConfig;
import com.soundcloud.android.sync.timeline.TimelineStorage;
import com.soundcloud.android.utils.PropertySetComparator;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import org.jetbrains.annotations.Nullable;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ActivitiesNotifier {

    private final TimelineStorage activitiesStorage;
    private final ContentStats contentStats;
    private final ImageOperations imageOperations;

    public ActivitiesNotifier(TimelineStorage activitiesStorage,
                              ContentStats contentStats,
                              ImageOperations imageOperations) {
        this.activitiesStorage = activitiesStorage;
        this.contentStats = contentStats;
        this.imageOperations = imageOperations;
    }

    public void notifyUnseenItems(Context context) {
        final long lastOwnSeen = contentStats.getLastSeen(Content.ME_ACTIVITIES);
        List<PropertySet> activities = activitiesStorage.timelineItemsSince(lastOwnSeen, Integer.MAX_VALUE);
        if (!activities.isEmpty()) {
            notifyNewActivities(context, activities);
        }
    }

    private void notifyNewActivities(Context context, List<PropertySet> activities) {
        final List<PropertySet> likes = getLikeNotifications(context, activities);
        final List<PropertySet> comments = getCommentNotifications(context, activities);
        final List<PropertySet> reposts = getRepostNotifications(context, activities);
        final List<PropertySet> followers = getFollowersNotifications(context, activities);
        final List<PropertySet> activitiesToNotify = new LinkedList<>();
        activitiesToNotify.addAll(likes);
        activitiesToNotify.addAll(comments);
        activitiesToNotify.addAll(reposts);
        activitiesToNotify.addAll(followers);
        Collections.sort(activitiesToNotify, new PropertySetComparator<>(ActivityProperty.DATE, PropertySetComparator.DESC));

        if (newNotificationsAvailable(activitiesToNotify)) {
            final NotificationMessage msg = new NotificationMessage
                    .Builder(context.getResources())
                    .setLikes(likes)
                    .setComments(comments)
                    .setReposts(reposts)
                    .setFollowers(followers)
                    .build();
            showDashboardNotification(context, msg.ticker, msg.title, msg.message,
                    createNotificationIntent(Actions.ACTIVITY),
                    NotificationConstants.DASHBOARD_NOTIFY_ACTIVITIES_ID,
                    activitiesToNotify.get(0).get(ActivityProperty.USER_URN));

            contentStats.setLastNotifiedItem(Content.ME_ACTIVITIES,
                    getDateOfNewestItem(activitiesToNotify).getTime());
        }
    }

    private boolean newNotificationsAvailable(List<PropertySet> activitiesToNotify) {
        if (activitiesToNotify.isEmpty()) {
            return false;
        }
        final Date newestActivityDate = getDateOfNewestItem(activitiesToNotify);
        return newestActivityDate.getTime() > contentStats.getLastNotifiedItem(Content.ME_ACTIVITIES);
    }

    private Date getDateOfNewestItem(List<PropertySet> activitiesToNotify) {
        return activitiesToNotify.get(0).get(ActivityProperty.DATE);
    }

    private List<PropertySet> getFollowersNotifications(Context context, List<PropertySet> activities) {
        return SyncConfig.isNewFollowerNotificationsEnabled(context)
                ? activitiesOfKind(activities, ActivityKind.USER_FOLLOW)
                : Collections.<PropertySet>emptyList();
    }

    private List<PropertySet> getRepostNotifications(Context context, List<PropertySet> activities) {
        return SyncConfig.isRepostNotificationsEnabled(context)
                ? activitiesOfKind(activities, ActivityKind.TRACK_REPOST, ActivityKind.PLAYLIST_REPOST)
                : Collections.<PropertySet>emptyList();
    }

    private List<PropertySet> getCommentNotifications(Context context, List<PropertySet> activities) {
        return SyncConfig.isCommentNotificationsEnabled(context)
                ? activitiesOfKind(activities, ActivityKind.TRACK_COMMENT)
                : Collections.<PropertySet>emptyList();
    }

    private List<PropertySet> getLikeNotifications(Context context, List<PropertySet> activities) {
        return SyncConfig.isLikeNotificationEnabled(context)
                ? activitiesOfKind(activities, ActivityKind.TRACK_LIKE, ActivityKind.PLAYLIST_LIKE)
                : Collections.<PropertySet>emptyList();
    }

    private List<PropertySet> activitiesOfKind(List<PropertySet> activities, ActivityKind... kinds) {
        final List<PropertySet> filteredActivities = new LinkedList<>();
        for (PropertySet activity : activities) {
            for (ActivityKind kind : kinds) {
                if (kind.equals(activity.get(ActivityProperty.KIND))) {
                    filteredActivities.add(activity);
                }
            }
        }
        return filteredActivities;
    }

    private void showDashboardNotification(final Context context,
                                           final CharSequence ticker,
                                           final CharSequence title,
                                           final CharSequence message,
                                           final Intent intent,
                                           final int id,
                                           final Urn userUrn) {
        final Resources resources = context.getResources();
        final int targetIconWidth = resources.getDimensionPixelSize(R.dimen.notification_image_large_width);
        final int targetIconHeight = resources.getDimensionPixelSize(R.dimen.notification_image_large_height);
        final ApiImageSize imageSize = ApiImageSize.getNotificationLargeIconImageSize(resources);
        imageOperations.artwork(userUrn, imageSize, targetIconWidth, targetIconHeight)
                .subscribe(new IconSubscriber(context, ticker, intent, title, message, id));
    }

    private static void showDashboardNotification(Context context,
                                           CharSequence ticker,
                                           Intent intent,
                                           CharSequence title,
                                           CharSequence message,
                                           int id,
                                           @Nullable Bitmap bmp) {

        final PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification_cloud);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setTicker(ticker);
        builder.setContentTitle(context.getResources().getString(R.string.app_name));
        builder.setContentText(title + ScTextUtils.SPACE_SEPARATOR + message);

        if (bmp != null) {
            builder.setLargeIcon(bmp);
        }
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, builder.build());
    }

    private static Intent createNotificationIntent(String action) {
        Intent intent = new Intent(action)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Screen.NOTIFICATION.addToIntent(intent);
        Referrer.ACTIVITIES_NOTIFICATION.addToIntent(intent);
        return intent;
    }

    private static class IconSubscriber extends DefaultSubscriber<Bitmap> {
        private final Context context;
        private final CharSequence ticker;
        private final Intent intent;
        private final CharSequence title;
        private final CharSequence message;
        private final int id;

        public IconSubscriber(Context context, CharSequence ticker, Intent intent,
                              CharSequence title, CharSequence message, int id) {
            this.context = context;
            this.ticker = ticker;
            this.intent = intent;
            this.title = title;
            this.message = message;
            this.id = id;
        }

        @Override
        public void onNext(Bitmap bitmap) {
            showDashboardNotification(context, ticker, intent, title, message, id, bitmap);
        }

        @Override
        public void onError(Throwable e) {
            showDashboardNotification(context, ticker, intent, title, message, id, null);
        }
    }
}
