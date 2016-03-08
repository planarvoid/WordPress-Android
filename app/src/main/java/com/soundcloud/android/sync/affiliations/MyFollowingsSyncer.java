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
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.VerifyAgeActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.LegacySyncStrategy;
import com.soundcloud.android.users.UserAssociationProperty;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.http.HttpStatus;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.reflect.TypeToken;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyFollowingsSyncer extends LegacySyncStrategy {

    private static final String REQUEST_NO_BACKOFF = "0";

    private final UserAssociationStorage userAssociationStorage;
    private final FollowingOperations followingOperations;
    private final NotificationManager notificationManager;
    private final JsonTransformer jsonTransformer;
    private final Navigator navigator;

    @Inject
    public MyFollowingsSyncer(Context context,
                              PublicApi publicApi,
                              AccountOperations accountOperations,
                              FollowingOperations followingOperations,
                              NotificationManager notificationManager,
                              JsonTransformer jsonTransformer, Navigator navigator,
                              UserAssociationStorage userAssociationStorage) {
        super(context, publicApi, accountOperations);
        this.userAssociationStorage = userAssociationStorage;
        this.followingOperations = followingOperations;
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
            return syncLocalToRemote();
        }
    }

    private ApiSyncResult syncLocalToRemote() throws IOException {
        ApiSyncResult result = new ApiSyncResult(Content.ME_FOLLOWINGS.uri);

        Set<Long> local = userAssociationStorage.loadFollowedUserIds();
        List<Long> followedUserIds = api.readFullCollection(Request.to(Content.ME_FOLLOWINGS.remoteUri + "/ids"), IdHolder.class);

        log("Cloud Api service: got followedUserIds " + followedUserIds.size() + " vs [local] " + local.size());
        result.setSyncData(System.currentTimeMillis(), followedUserIds.size());

        if (checkUnchanged(result, local, followedUserIds)) {
            result.success = true;
            return result;
        }

        // deletions can happen here, has no impact
        List<Long> itemDeletions = new ArrayList<>(local);
        itemDeletions.removeAll(followedUserIds);
        userAssociationStorage.deleteFollowingsById(itemDeletions);

        int startPosition;

        // load the first page of items to get proper last_seen ordering
        // parse and add first items
        List<PublicApiUser> followedUsers = api.readList(Request.to(Content.ME_FOLLOWINGS.remoteUri)
                .add(PublicApi.LINKED_PARTITIONING, "1")
                .add("limit", Consts.LIST_PAGE_SIZE));

        userAssociationStorage.insertFollowedUsers(followedUsers);

        // remove items from master followedUserIds list and adjust start index
        for (PublicApiUser user : followedUsers) {
            followedUserIds.remove(user.getId());
        }
        startPosition = followedUsers.size();

        userAssociationStorage.insertFollowedUserIds(followedUserIds, startPosition);
        result.success = true;
        return result;
    }

    private LocalState getLocalSyncState(PropertySet userAssociation) {
        if (userAssociation.contains(UserAssociationProperty.ADDED_AT)) {
            return LocalState.PENDING_ADDITION;
        } else if (userAssociation.contains(UserAssociationProperty.REMOVED_AT)) {
            return LocalState.PENDING_REMOVAL;
        } else {
            return LocalState.NONE;
        }
    }

    boolean pushUserAssociation(PropertySet userAssociation) {
        final Urn userUrn = userAssociation.get(UserProperty.URN);
        final String userName = userAssociation.get(UserProperty.USERNAME);
        final Request request = Request.to(Endpoints.MY_FOLLOWING, userUrn.getNumericId());
        try {

            switch (getLocalSyncState(userAssociation)) {
                case PENDING_ADDITION:
                    return pushUserAssociationAddition(userUrn, userName, request);

                case PENDING_REMOVAL:
                    return pushUserAssociationRemoval(userUrn, request);

                default:
                    // no flags, no op.
                    return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return false;
        }
    }

    private boolean pushUserAssociationAddition(Urn userUrn, String userName, Request request) throws IOException {
        final HttpResponse response = api.put(request);
        int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.FORBIDDEN) {
            forbiddenUserPushHandler(userUrn, userName, extractApiErrors(response.getEntity()));
            return true;
        } else if (status == HttpStatus.OK || status == HttpStatus.CREATED) {
            userAssociationStorage.updateFollowingFromPendingState(userUrn);
            return true;
        } else {
            Log.w(TAG, "failure " + status + " in user association addition of " + userUrn);
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

    private void forbiddenUserPushHandler(Urn userUrn, String userName, FollowErrors errors) {
        Notification notification = getForbiddenNotification(userUrn, userName, errors);
        notificationManager.notify(userUrn.toString(), NotificationConstants.FOLLOW_BLOCKED_NOTIFICATION_ID, notification);
        DefaultSubscriber.fireAndForget(followingOperations.toggleFollowing(userUrn, false));
    }

    private Notification getForbiddenNotification(Urn userUrn, String userName, FollowErrors errors) {
        if (errors.isAgeRestricted()) {
            return buildUnderAgeNotification(userUrn, errors, userName);
        } else if (errors.isAgeUnknown()) {
            return buildUnknownAgeNotification(userUrn, userName);
        } else {
            return buildBlockedFollowNotification(userUrn, userName);
        }
    }

    private Notification buildBlockedFollowNotification(Urn userUrn, String userName) {
        String title = context.getString(R.string.follow_blocked_title);
        String content = context.getString(R.string.follow_blocked_content_username, userName);
        String contentLong = context.getString(R.string.follow_blocked_content_long_username, userName);
        return buildNotification(title, content, contentLong, navigator.openProfileFromNotification(context, userUrn));
    }

    private Notification buildUnknownAgeNotification(Urn userUrn, String userName) {
        String title = context.getString(R.string.follow_age_unknown_title);
        String content = context.getString(R.string.follow_age_unknown_content_username, userName);
        String contentLong = context.getString(R.string.follow_age_unknown_content_long_username, userName);
        return buildNotification(title, content, contentLong, buildVerifyAgeIntent(userUrn));
    }

    private Notification buildUnderAgeNotification(Urn userUrn, FollowErrors errors, String userName) {
        String title = context.getString(R.string.follow_age_restricted_title);
        String content = context.getString(R.string.follow_age_restricted_content_age_username, errors.getAge(), userName);
        String contentLong = context.getString(R.string.follow_age_restricted_content_long_age_username, errors.getAge(), userName);
        return buildNotification(title, content, contentLong, navigator.openProfileFromNotification(context, userUrn));
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

    private PendingIntent buildVerifyAgeIntent(Urn userUrn) {
        Intent verifyAgeActivity = VerifyAgeActivity.getIntent(context, userUrn);
        verifyAgeActivity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, verifyAgeActivity, 0);
    }

    private boolean pushUserAssociationRemoval(Urn userUrn, Request request) throws IOException {
        int status = api.delete(request).getStatusLine().getStatusCode();
        if (status == HttpStatus.OK || status == HttpStatus.NOT_FOUND || status == HttpStatus.UNPROCESSABLE_ENTITY) {
            userAssociationStorage.updateFollowingFromPendingState(userUrn);
            return true;
        } else {
            Log.w(TAG, "failure " + status + " in user association removal of " + userUrn);
            return false;
        }
    }

    private boolean checkUnchanged(ApiSyncResult result, Set<Long> localSet, List<Long> remote) {
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
        if (userAssociationStorage.hasStaleFollowings()) {
            List<PropertySet> associationsNeedingSync = userAssociationStorage.loadStaleFollowings();
            for (PropertySet userAssociation : associationsNeedingSync) {
                if (!pushUserAssociation(userAssociation)) {
                    result.success = false;
                }
            }
        }
        return result;
    }

    private enum LocalState {
        NONE, PENDING_ADDITION, PENDING_REMOVAL
    }

    static class IdHolder extends CollectionHolder<Long> {
    }
}
