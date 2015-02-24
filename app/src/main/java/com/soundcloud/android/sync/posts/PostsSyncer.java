package com.soundcloud.android.sync.posts;

import com.soundcloud.propeller.PropertySet;

import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

public class PostsSyncer implements Callable<Boolean> {

    private final LoadLocalPostsCommand loadLocalPosts;
    private final FetchPostsCommand fetchRemotePosts;
    private final StorePostsCommand storePostsCommand;
    private final RemovePostsCommand removePostsCommand;

    public PostsSyncer(LoadLocalPostsCommand loadLocalPosts,
                       FetchPostsCommand fetchRemotePosts,
                       StorePostsCommand storePostsCommand,
                       RemovePostsCommand removePostsCommand) {
        this.loadLocalPosts = loadLocalPosts;
        this.fetchRemotePosts = fetchRemotePosts;
        this.storePostsCommand = storePostsCommand;
        this.removePostsCommand = removePostsCommand;
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
                storePostsCommand.with(additions).call();
            }
            return true;
        }
    }

    @SafeVarargs
    private final Set<PropertySet> getSetDifference(Set<PropertySet> set, Set<PropertySet>... without) {
        final Set<PropertySet> difference = new TreeSet<>(PostProperty.COMPARATOR);
        difference.addAll(set);
        for (Set<PropertySet> s : without) {
            difference.removeAll(s);
        }
        return difference;
    }
}
