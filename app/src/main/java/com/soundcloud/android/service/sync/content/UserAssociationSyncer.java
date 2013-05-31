package com.soundcloud.android.service.sync.content;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.Wrapper;
import com.soundcloud.android.dao.CollectionStorage;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncResult;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserAssociationSyncer extends SyncStrategy {

    private static final int BULK_INSERT_BATCH_SIZE = 500;
    private static final String REQUEST_NO_BACKOFF = "0";

    private final CollectionStorage mCollectionStorage;
    private final UserAssociationStorage mUserAssociationStorage;
    private int mBulkInsertBatchSize = BULK_INSERT_BATCH_SIZE;

    public UserAssociationSyncer(Context context, ContentResolver resolver) {
        this(context, resolver, new UserAssociationStorage(resolver));
    }

    public UserAssociationSyncer(Context context, ContentResolver resolver, UserAssociationStorage userAssociationStorage) {
        super(context, resolver);
        mCollectionStorage = new CollectionStorage();
        mUserAssociationStorage = userAssociationStorage;
    }

    public void setBulkInsertBatchSize(int bulkInsertBatchSize) {
        mBulkInsertBatchSize = bulkInsertBatchSize;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@NotNull Uri uri, @Nullable String action) throws IOException {
        if (action != null && action.equals(ApiSyncService.ACTION_PUSH)) {
            return pushUserAssociations(Content.match(uri));
        } else {
            return syncLocalToRemote(Content.match(uri), SoundCloudApplication.getUserId());
        }
    }

    private ApiSyncResult syncLocalToRemote(Content content, final long userId) throws IOException {
        ApiSyncResult result = new ApiSyncResult(content.uri);
        if (!Content.ID_BASED.contains(content)) return result;

        List<Long> local = mUserAssociationStorage.getStoredIds(content.uri);
        List<Long> remote = mApi.readFullCollection(Request.to(content.remoteUri + "/ids"), IdHolder.class);

        log("Cloud Api service: got remote ids " + remote.size() + " vs [local] " + local.size());
        result.setSyncData(System.currentTimeMillis(), remote.size(), null);

        if (checkUnchanged(content, result, local, remote)) return result;

        // deletions can happen here, has no impact
        List<Long> itemDeletions = new ArrayList<Long>(local);
        itemDeletions.removeAll(remote);
        mUserAssociationStorage.deleteAssociations(content.uri, itemDeletions);

        int startPosition = 1;
        int added = 0;
        switch (content) {
            case ME_FOLLOWERS:
            case ME_FOLLOWINGS:
                // load the first page of items to get proper last_seen ordering
                // parse and add first items
                List<ScResource> resources = mApi.readList(Request.to(content.remoteUri)
                        .add(Wrapper.LINKED_PARTITIONING, "1")
                        .add("limit", Consts.COLLECTION_PAGE_SIZE));

                added = mUserAssociationStorage.insertAssociations(resources, content.uri, userId);

                // remove items from master remote list and adjust start index
                for (ScResource u : resources) {
                    remote.remove(u.id);
                }
                startPosition = resources.size();
                break;
            case ME_FRIENDS:
                // sync all friends. It is the only way ordering works properly
                added = mCollectionStorage.fetchAndStoreMissingCollectionItems(
                        mApi,
                        remote,
                        Content.USERS,
                        false
                );
                break;
        }

        log("Added " + added + " new items for this endpoint");
        mUserAssociationStorage.insertInBatches(content, userId, remote, startPosition, mBulkInsertBatchSize);
        result.success = true;
        return result;
    }

    private ApiSyncResult pushUserAssociations(Content content) {
        ApiSyncResult result = new ApiSyncResult(content.uri);
        result.success = true;
        if (content == Content.ME_FOLLOWINGS && mUserAssociationStorage.hasFollowingsNeedingSync()) {
            for (UserAssociation userAssociation : mUserAssociationStorage.getFollowingsNeedingSync()) {
                if (!pushUserAssociation(userAssociation)) result.success = false;
            }
        }
        return result;
    }

    /* package */ boolean pushUserAssociation(UserAssociation a) {
        final Request request = Request.to(Endpoints.MY_FOLLOWING, a.getUser().id);
        try {
            final boolean success;

            switch (a.getLocalSyncState()) {
                case PENDING_ADDITION:
                    int status = mApi.put(request).getStatusLine().getStatusCode();
                    success = status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED;
                    break;

                case PENDING_REMOVAL:
                    status = mApi.delete(request).getStatusLine().getStatusCode();
                    success = status == HttpStatus.SC_OK || status == HttpStatus.SC_NOT_FOUND;
                    break;

                default:
                    // no flags, no op.
                    return true;
            }
            if (success) mUserAssociationStorage.setFollowingAsSynced(a);
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
}
