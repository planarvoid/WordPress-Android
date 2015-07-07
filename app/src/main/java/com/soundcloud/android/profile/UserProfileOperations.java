package com.soundcloud.android.profile;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.playlists.PlaylistRecordHolder;
import com.soundcloud.android.search.LoadPlaylistLikedStatuses;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.tracks.TrackRecordHolder;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class UserProfileOperations {

    private static final Func1<ModelCollection<? extends PropertySetSource>, PagedRemoteCollection> TO_PAGED_REMOTE_COLLECTION =
            new Func1<ModelCollection<? extends PropertySetSource>, PagedRemoteCollection>() {
        @Override
        public PagedRemoteCollection call(ModelCollection<? extends PropertySetSource> modelCollection) {
            return new PagedRemoteCollection(modelCollection);
        }
    };

    private final ProfileApi profileApi;
    private final Scheduler scheduler;
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    private final UserRepository userRepository;
    private final StoreTracksCommand storeTracksCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreUsersCommand storeUsersCommand;

    private final Func1<PagedRemoteCollection, PagedRemoteCollection> mergePlayableInfo = new Func1<PagedRemoteCollection, PagedRemoteCollection>() {
        @Override
        public PagedRemoteCollection call(PagedRemoteCollection input) {
            final Map<Urn, PropertySet> playlistsIsLikedStatus = loadPlaylistLikedStatuses.call(input);
            for (final PropertySet resultItem : input) {
                final Urn itemUrn = resultItem.getOrElse(PlaylistProperty.URN, Urn.NOT_SET);
                if (playlistsIsLikedStatus.containsKey(itemUrn)) {
                    resultItem.update(playlistsIsLikedStatus.get(itemUrn));
                }
            }
            return input;
        }
    };

    private final Action1<ModelCollection<PropertySetSource>> writeMixedRecordsToStorage = new Action1<ModelCollection<PropertySetSource>>() {
        @Override
        public void call(ModelCollection<PropertySetSource> collection) {
            List<TrackRecord> tracks = new ArrayList<>();
            List<PlaylistRecord> playlists = new ArrayList<>();
            for (PropertySetSource entity : collection) {
                if (entity instanceof TrackRecordHolder) {
                    tracks.add(((TrackRecordHolder) entity).getTrackRecord());
                }
                if (entity instanceof PlaylistRecordHolder) {
                    playlists.add(((PlaylistRecordHolder) entity).getPlaylistRecord());
                }
            }
            if (!tracks.isEmpty()){
                storeTracksCommand.call(tracks);
            }
            if (!playlists.isEmpty()){
                storePlaylistsCommand.call(playlists);
            }
        }
    };

    @Inject
    UserProfileOperations(ProfileApi profileApi,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                          LoadPlaylistLikedStatuses loadPlaylistLikedStatuses,
                          UserRepository userRepository,
                          StoreTracksCommand storeTracksCommand,
                          StorePlaylistsCommand storePlaylistsCommand,
                          StoreUsersCommand storeUsersCommand) {
        this.profileApi = profileApi;
        this.scheduler = scheduler;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.userRepository = userRepository;
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
    }

    public Observable<ProfileUser> getLocalProfileUser(Urn user) {
        return userRepository.localUserInfo(user).map(new Func1<PropertySet, ProfileUser>() {
            @Override
            public ProfileUser call(PropertySet properties) {
                return new ProfileUser(properties);
            }
        });
    }

    public Observable<ProfileUser> getLocalAndSyncedProfileUser(Urn user) {
        return userRepository.localAndSyncedUserInfo(user).map(new Func1<PropertySet, ProfileUser>() {
            @Override
            public ProfileUser call(PropertySet properties) {
                return new ProfileUser(properties);
            }
        });
    }

    public Observable<ProfileUser> getSyncedProfileUser(Urn user) {
        return userRepository.syncedUserInfo(user).map(new Func1<PropertySet, ProfileUser>() {
            @Override
            public ProfileUser call(PropertySet properties) {
                return new ProfileUser(properties);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedPostItems(Urn user) {
        return profileApi
                .userPosts(user)
                .doOnNext(writeMixedRecordsToStorage)
                .map(TO_PAGED_REMOTE_COLLECTION)
                .map(mergePlayableInfo)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<PagedRemoteCollection> postsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userPosts(nextPageLink)
                        .doOnNext(writeMixedRecordsToStorage)
                        .map(TO_PAGED_REMOTE_COLLECTION)
                        .map(mergePlayableInfo)
                        .subscribeOn(scheduler);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedLikes(Urn user) {
        return profileApi
                .userLikes(user)
                .doOnNext(writeMixedRecordsToStorage)
                .map(TO_PAGED_REMOTE_COLLECTION)
                .map(mergePlayableInfo)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<PagedRemoteCollection> likesPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userLikes(nextPageLink)
                        .doOnNext(writeMixedRecordsToStorage)
                        .map(TO_PAGED_REMOTE_COLLECTION)
                        .map(mergePlayableInfo)
                        .subscribeOn(scheduler);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedPlaylists(Urn user) {
        return profileApi
                .userPlaylists(user)
                .doOnNext(storePlaylistsCommand.toAction())
                .map(TO_PAGED_REMOTE_COLLECTION)
                .map(mergePlayableInfo)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<PagedRemoteCollection> playlistsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userPlaylists(nextPageLink)
                        .doOnNext(storePlaylistsCommand.toAction())
                        .map(TO_PAGED_REMOTE_COLLECTION)
                        .map(mergePlayableInfo)
                        .subscribeOn(scheduler);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedFollowings(Urn user) {
        return profileApi
                .userFollowings(user)
                .doOnNext(storeUsersCommand.toAction())
                .map(TO_PAGED_REMOTE_COLLECTION)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<PagedRemoteCollection> followingsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userFollowings(nextPageLink)
                        .doOnNext(storeUsersCommand.toAction())
                        .map(TO_PAGED_REMOTE_COLLECTION)
                        .subscribeOn(scheduler);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedFollowers(Urn user) {
        return profileApi
                .userFollowers(user)
                .doOnNext(storeUsersCommand.toAction())
                .map(TO_PAGED_REMOTE_COLLECTION)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<PagedRemoteCollection> followersPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userFollowers(nextPageLink)
                        .doOnNext(storeUsersCommand.toAction())
                        .map(TO_PAGED_REMOTE_COLLECTION)
                        .subscribeOn(scheduler);
            }
        });
    }

    private Pager.PagingFunction<PagedRemoteCollection> pagingFunction(final Command<String, Observable<PagedRemoteCollection>> nextPage) {
        return new Pager.PagingFunction<PagedRemoteCollection>() {
            @Override
            public Observable<PagedRemoteCollection> call(PagedRemoteCollection collection) {
                final Optional<String> nextPageLink = collection.nextPageLink();
                if (nextPageLink.isPresent()) {
                    return nextPage.call(nextPageLink.get()).subscribeOn(scheduler);
                } else {
                    return Pager.finish();
                }
            }
        };
    }
}
