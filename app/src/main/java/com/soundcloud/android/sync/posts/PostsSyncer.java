package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.events.RepostsStatusEvent.RepostStatus.createReposted;
import static com.soundcloud.android.events.RepostsStatusEvent.RepostStatus.createUnposted;

import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

public class PostsSyncer<ApiModel> implements Callable<Boolean> {

    private static final String TAG = "PostsSyncer";

    private final LoadLocalPostsCommand loadLocalPosts;
    private final FetchPostsCommand fetchRemotePosts;
    private final StorePostsCommand storePostsCommand;
    private final RemovePostsCommand removePostsCommand;
    private final BulkFetchCommand<ApiModel, ApiModel> fetchPostResources;
    private final WriteStorageCommand storePostResources;
    private final EventBus eventBus;

    public PostsSyncer(LoadLocalPostsCommand loadLocalPosts,
                       FetchPostsCommand fetchRemotePosts,
                       StorePostsCommand storePostsCommand,
                       RemovePostsCommand removePostsCommand,
                       BulkFetchCommand<ApiModel, ApiModel> fetchPostResources,
                       WriteStorageCommand storePostResources,
                       EventBus eventBus) {
        this.loadLocalPosts = loadLocalPosts;
        this.fetchRemotePosts = fetchRemotePosts;
        this.storePostsCommand = storePostsCommand;
        this.removePostsCommand = removePostsCommand;
        this.fetchPostResources = fetchPostResources;
        this.storePostResources = storePostResources;
        this.eventBus = eventBus;
    }

    @Override
    public Boolean call() throws Exception {
        return call(Collections.emptyList());
    }

    public Boolean call(List<Urn> recentlyPostedUrns) throws Exception {
        final NavigableSet<PostRecord> remotePosts = fetchRemotePosts.call();
        final Set<PostRecord> localPosts = new TreeSet<>(PostRecord.COMPARATOR);
        localPosts.addAll(loadLocalPosts.call());

        Log.d(TAG, "Syncing Posts : Local Count = " + localPosts.size() + " , Remote Count = " + remotePosts.size());

        final Set<PostRecord> additions = getSetDifference(remotePosts, localPosts);
        final Set<PostRecord> removals = getSetDifference(localPosts, remotePosts);

        // A race condition occurs when a recently posted playlist is not returned
        // by the server immediately, leading to remove the recently created local
        // playlist from the database.
        removeRecentPosts(additions, recentlyPostedUrns);
        removeRecentPosts(removals, recentlyPostedUrns);

        if (additions.isEmpty() && removals.isEmpty()) {
            Log.d(TAG, "Returning with no change");
            return false;
        } else {
            if (!removals.isEmpty()) {
                Log.d(TAG, "Removing items " + TextUtils.join(",", removals));
                removePostsCommand.call(removals);
            }
            if (!additions.isEmpty()) {
                Log.d(TAG, "Adding items " + TextUtils.join(",", additions));
                fetchResourcesForAdditions(additions);
                storePostsCommand.call(additions);
            }

            publishStateChanges(additions, true);
            publishStateChanges(removals, false);

            return true;
        }
    }

    private void removeRecentPosts(Set<PostRecord> posts, List<Urn> recentlyPostedUrns) {
        Iterator<PostRecord> iterator = posts.iterator();
        while (iterator.hasNext()) {
            PostRecord post = iterator.next();
            if (recentlyPostedUrns.contains(post.getTargetUrn())) {
                iterator.remove();
            }
        }
    }

    private void publishStateChanges(Set<PostRecord> changes, boolean isAddition) {
        final Map<Urn, RepostsStatusEvent.RepostStatus> updatedEntities = new HashMap<>(changes.size());
        final Set<Urn> newEntities = new HashSet<>(changes.size());

        for (PostRecord post : changes) {
            if (post.isRepost()) {
                final Urn urn = post.getTargetUrn();
                updatedEntities.put(urn, isAddition ? createReposted(urn) : createUnposted(urn));
            } else {
                newEntities.add(post.getTargetUrn());
            }
        }

        if (!updatedEntities.isEmpty()) {
            eventBus.publish(EventQueue.REPOST_CHANGED, RepostsStatusEvent.create(updatedEntities));
        }

        if (!newEntities.isEmpty()) {
            eventBus.publish(EventQueue.URN_STATE_CHANGED, isAddition ? UrnStateChangedEvent.fromEntitiesCreated(newEntities) : UrnStateChangedEvent.fromEntitiesDeleted(newEntities));
        }
    }

    private void fetchResourcesForAdditions(Set<PostRecord> additions) throws Exception {
        final ArrayList<Urn> urns = new ArrayList<>(additions.size());
        for (PostRecord like : additions) {
            urns.add(like.getTargetUrn());
        }
        final Collection<ApiModel> apiResources = fetchPostResources.with(urns).call();
        storePostResources.call(apiResources);
    }

    private Set<PostRecord> getSetDifference(Set<PostRecord> set, Set<PostRecord> without) {
        final Set<PostRecord> difference = new TreeSet<>(PostRecord.COMPARATOR);
        difference.addAll(set);
        difference.removeAll(without);
        return difference;
    }
}
