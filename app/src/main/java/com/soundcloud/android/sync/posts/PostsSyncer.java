package com.soundcloud.android.sync.posts;

import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
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

    public PostsSyncer(LoadLocalPostsCommand loadLocalPosts,
                       FetchPostsCommand fetchRemotePosts,
                       StorePostsCommand storePostsCommand,
                       RemovePostsCommand removePostsCommand,
                       BulkFetchCommand<ApiModel> fetchPostResources,
                       WriteStorageCommand storePostResources) {
        this.loadLocalPosts = loadLocalPosts;
        this.fetchRemotePosts = fetchRemotePosts;
        this.storePostsCommand = storePostsCommand;
        this.removePostsCommand = removePostsCommand;
        this.fetchPostResources = fetchPostResources;
        this.storePostResources = storePostResources;
    }

    @Override
    public Boolean call() throws Exception {
        final Set<PropertySet> localPosts = new TreeSet<>(PostProperty.COMPARATOR);
        localPosts.addAll(loadLocalPosts.call());

        final NavigableSet<PropertySet> remotePosts = fetchRemotePosts.call();

        Log.d(TAG, "Syncing Posts : Local Count = " + localPosts.size() + " , Remote Count = " + remotePosts.size());

        final Set<PropertySet> additions = getSetDifference(remotePosts, localPosts);
        final Set<PropertySet> removals = getSetDifference(localPosts, remotePosts);

        if (additions.isEmpty() && removals.isEmpty()){
            Log.d(TAG, "Returning with no change");
            return false;
        } else {
            if (!removals.isEmpty()){
                Log.d(TAG, "Removing items " + TextUtils.join(",", removals));
                removePostsCommand.with(removals).call();
            }
            if (!additions.isEmpty()){
                Log.d(TAG, "Adding items " + TextUtils.join(",", additions));
                fetchResourcesForAdditions(additions);
                storePostsCommand.with(additions).call();
            }
            return true;
        }
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
