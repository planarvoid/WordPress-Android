package com.soundcloud.android.sync.content;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.APIRequestException;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.rx.observers.SuccessSubscriber;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserAssociationSyncer extends SyncStrategy {

    private static final int BULK_INSERT_BATCH_SIZE = 500;
    private static final String REQUEST_NO_BACKOFF = "0";

    private UserAssociationStorage userAssociationStorage;
    private FollowingOperations followingOperations;
    private int bulkInsertBatchSize = BULK_INSERT_BATCH_SIZE;

    public UserAssociationSyncer(Context context) {
        this(context, SoundCloudApplication.fromContext(context).getAccountOperations());
    }

    @VisibleForTesting
    protected UserAssociationSyncer(Context context, AccountOperations accountOperations) {
        super(context, context.getContentResolver(), accountOperations);
        Scheduler scheduler = Schedulers.immediate();
        init(new UserAssociationStorage(scheduler, context.getContentResolver()), new FollowingOperations(scheduler));
    }

    @VisibleForTesting
    protected UserAssociationSyncer(Context context, ContentResolver resolver, UserAssociationStorage userAssociationStorage,
                                    FollowingOperations followingOperations, AccountOperations accountOperations) {
        super(context, resolver, accountOperations);
        init(userAssociationStorage, followingOperations);
    }

    private void init(UserAssociationStorage userAssociationStorage, FollowingOperations followingOperations) {
        this.followingOperations = followingOperations;
        this.userAssociationStorage = userAssociationStorage;
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
        List<Long> itemDeletions = new ArrayList<Long>(local);
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
                        .add(PublicApiWrapper.LINKED_PARTITIONING, "1")
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
            final boolean success;

            switch (userAssociation.getLocalSyncState()) {
                case PENDING_ADDITION:
                    int status = api.put(request).getStatusLine().getStatusCode();
                    success = status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED;
                    break;

                case PENDING_REMOVAL:
                    status = api.delete(request).getStatusLine().getStatusCode();
                    success = status == HttpStatus.SC_OK || status == HttpStatus.SC_NOT_FOUND;
                    break;

                default:
                    // no flags, no op.
                    return true;
            }
            if (success) {
                userAssociationStorage.setFollowingAsSynced(userAssociation);
            }
            return success;

        } catch (IOException e) {
            Log.e(TAG, "error", e);
            return false;
        }
    }

    private boolean checkUnchanged(Content content, ApiSyncResult result, List<Long> local, List<Long> remote) {
        switch (content) {
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                Set<Long> localSet = new HashSet<Long>(local);
                Set<Long> remoteSet = new HashSet<Long>(remote);
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

            final BulkFollowSubscriber observer = new BulkFollowSubscriber(associationsNeedingSync, userAssociationStorage, new FollowingOperations());
            followingOperations.bulkFollowAssociations(associationsNeedingSync).subscribe(observer);
            if (!observer.wasSuccess()) {
                result.success = false;
            }
        }
        return result;
    }

    protected static class BulkFollowSubscriber extends SuccessSubscriber {

        private final FollowingOperations followingOperations;
        private UserAssociationStorage userAssociationStorage;
        private Collection<UserAssociation> userAssociations;

        public BulkFollowSubscriber(Collection<UserAssociation> userAssociations, UserAssociationStorage userAssociationStorage, FollowingOperations followingOperations) {
            this.userAssociations = userAssociations;
            this.userAssociationStorage = userAssociationStorage;
            this.followingOperations = followingOperations;
        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof APIRequestException && ((APIRequestException) e).response().responseCodeisForbidden()) {
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
