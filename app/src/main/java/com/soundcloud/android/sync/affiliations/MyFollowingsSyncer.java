package com.soundcloud.android.sync.affiliations;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.Endpoints;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.VerifyAgeActivity;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.LegacySyncResult;
import com.soundcloud.android.sync.LegacySyncStrategy;
import com.soundcloud.android.users.UserAssociationProperty;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.http.HttpStatus;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
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
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@AutoFactory
public class MyFollowingsSyncer extends LegacySyncStrategy implements Callable<Boolean> {

    private static final String REQUEST_NO_BACKOFF = "0";

    private final ApiClient apiClient;
    private final UserAssociationStorage userAssociationStorage;
    private final FollowingOperations followingOperations;
    private final NotificationManager notificationManager;
    private final JsonTransformer jsonTransformer;
    private final Navigator navigator;
    private final String action;

    @Inject
    public MyFollowingsSyncer(@Provided Context context,
                              @Provided PublicApi publicApi,
                              @Provided ApiClient apiClient,
                              @Provided AccountOperations accountOperations,
                              @Provided FollowingOperations followingOperations,
                              @Provided NotificationManager notificationManager,
                              @Provided JsonTransformer jsonTransformer,
                              @Provided Navigator navigator,
                              @Provided UserAssociationStorage userAssociationStorage,
                              @Nullable String action) {

        super(context, publicApi, accountOperations);
        this.apiClient = apiClient;
        this.userAssociationStorage = userAssociationStorage;
        this.followingOperations = followingOperations;
        this.notificationManager = notificationManager;
        this.jsonTransformer = jsonTransformer;
        this.navigator = navigator;
        this.action = action;
    }

    @NotNull
    @Override
    public LegacySyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws IOException, ApiMapperException, ApiRequestException {
        if (!isLoggedIn()) {
            Log.w(TAG, "Invalid user id, skipping sync ");
            return new LegacySyncResult(Content.ME_FOLLOWINGS.uri);
        } else {
            final LegacySyncResult legacySyncResult = pushUserAssociations();
            if (legacySyncResult.success) {
                return syncLocalToRemote();
             } else {
                return legacySyncResult;
            }
        }
    }

    @Override
    public Boolean call() throws Exception {
        return syncContent(Content.ME_FOLLOWINGS.uri, this.action).change != LegacySyncResult.UNCHANGED;
    }

    private LegacySyncResult syncLocalToRemote() throws IOException, ApiMapperException, ApiRequestException {
        LegacySyncResult result = new LegacySyncResult(Content.ME_FOLLOWINGS.uri);

        Set<Long> local = userAssociationStorage.loadFollowedUserIds();
        List<Long> followedUserIds = getFollowingUserIds();

        result.setSyncData(System.currentTimeMillis(), followedUserIds.size());
        if (checkUnchanged(result, local, followedUserIds)) {
            result.success = true;
            return result;
        }

        // deletions can happen here, has no impact
        List<Long> itemDeletions = new ArrayList<>(local);
        itemDeletions.removeAll(followedUserIds);
        userAssociationStorage.deleteFollowingsById(itemDeletions);
        userAssociationStorage.insertFollowedUserIds(followedUserIds, 0);
        result.success = true;
        return result;
    }

    @NonNull
    private List<Long> getFollowingUserIds() throws IOException, ApiRequestException, ApiMapperException {
        final ApiRequest request = ApiRequest.get(ApiEndpoints.MY_FOLLOWINGS.path())
                                             .forPrivateApi()
                                             .build();
        ModelCollection<ApiFollowing> apiFollowings = apiClient.fetchMappedResponse(request,
                                                                                    new TypeToken<ModelCollection<ApiFollowing>>() {
                                                                         });
        return Lists.transform(apiFollowings.getCollection(), new Function<ApiFollowing, Long>() {
            @Nullable
            @Override
            public Long apply(ApiFollowing input) {
                return input.getTargetUrn().getNumericId();
            }
        });
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
        notificationManager.notify(userUrn.toString(),
                                   NotificationConstants.FOLLOW_BLOCKED_NOTIFICATION_ID,
                                   notification);
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
        String content = context.getString(R.string.follow_age_restricted_content_age_username,
                                           String.valueOf(errors.getAge()),
                                           userName);
        String contentLong = context.getString(R.string.follow_age_restricted_content_long_age_username,
                                               String.valueOf(errors.getAge()),
                                               userName);
        return buildNotification(title, content, contentLong, navigator.openProfileFromNotification(context, userUrn));
    }

    private Notification buildNotification(String title,
                                           String content,
                                           String contentLong,
                                           PendingIntent contentIntent) {
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

    private boolean checkUnchanged(LegacySyncResult result, Set<Long> localSet, List<Long> remote) {
        Set<Long> remoteSet = new HashSet<>(remote);
        if (!localSet.equals(remoteSet)) {
            result.change = LegacySyncResult.CHANGED;
            result.extra = REQUEST_NO_BACKOFF; // reset sync misses
        } else {
            result.change = remoteSet.isEmpty() ?
                            LegacySyncResult.UNCHANGED :
                            LegacySyncResult.REORDERED; // always mark users as reordered so we get the first page
        }
        return result.change == LegacySyncResult.UNCHANGED;
    }

    private LegacySyncResult pushUserAssociations() {
        final LegacySyncResult result = new LegacySyncResult(Content.ME_FOLLOWINGS.uri);
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
}
