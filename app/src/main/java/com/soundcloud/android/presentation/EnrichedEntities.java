package com.soundcloud.android.presentation;

import static com.soundcloud.java.collections.Maps.asMap;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.associations.FollowingStateProvider;
import com.soundcloud.android.associations.RepostsStateProvider;
import com.soundcloud.android.likes.LikesStateProvider;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Observable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnrichedEntities implements EntityItemEmitter {

    private final EntityItemCreator entityItemCreator;
    private final LikesStateProvider likesStateProvider;
    private final RepostsStateProvider repostsStateProvider;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final FollowingStateProvider followingStateProvider;

    @Inject
    public EnrichedEntities(EntityItemCreator entityItemCreator,
                            LikesStateProvider likesStateProvider,
                            RepostsStateProvider repostsStateProvider,
                            PlaySessionStateProvider playSessionStateProvider,
                            FollowingStateProvider followingStateProvider) {
        this.entityItemCreator = entityItemCreator;
        this.likesStateProvider = likesStateProvider;
        this.repostsStateProvider = repostsStateProvider;
        this.playSessionStateProvider = playSessionStateProvider;
        this.followingStateProvider = followingStateProvider;
    }

    @Override
    public Observable<List<TrackItem>> trackItems(List<ApiTrack> apiTracks) {
        List<TrackItem> items = Lists.transform(apiTracks, entityItemCreator::trackItem);
        return withTrackUpdates(items);
    }

    @Override
    public Observable<Map<Urn, TrackItem>> trackItemsAsMap(List<ApiTrack> apiTracks) {
        List<TrackItem> items = Lists.transform(apiTracks, entityItemCreator::trackItem);
        return withTrackUpdates(items).map(trackItems -> asMap(trackItems, TrackItem::getUrn));
    }

    @Override
    public Observable<List<PlaylistItem>> playlistItems(List<ApiPlaylist> apiPlaylists) {
        List<PlaylistItem> items = Lists.transform(apiPlaylists, entityItemCreator::playlistItem);
        return withPlaylistUpdates(items);
    }

    @Override
    public Observable<Map<Urn, PlaylistItem>> playlistItemsAsMap(List<ApiPlaylist> apiPlaylists) {
        List<PlaylistItem> items = Lists.transform(apiPlaylists, entityItemCreator::playlistItem);
        return withPlaylistUpdates(items).map(playlistItems -> asMap(playlistItems, PlaylistItem::getUrn));
    }

    @Override
    public Observable<List<UserItem>> userItems(List<ApiUser> apiUsers) {
        List<UserItem> items = Lists.transform(apiUsers, entityItemCreator::userItem);
        return withUserUpdates(items);
    }

    @Override
    public Observable<Map<Urn, UserItem>> userItemsAsMap(List<ApiUser> apiUsers) {
        List<UserItem> items = Lists.transform(apiUsers, entityItemCreator::userItem);
        return withUserUpdates(items).map(playlistItems -> asMap(playlistItems, UserItem::getUrn));
    }

    private Observable<List<TrackItem>> withTrackUpdates(List<TrackItem> items) {
        if (items.isEmpty()) {
            return Observable.empty();
        } else {
            return Observable.combineLatest(
                    Observable.just(items),
                    likesStateProvider.likedStatuses(),
                    repostsStateProvider.repostedStatuses(),
                    playSessionStateProvider.nowPlayingUrn(),
                    (trackItems, likedStatuses, repostStatuses, urn) -> {
                        List<TrackItem> updatedTrackItems = new ArrayList<>(trackItems.size());
                        for (TrackItem trackItem : trackItems) {
                            updatedTrackItems.add(trackItem.updatedWithLikeAndRepostStatus(likedStatuses.isLiked(trackItem.getUrn()),
                                                                                           repostStatuses.isReposted(trackItem.getUrn()))
                                                           .updateNowPlaying(urn));
                        }
                        return updatedTrackItems;
                    }
            ).distinctUntilChanged();
        }
    }

    private Observable<List<UserItem>> withUserUpdates(List<UserItem> items) {
        if (items.isEmpty()) {
            return Observable.empty();
        } else {
            return Observable.combineLatest(
                    Observable.just(items),
                    followingStateProvider.followingStatuses(),
                    (userItems, followStatuses) -> {
                        List<UserItem> updatedUserItems = new ArrayList<>(userItems.size());
                        for (UserItem userItem : userItems) {
                            updatedUserItems.add(userItem.copyWithFollowing(followStatuses.isFollowed(userItem.getUrn())));
                        }
                        return updatedUserItems;
                    }
            ).distinctUntilChanged();
        }
    }

    private Observable<List<PlaylistItem>> withPlaylistUpdates(List<PlaylistItem> items) {
        if (items.isEmpty()) {
            return Observable.empty();
        } else {
            return Observable.combineLatest(
                    Observable.just(items),
                    likesStateProvider.likedStatuses(),
                    repostsStateProvider.repostedStatuses(),
                    (playlistItems, likedStatuses, repostStatuses) -> {
                        List<PlaylistItem> updatedPlaylistItems = new ArrayList<>(playlistItems.size());
                        for (PlaylistItem playlistItem : playlistItems) {
                            updatedPlaylistItems.add(playlistItem.updatedWithLikeAndRepostStatus(likedStatuses.isLiked(playlistItem.getUrn()), repostStatuses.isReposted(playlistItem.getUrn())));
                        }
                        return updatedPlaylistItems;
                    }
            ).distinctUntilChanged();
        }
    }
}
