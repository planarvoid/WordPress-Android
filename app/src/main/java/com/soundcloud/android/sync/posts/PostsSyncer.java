package com.soundcloud.android.sync.posts;

import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.collections.Sets;
import com.soundcloud.rx.eventbus.EventBus;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
    private final BulkFetchCommand<ApiModel> fetchPostResources;
    private final WriteStorageCommand storePostResources;
    private final EventBus eventBus;

    public PostsSyncer(LoadLocalPostsCommand loadLocalPosts,
                       FetchPostsCommand fetchRemotePosts,
                       StorePostsCommand storePostsCommand,
                       RemovePostsCommand removePostsCommand,
                       BulkFetchCommand<ApiModel> fetchPostResources,
                       WriteStorageCommand storePostResources, EventBus eventBus) {
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
        return call(Collections.<Urn>emptyList());
    }

    public Boolean call(List<Urn> recentlyPostedUrns) throws Exception {
        final Set<PropertySet> localPosts = new TreeSet<>(PostProperty.COMPARATOR);
        localPosts.addAll(loadLocalPosts.call());

        final NavigableSet<PropertySet> remotePosts = fetchRemotePosts.call();

        Log.d(TAG, "Syncing Posts : Local Count = " + localPosts.size() + " , Remote Count = " + remotePosts.size());

        final Set<PropertySet> additions = getSetDifference(remotePosts, localPosts);
        final Set<PropertySet> removals = getSetDifference(localPosts, remotePosts);

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

            publishStateChanged(additions, removals);
            return true;
        }
    }

    private void removeRecentPosts(Set<PropertySet> posts, List<Urn> recentlyPostedUrns) {
        Iterator<PropertySet> iterator = posts.iterator();
        while (iterator.hasNext()) {
            PropertySet post = iterator.next();
            if (recentlyPostedUrns.contains(post.get(PostProperty.TARGET_URN))) {
                iterator.remove();
            }
        }
    }

    private void publishStateChanged(Set<PropertySet> additions, Set<PropertySet> removals) {
        final Set<PropertySet> changedEntities = Sets.newHashSetWithExpectedSize(additions.size() + removals.size());
        changedEntities.addAll(createChangedEntities(additions, true));
        changedEntities.addAll(createChangedEntities(removals, false));
        if (!changedEntities.isEmpty()) {
            eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromSync(changedEntities));
        }
    }

    private Set<PropertySet> createChangedEntities(Set<PropertySet> posts, boolean isAdded) {
        Set<PropertySet> changedEntities = Sets.newHashSetWithExpectedSize(posts.size());
        for (PropertySet post : posts) {
            final boolean isUserRepost = post.get(PostProperty.IS_REPOST) && isAdded;
            changedEntities.add(PropertySet.from(
                    PlayableProperty.URN.bind(post.get(PlayableProperty.URN)),
                    PlayableProperty.IS_USER_REPOST.bind(isUserRepost)
            ));
        }
        return changedEntities;
    }

    private void fetchResourcesForAdditions(Set<PropertySet> additions) throws Exception {
        final ArrayList<Urn> urns = new ArrayList<>(additions.size());
        for (PropertySet like : additions) {
            urns.add(like.get(LikeProperty.TARGET_URN));
        }
        final Collection<ApiModel> apiResources = fetchPostResources.with(urns).call();
        storePostResources.call(apiResources);
    }

    private Set<PropertySet> getSetDifference(Set<PropertySet> set, Set<PropertySet>... without) {
        final Set<PropertySet> difference = new TreeSet<>(PostProperty.COMPARATOR);
        difference.addAll(set);
        for (Set<PropertySet> s : without) {
            difference.removeAll(s);
        }
        return difference;
    }
}
