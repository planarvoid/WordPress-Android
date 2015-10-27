package com.soundcloud.android.sync.affiliations;

import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.Endpoints;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.associations.NextFollowingOperations;
import com.soundcloud.android.profile.VerifyAgeActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.LegacyUserAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.LegacySyncStrategy;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.schedulers.Schedulers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyFollowingsSyncer extends LegacySyncStrategy {

    private static final int BULK_INSERT_BATCH_SIZE = 500;
    private static final String REQUEST_NO_BACKOFF = "0";

    private final LegacyUserAssociationStorage legacyUserAssociationStorage;
    private final NextFollowingOperations nextFollowingOperations;
    private final NotificationManager notificationManager;
    private final JsonTransformer jsonTransformer;
    private final Navigator navigator;

    private int bulkInsertBatchSize = BULK_INSERT_BATCH_SIZE;

    public MyFollowingsSyncer(Context context, AccountOperations accountOperations,
                              NextFollowingOperations nextFollowingOperations,
                              NotificationManager notificationManager,
                              JsonTransformer jsonTransformer, Navigator navigator) {
        this(context, context.getContentResolver(),
                new LegacyUserAssociationStorage(Schedulers.immediate(), context.getContentResolver()),
                accountOperations, nextFollowingOperations, notificationManager, jsonTransformer, navigator);
    }

    @VisibleForTesting
    protected MyFollowingsSyncer(Context context, ContentResolver resolver, LegacyUserAssociationStorage legacyUserAssociationStorage,
                                 AccountOperations accountOperations,
                                 NextFollowingOperations nextFollowingOperations, NotificationManager notificationManager, JsonTransformer jsonTransformer,
                                 Navigator navigator) {
        super(context, resolver, accountOperations);
        this.legacyUserAssociationStorage = legacyUserAssociationStorage;
        this.nextFollowingOperations = nextFollowingOperations;
        this.notificationManager = notificationManager;
        this.jsonTransformer = jsonTransformer;
        this.navigator = navigator;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws IOException {
        if (!isLoggedIn()) {
            Log.w(TAG, "Invalid user id, skipping sync ");
            return new ApiSyncResult(Content.ME_FOLLOWINGS.uri);

        } else if (action != null && action.equals(ApiSyncService.ACTION_PUSH)) {
            return pushUserAssociations();
        } else {
            return syncLocalToRemote(accountOperations.getLoggedInUserId());
        }
    }

    private ApiSyncResult syncLocalToRemote(final long userId) throws IOException {
        ApiSyncResult result = new ApiSyncResult(Content.ME_FOLLOWINGS.uri);

        List<Long> local = legacyUserAssociationStorage.getStoredIds(Content.ME_FOLLOWINGS.uri);
        List<Long> remote = api.readFullCollection(Request.to(Content.ME_FOLLOWINGS.remoteUri + "/ids"), IdHolder.class);

        if (!isLoggedIn()) {
            return result;
        }

        log("Cloud Api service: got remote ids " + remote.size() + " vs [local] " + local.size());
        result.setSyncData(System.currentTimeMillis(), remote.size());

        if (checkUnchanged(result, local, remote)) {
            result.success = true;
            return result;
        }

        // deletions can happen here, has no impact
        List<Long> itemDeletions = new ArrayList<>(local);
        itemDeletions.removeAll(remote);
        legacyUserAssociationStorage.deleteAssociations(Content.ME_FOLLOWINGS.uri, itemDeletions);

        int startPosition = 1;
        int added = 0;

        // load the first page of items to get proper last_seen ordering
        // parse and add first items
        List<PublicApiResource> resources = api.readList(Request.to(Content.ME_FOLLOWINGS.remoteUri)
                .add(PublicApi.LINKED_PARTITIONING, "1")
                .add("limit", Consts.LIST_PAGE_SIZE));

        if (!isLoggedIn()) {
            return new ApiSyncResult(Content.ME_FOLLOWINGS.uri);
        }

        added = legacyUserAssociationStorage.insertAssociations(resources, Content.ME_FOLLOWINGS.uri, userId);

        // remove items from master remote list and adjust start index
        for (PublicApiResource u : resources) {
            remote.remove(u.getId());
        }
        startPosition = resources.size();

        log("Added " + added + " new items for this endpoint");
        legacyUserAssociationStorage.insertInBatches(userId, remote, startPosition, bulkInsertBatchSize);
        result.success = true;
        return result;
    }

    /* package */ boolean pushUserAssociation(UserAssociation userAssociation) {
        final Request request = Request.to(Endpoints.MY_FOLLOWING, userAssociation.getUser().getId());
        try {

            switch (userAssociation.getLocalSyncState()) {
                case PENDING_ADDITION:
                    return pushUserAssociationAddition(userAssociation, request);

                case PENDING_REMOVAL:
                    return pushUserAssociationRemoval(userAssociation, request);

                default:
                    // no flags, no op.
                    return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return false;
        }
    }

    private boolean pushUserAssociationAddition(UserAssociation userAssociation, Request request) throws IOException {
        final HttpResponse response = api.put(request);
        int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_FORBIDDEN) {
            forbiddenUserPushHandler(userAssociation, extractApiErrors(response.getEntity()));
            return true;
        } else if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED) {
            legacyUserAssociationStorage.setFollowingAsSynced(userAssociation);
            return true;
        } else {
            Log.w(TAG, "failure " + status + " in user association addition of " + userAssociation.getUser().getId());
            return false;
        }
    }

    private FollowErrors extractApiErrors(HttpEntity entity) throws IOException {
        try {
            String responseBody = IOUtils.readInputStream(entity.getContent());
            return jsonTransformer.fromJson(responseBody, TypeToken.of(FollowErrors.class));
        } catch (ApiMapperException e) {
            return FollowErrors.empty();
        }
    }

    private void forbiddenUserPushHandler(UserAssociation userAssociation, FollowErrors errors) {
        Notification notification = getForbiddenNotification(userAssociation, errors);
        notificationManager.notify(userAssociation.getUser().getUrn().toString(), NotificationConstants.FOLLOW_BLOCKED_NOTIFICATION_ID, notification);
        DefaultSubscriber.fireAndForget(nextFollowingOperations.toggleFollowing(userAssociation.getUser().getUrn(), false));
    }

    private Notification getForbiddenNotification(UserAssociation userAssociation, FollowErrors errors) {
        final String userName = userAssociation.getUser().getDisplayName();
        if (errors.isAgeRestricted()) {
            return buildUnderAgeNotification(userAssociation, errors, userName);
        } else if (errors.isAgeUnknown()) {
            return buildUnknownAgeNotification(userAssociation, userName);
        } else {
            return buildBlockedFollowNotification(userAssociation, userName);
        }
    }

    private Notification buildBlockedFollowNotification(UserAssociation userAssociation, String userName) {
        String title = context.getString(R.string.follow_blocked_title);
        String content = context.getString(R.string.follow_blocked_content_username, userName);
        String contentLong = context.getString(R.string.follow_blocked_content_long_username, userName);
        return buildNotification(title, content, contentLong, buildReturnToProfileIntent(userAssociation));
    }

    private Notification buildUnknownAgeNotification(UserAssociation userAssociation, String userName) {
        String title = context.getString(R.string.follow_age_unknown_title);
        String content = context.getString(R.string.follow_age_unknown_content_username, userName);
        String contentLong = context.getString(R.string.follow_age_unknown_content_long_username, userName);
        return buildNotification(title, content, contentLong, buildVerifyAgeIntent(userAssociation));
    }

    private Notification buildUnderAgeNotification(UserAssociation userAssociation, FollowErrors errors, String userName) {
        String title = context.getString(R.string.follow_age_restricted_title);
        String content = context.getString(R.string.follow_age_restricted_content_age_username, errors.getAge(), userName);
        String contentLong = context.getString(R.string.follow_age_restricted_content_long_age_username, errors.getAge(), userName);
        return buildNotification(title, content, contentLong, buildReturnToProfileIntent(userAssociation));
    }

    private Notification buildNotification(String title, String content, String contentLong, PendingIntent contentIntent) {
        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification_cloud)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentLong).setBigContentTitle(title))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build();
    }

    private PendingIntent buildVerifyAgeIntent(UserAssociation userAssociation) {
        Intent verifyAgeActivity = VerifyAgeActivity.getIntent(context, userAssociation.getUser().getUrn());
        verifyAgeActivity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, verifyAgeActivity, 0);
    }

    private PendingIntent buildReturnToProfileIntent(UserAssociation userAssociation) {
        return navigator.openProfileFromNotification(context, userAssociation.getUser().getUrn());
    }

    private boolean pushUserAssociationRemoval(UserAssociation userAssociation, Request request) throws IOException {
        int status = api.delete(request).getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_OK || status == HttpStatus.SC_NOT_FOUND || status == HttpStatus.SC_UNPROCESSABLE_ENTITY) {
            legacyUserAssociationStorage.setFollowingAsSynced(userAssociation);
            return true;
        } else {
            Log.w(TAG, "failure " + status + " in user association removal of " + userAssociation.getUser().getId());
            return false;
        }
    }

    private boolean checkUnchanged(ApiSyncResult result, List<Long> local, List<Long> remote) {
        Set<Long> localSet = new HashSet<>(local);
        Set<Long> remoteSet = new HashSet<>(remote);
        if (!localSet.equals(remoteSet)) {
            result.change = ApiSyncResult.CHANGED;
            result.extra = REQUEST_NO_BACKOFF; // reset sync misses
        } else {
            result.change = remoteSet.isEmpty() ? ApiSyncResult.UNCHANGED : ApiSyncResult.REORDERED; // always mark users as reordered so we get the first page
        }
        return result.change == ApiSyncResult.UNCHANGED;
    }

    private ApiSyncResult pushUserAssociations() {
        final ApiSyncResult result = new ApiSyncResult(Content.ME_FOLLOWINGS.uri);
        result.success = true;
        if (legacyUserAssociationStorage.hasFollowingsNeedingSync()) {
            List<UserAssociation> associationsNeedingSync = legacyUserAssociationStorage.getFollowingsNeedingSync();
            for (UserAssociation userAssociation : associationsNeedingSync) {
                if (!userAssociation.hasToken() && !pushUserAssociation(userAssociation)) {
                    result.success = false;
                }
            }
        }
        return result;
    }
}
