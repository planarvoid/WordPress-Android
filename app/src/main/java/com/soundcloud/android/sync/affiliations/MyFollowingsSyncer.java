package com.soundcloud.android.sync.affiliations;

import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.PendingIntentFactory;
import com.soundcloud.android.profile.VerifyAgeActivity;
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver;
import com.soundcloud.android.users.Following;
import com.soundcloud.android.users.FollowingStorage;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserStorage;
import com.soundcloud.android.utils.Log;
import com.soundcloud.http.HttpStatus;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.java.strings.Strings;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.NotificationCompat;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class MyFollowingsSyncer implements Callable<Boolean> {

    private static final String TAG = "MyFollowingsSyncer";

    private final FollowErrorNotificationBuilder notificationBuilder;
    private final ApiClient apiClient;
    private final FollowingStorage followingStorage;
    private final FollowingOperations followingOperations;
    private final NotificationManager notificationManager;
    private final JsonTransformer jsonTransformer;
    private final UserStorage userStorage;

    @Inject
    MyFollowingsSyncer(FollowErrorNotificationBuilder notificationBuilder,
                       ApiClient apiClient,
                       FollowingOperations followingOperations,
                       NotificationManager notificationManager,
                       JsonTransformer jsonTransformer,
                       FollowingStorage followingStorage,
                       UserStorage userStorage) {

        super();
        this.notificationBuilder = notificationBuilder;
        this.apiClient = apiClient;
        this.followingStorage = followingStorage;
        this.followingOperations = followingOperations;
        this.notificationManager = notificationManager;
        this.jsonTransformer = jsonTransformer;
        this.userStorage = userStorage;
    }

    @Override
    public Boolean call() throws Exception {
        return pushUserAssociations() && syncLocalToRemote();
    }

    private boolean pushUserAssociations() throws IOException, ApiRequestException {
        if (followingStorage.hasStaleFollowings()) {
            List<Following> followings = followingStorage.loadStaleFollowings();
            for (Following following : followings) {
                pushFollowing(following);
            }
        }
        return true;
    }

    private boolean syncLocalToRemote() throws IOException, ApiMapperException, ApiRequestException {
        Set<Urn> local = followingStorage.loadFollowedUserIds();
        List<Urn> followedUserIds = getFollowingUserIds();
        Set<Urn> remoteSet = new HashSet<>(followedUserIds);
        if (local.equals(remoteSet)) {
            return false;
        }

        // deletions can happen here, has no impact
        List<Urn> itemDeletions = new ArrayList<>(local);
        itemDeletions.removeAll(followedUserIds);
        followingStorage.deleteFollowingsById(itemDeletions);
        followingStorage.insertFollowedUserIds(followedUserIds);
        return true;
    }

    @NonNull
    private List<Urn> getFollowingUserIds() throws IOException, ApiRequestException, ApiMapperException {
        final ApiRequest request = ApiRequest.get(ApiEndpoints.MY_FOLLOWINGS.path())
                                             .forPrivateApi()
                                             .build();
        ModelCollection<ApiFollowing> apiFollowings = apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<ApiFollowing>>() {
        });
        return Lists.transform(apiFollowings.getCollection(), ApiFollowing.TO_USER_URNS);
    }

    private void pushFollowing(Following following) throws IOException, ApiRequestException {
        final Urn userUrn = following.getUserUrn();

        if (following.getAddedAt() != null) {
            pushUserAssociationAddition(userUrn,
                                        ApiRequest.post(ApiEndpoints.USER_FOLLOWS.path(userUrn))
                                                  .forPrivateApi()
                                                  .build());
        } else if (following.getRemovedAt() != null) {
            pushUserAssociationRemoval(userUrn,
                                       ApiRequest.delete(ApiEndpoints.USER_FOLLOWS.path(userUrn))
                                                 .forPrivateApi()
                                                 .build());
        } else {
            throw new IllegalArgumentException("FollowingWithUser does not need syncing: " + following);
        }
    }

    private void pushUserAssociationAddition(Urn userUrn, ApiRequest request) throws IOException, ApiRequestException {
        final ApiResponse response = apiClient.fetchResponse(request);
        int status = response.getStatusCode();
        if (shouldHandleError(status)) {
            handleError(userUrn, extractApiError(response.getResponseBody()));
        } else if (response.isSuccess()) {
            followingStorage.updateFollowingFromPendingState(userUrn);
        } else {
            Log.w(TAG, "failure " + status + " in user association addition of " + userUrn);
            throw response.getFailure();
        }
    }

    private boolean shouldHandleError(int status) {
        return status == HttpStatus.FORBIDDEN || status == HttpStatus.BAD_REQUEST || status == HttpStatus.NOT_FOUND;
    }

    private void pushUserAssociationRemoval(Urn userUrn, ApiRequest request) throws ApiRequestException {
        ApiResponse apiResponse = apiClient.fetchResponse(request);
        int status = apiResponse.getStatusCode();
        if (apiResponse.isSuccess()
                || apiResponse.getStatusCode() == HttpStatus.NOT_FOUND
                || apiResponse.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
            followingStorage.updateFollowingFromPendingState(userUrn);
        } else {
            Log.w(TAG, "failure " + status + " in user association removal of " + userUrn);
            throw apiResponse.getFailure();
        }
    }

    @Nullable
    private FollowError extractApiError(String responseBody) throws IOException {
        if (Strings.isBlank(responseBody)) {
            return null;
        }

        try {
            return jsonTransformer.fromJson(responseBody, TypeToken.of(FollowError.class));
        } catch (ApiMapperException e) {
            return null;
        }
    }

    private void handleError(Urn userUrn, FollowError error) {
        final Optional<User> optionalUser = userStorage.loadUser(userUrn).map(Optional::of).toSingle(Optional.absent()).blockingGet();
        optionalUser.ifPresent(user -> {
            Optional<Notification> notification = getNotificationForError(userUrn, user.username(), error);
            if (notification.isPresent()) {
                notificationManager.notify(userUrn.toString(),
                                           NotificationConstants.FOLLOW_BLOCKED_NOTIFICATION_ID,
                                           notification.get());
            }
        });
        followingOperations.toggleFollowing(userUrn, false).subscribe(new DefaultDisposableCompletableObserver());
    }

    private Optional<Notification> getNotificationForError(Urn userUrn, String userName, FollowError error) {
        if (error == null) return Optional.absent();

        if (error.isAgeRestricted()) {
            return Optional.of(notificationBuilder.buildUnderAgeNotification(userUrn, error.age, userName));
        } else if (error.isAgeUnknown()) {
            return Optional.of(notificationBuilder.buildUnknownAgeNotification(userUrn, userName));
        } else if (error.isBlocked()) {
            return Optional.of(notificationBuilder.buildBlockedFollowNotification(userUrn, userName));
        }

        return Optional.absent();
    }

    @VisibleForTesting
    static class FollowErrorNotificationBuilder {
        private final Context context;

        @Inject
        FollowErrorNotificationBuilder(Context context) {
            this.context = context;
        }

        Notification buildBlockedFollowNotification(Urn userUrn, String userName) {
            String title = context.getString(R.string.follow_blocked_title);
            String content = context.getString(R.string.follow_blocked_content_username, userName);
            String contentLong = context.getString(R.string.follow_blocked_content_long_username, userName);
            return buildNotification(title, content, contentLong, PendingIntentFactory.openProfileFromNotification(context, userUrn));
        }

        Notification buildUnknownAgeNotification(Urn userUrn, String userName) {
            String title = context.getString(R.string.follow_age_unknown_title);
            String content = context.getString(R.string.follow_age_unknown_content_username, userName);
            String contentLong = context.getString(R.string.follow_age_unknown_content_long_username, userName);
            return buildNotification(title, content, contentLong, buildVerifyAgeIntent(userUrn));
        }

        Notification buildUnderAgeNotification(Urn userUrn, int age, String userName) {
            String title = context.getString(R.string.follow_age_restricted_title);
            String content = context.getString(R.string.follow_age_restricted_content_age_username,
                                               String.valueOf(age),
                                               userName);
            String contentLong = context.getString(R.string.follow_age_restricted_content_long_age_username,
                                                   String.valueOf(age),
                                                   userName);
            return buildNotification(title, content, contentLong, PendingIntentFactory.openProfileFromNotification(context, userUrn));
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
    }
}
