package com.soundcloud.android.collections;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.likes.LoadLikedTrackUrnsCommand;
import com.soundcloud.android.likes.PlaylistLikesStorage;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistPostStorage;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func2;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

class CollectionsOperations {

    public static final Comparator<PropertySet> POSTED_AND_LIKED_COMPARATOR = new Comparator<PropertySet>() {
        @Override
        public int compare(PropertySet lhs, PropertySet rhs) {
            // flipped as we want reverse chronological order
            return getAssociationDate(rhs).compareTo(getAssociationDate(lhs));
        }

        private Date getAssociationDate(PropertySet propertySet) {
            return propertySet.contains(LikeProperty.CREATED_AT) ? propertySet.get(LikeProperty.CREATED_AT) : propertySet.get(PostProperty.CREATED_AT);
        }
    };

    @VisibleForTesting
    static final int PLAYLIST_LIMIT = 1000; //arbitrarily high, we don't want to worry about paging

    private final Scheduler scheduler;
    private final PlaylistPostStorage playlistPostStorage;
    private final PlaylistLikesStorage playlistLikesStorage;
    private final LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand;

    private static Func2<List<PropertySet>, List<PropertySet>, List<PropertySet>> COMBINE_POSTED_AND_LIKED = new Func2<List<PropertySet>, List<PropertySet>, List<PropertySet>>() {
        @Override
        public List<PropertySet> call(List<PropertySet> postedPlaylists, List<PropertySet> likedPlaylists) {
            List<PropertySet> all = new ArrayList<>(postedPlaylists.size() + likedPlaylists.size());
            all.addAll(postedPlaylists);
            all.addAll(likedPlaylists);
            Collections.sort(all, POSTED_AND_LIKED_COMPARATOR);
            return all;
        }
    };

    private static Func2<List<PlaylistItem>, List<Urn>, MyCollections> COMBINE_LIKES_AND_PLAYLISTS = new Func2<List<PlaylistItem>, List<Urn>, MyCollections>() {
        @Override
        public MyCollections call(List<PlaylistItem> playlistItems, List<Urn> urns) {
            return new MyCollections(urns.size(), playlistItems);
        }
    };

    @Inject
    CollectionsOperations(@Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                          PlaylistPostStorage playlistPostStorage,
                          PlaylistLikesStorage playlistLikesStorage,
                          LoadLikedTrackUrnsCommand loadLikedTrackUrnsCommand) {
        this.scheduler = scheduler;
        this.playlistPostStorage = playlistPostStorage;
        this.playlistLikesStorage = playlistLikesStorage;
        this.loadLikedTrackUrnsCommand = loadLikedTrackUrnsCommand;
    }

    Observable<MyCollections> collections() {
        return postedAndLikedPlaylists().zipWith(loadLikedTrackUrnsCommand.toObservable().subscribeOn(scheduler),
                COMBINE_LIKES_AND_PLAYLISTS);
    }

    private Observable<List<PlaylistItem>> postedAndLikedPlaylists() {
        final Observable<List<PropertySet>> loadLikedPlaylists = playlistLikesStorage.loadLikedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE);
        final Observable<List<PropertySet>> loadPostedPlaylists = playlistPostStorage.loadPostedPlaylists(PLAYLIST_LIMIT, Long.MAX_VALUE);
        return loadPostedPlaylists.zipWith(loadLikedPlaylists, COMBINE_POSTED_AND_LIKED)
                .map(PlaylistItem.fromPropertySets())
                .subscribeOn(scheduler);
    }

}
