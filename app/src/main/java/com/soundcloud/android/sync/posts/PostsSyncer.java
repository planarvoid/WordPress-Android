package com.soundcloud.android.sync.posts;

import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

public class PostsSyncer<ApiModel> implements Callable<Boolean> {

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
        final Set<PropertySet> localPlaylists = new TreeSet<>(PostProperty.COMPARATOR);
        localPlaylists.addAll(loadLocalPosts.call());

        final NavigableSet<PropertySet> remotePlaylists = fetchRemotePosts.call();

        final Set<PropertySet> additions = getSetDifference(remotePlaylists, localPlaylists);
        final Set<PropertySet> removals = getSetDifference(localPlaylists, remotePlaylists);

        if (additions.isEmpty() && removals.isEmpty()){
            return false;
        } else {

            if (!removals.isEmpty()){
                removePostsCommand.with(removals).call();
            }
            if (!additions.isEmpty()){
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
