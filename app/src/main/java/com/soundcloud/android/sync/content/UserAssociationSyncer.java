package com.soundcloud.android.sync.content;

import static com.soundcloud.android.api.ApiRequestException.Reason.NOT_ALLOWED;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.Endpoints;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModel;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.profile.VerifyAgeActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.rx.observers.SuccessSubscriber;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
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
import android.support.v4.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserAssociationSyncer extends LegacySyncStrategy {

    private static final int BULK_INSERT_BATCH_SIZE = 500;
    private static final String REQUEST_NO_BACKOFF = "0";

    private final UserAssociationStorage userAssociationStorage;
    private final FollowingOperations followingOperations;
    private final NotificationManager notificationManager;
    private final JsonTransformer jsonTransformer;
    private final Navigator navigator;

    private int bulkInsertBatchSize = BULK_INSERT_BATCH_SIZE;

    public UserAssociationSyncer(Context context, AccountOperations accountOperations,
                                 FollowingOperations followingOperations, NotificationManager notificationManager,
                                 JsonTransformer jsonTransformer, Navigator navigator) {
        this(context, context.getContentResolver(),
                new UserAssociationStorage(Schedulers.immediate(), context.getContentResolver()),
                followingOperations, accountOperations, notificationManager, jsonTransformer, navigator);
    }

    @VisibleForTesting
    protected UserAssociationSyncer(Context context, ContentResolver resolver, UserAssociationStorage userAssociationStorage,
                                    FollowingOperations followingOperations, AccountOperations accountOperations,
                                    NotificationManager notificationManager, JsonTransformer jsonTransformer,
                                    Navigator navigator) {
        super(context, resolver, accountOperations);
        this.userAssociationStorage = userAssociationStorage;
        this.followingOperations = followingOperations;
        this.notificationManager = notificationManager;
        this.jsonTransformer = jsonTransformer;
        this.navigator = navigator;
    }

    public void setBulkInsertBatchSize(int bulkInsertBatchSize) {
        this.bulkInsertBatchSize = bulkInsertBatchSize;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@NotNull Uri uri, @Nullable String action) throws IOException {
        if (!isLoggedIn()) {
            Log.w(TAG, "Invalid user id, skipping sync ");
            return new ApiSyncResult(uri);

        } else if (action != null && action.equals(ApiSyncService.ACTION_PUSH)) {
            return pushUserAssociations(Content.match(uri));
        } else {
            return syncLocalToRemote(Content.match(uri), accountOperations.getLoggedInUserId());
        }
    }

    private ApiSyncResult syncLocalToRemote(Content content, final long userId) throws IOException {
        ApiSyncResult result = new ApiSyncResult(content.uri);
        if (!Content.ID_BASED.contains(content)) {
            return result;
        }

        List<Long> local = userAssociationStorage.getStoredIds(content.uri);
        List<Long> remote = api.readFullCollection(Request.to(content.remoteUri + "/ids"), IdHolder.class);

        if (!isLoggedIn()) {
            return result;
        }

        log("Cloud Api service: got remote ids " + remote.size() + " vs [local] " + local.size());
        result.setSyncData(System.currentTimeMillis(), remote.size());

        if (checkUnchanged(content, result, local, remote)) {
            result.success = true;
            return result;
        }

        // deletions can happen here, has no impact
        List<Long> itemDeletions = new ArrayList<>(local);
        itemDeletions.removeAll(remote);
        userAssociationStorage.deleteAssociations(content.uri, itemDeletions);

        int startPosition = 1;
        int added = 0;
        switch (content) {
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                // load the first page of items to get proper last_seen ordering
                // parse and add first items
                List<PublicApiResource> resources = api.readList(Request.to(content.remoteUri)
                        .add(PublicApi.LINKED_PARTITIONING, "1")
                        .add("limit", Consts.LIST_PAGE_SIZE));

                if (!isLoggedIn()) {
                    return new ApiSyncResult(content.uri);
                }

                added = userAssociationStorage.insertAssociations(resources, content.uri, userId);

                // remove items from master remote list and adjust start index
                for (PublicApiResource u : resources) {
                    remote.remove(u.getId());
                }
                startPosition = resources.size();
                break;
        }

        log("Added " + added + " new items for this endpoint");
        userAssociationStorage.insertInBatches(content, userId, remote, startPosition, bulkInsertBatchSize);
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
            userAssociationStorage.setFollowingAsSynced(userAssociation);
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
        DefaultSubscriber.fireAndForget(followingOperations.removeFollowing(userAssociation.getUser()));
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
        String content = context.getString(R.string.follow_blocked_content, userName);
        String contentLong = context.getString(R.string.follow_blocked_content_long, userName);
        return buildNotification(title, content, contentLong, buildReturnToProfileIntent(userAssociation));
    }

    private Notification buildUnknownAgeNotification(UserAssociation userAssociation, String userName) {
        String title = context.getString(R.string.follow_age_unknown_title);
        String content = context.getString(R.string.follow_age_unknown_content, userName);
        String contentLong = context.getString(R.string.follow_age_unknown_content_long, userName);
        return buildNotification(title, content, contentLong, buildVerifyAgeIntent(userAssociation));
    }

    private Notification buildUnderAgeNotification(UserAssociation userAssociation, FollowErrors errors, String userName) {
        String title = context.getString(R.string.follow_age_restricted_title);
        String content = context.getString(R.string.follow_age_restricted_content, errors.getAge(), userName);
        String contentLong = context.getString(R.string.follow_age_restricted_content_long, errors.getAge(), userName);
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
            userAssociationStorage.setFollowingAsSynced(userAssociation);
            return true;
        } else {
            Log.w(TAG, "failure " + status + " in user association removal of " + userAssociation.getUser().getId());
            return false;
        }
    }

    private boolean checkUnchanged(Content content, ApiSyncResult result, List<Long> local, List<Long> remote) {
        switch (content) {
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                Set<Long> localSet = new HashSet<>(local);
                Set<Long> remoteSet = new HashSet<>(remote);
                if (!localSet.equals(remoteSet)) {
                    result.change = ApiSyncResult.CHANGED;
                    result.extra = REQUEST_NO_BACKOFF; // reset sync misses
                } else {
                    result.change = remoteSet.isEmpty() ? ApiSyncResult.UNCHANGED : ApiSyncResult.REORDERED; // always mark users as reordered so we get the first page
                }
                break;
            default:
                if (!local.equals(remote)) {
                    // items have been added or removed (not just ordering) so this is a sync hit
                    result.change = ApiSyncResult.CHANGED;
                    result.extra = REQUEST_NO_BACKOFF; // reset sync misses
                } else {
                    result.change = ApiSyncResult.UNCHANGED;
                    log("Cloud Api service: no change in URI " + content.uri + ". Skipping sync.");
                }
        }
        return result.change == ApiSyncResult.UNCHANGED;
    }

    private ApiSyncResult pushUserAssociations(Content content) {
        final ApiSyncResult result = new ApiSyncResult(content.uri);
        result.success = true;
        if (content == Content.ME_FOLLOWINGS && userAssociationStorage.hasFollowingsNeedingSync()) {
            List<UserAssociation> associationsNeedingSync = userAssociationStorage.getFollowingsNeedingSync();
            for (UserAssociation userAssociation : associationsNeedingSync) {
                if (!userAssociation.hasToken() && !pushUserAssociation(userAssociation)) {
                    result.success = false;
                }
            }

            final BulkFollowSubscriber observer = new BulkFollowSubscriber(associationsNeedingSync, userAssociationStorage, followingOperations);
            followingOperations.bulkFollowAssociations(associationsNeedingSync).subscribe(observer);
            if (!observer.wasSuccess()) {
                result.success = false;
            }
        }
        return result;
    }

    protected static class BulkFollowSubscriber extends SuccessSubscriber {

        private final FollowingOperations followingOperations;
        private final UserAssociationStorage userAssociationStorage;
        private final Collection<UserAssociation> userAssociations;

        public BulkFollowSubscriber(Collection<UserAssociation> userAssociations, UserAssociationStorage userAssociationStorage, FollowingOperations followingOperations) {
            this.userAssociations = userAssociations;
            this.userAssociationStorage = userAssociationStorage;
            this.followingOperations = followingOperations;
        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof ApiRequestException && ((ApiRequestException) e).reason() == NOT_ALLOWED) {
                /*
                 Tokens were expired. Delete the user associations and followings from memory.
                 TODO : retry logic somehow
                  */
                final Collection<PublicApiUser> users = Collections2.transform(userAssociations, new Function<UserAssociation, PublicApiUser>() {
                    @Override
                    public PublicApiUser apply(UserAssociation input) {
                        return input.getUser();
                    }
                });
                followingOperations.updateLocalStatus(false, ScModel.getIdList(Lists.newArrayList(users)));
                userAssociationStorage.deleteFollowings(userAssociations);
            }
            super.onError(e);
        }
    }
}
