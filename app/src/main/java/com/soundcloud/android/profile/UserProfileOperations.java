package com.soundcloud.android.profile;

import static com.soundcloud.android.api.model.PagedRemoteCollection.TO_PAGED_REMOTE_COLLECTION;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.model.PagedCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.Pager.PagingFunction;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class UserProfileOperations {

    private final ProfileApi profileApi;
    private final Scheduler scheduler;
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    private final UserRepository userRepository;
    private final WriteMixedRecordsCommand writeMixedRecordsCommand;
    private final StoreProfileCommand storeProfileCommand;
    private final SpotlightItemStatusLoader spotlightItemStatusLoader;
    private final EventBus eventBus;

    private final Func1<PagedRemoteCollection, PagedRemoteCollection> mergePlayableInfo =
            new Func1<PagedRemoteCollection, PagedRemoteCollection>() {
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

    private static final Func2<PagedRemoteCollection, PropertySet, PagedRemoteCollection> MERGE_REPOSTER =
            new Func2<PagedRemoteCollection, PropertySet, PagedRemoteCollection>() {
                @Override
                public PagedRemoteCollection call(PagedRemoteCollection remoteCollection, PropertySet propertySet) {
                    for (PropertySet post : remoteCollection) {
                        if (post.getOrElse(PostProperty.IS_REPOST, false)) {
                            post.put(PostProperty.REPOSTER, propertySet.get(UserProperty.USERNAME));
                            post.put(PostProperty.REPOSTER_URN, propertySet.get(UserProperty.URN));
                        }
                    }
                    return remoteCollection;
                }
            };

    private Func1<UserProfileRecord, UserProfile> TO_USER_PROFILE = new Func1<UserProfileRecord, UserProfile>() {
        @Override
        public UserProfile call(UserProfileRecord userProfileRecord) {
            return UserProfile.fromUserProfileRecord(userProfileRecord);
        }
    };

    @Inject
    UserProfileOperations(ProfileApi profileApi,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                          LoadPlaylistLikedStatuses loadPlaylistLikedStatuses,
                          UserRepository userRepository,
                          WriteMixedRecordsCommand writeMixedRecordsCommand,
                          StoreProfileCommand storeProfileCommand,
                          SpotlightItemStatusLoader spotlightItemStatusLoader,
                          EventBus eventBus) {
        this.profileApi = profileApi;
        this.scheduler = scheduler;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.userRepository = userRepository;
        this.writeMixedRecordsCommand = writeMixedRecordsCommand;
        this.storeProfileCommand = storeProfileCommand;
        this.spotlightItemStatusLoader = spotlightItemStatusLoader;
        this.eventBus = eventBus;
    }

    public Observable<ProfileUser> getLocalProfileUser(Urn user) {
        return userRepository.localUserInfo(user).map(ProfileUser::new);
    }

    Observable<ProfileUser> getLocalAndSyncedProfileUser(Urn user) {
        return userRepository.localAndSyncedUserInfo(user).map(ProfileUser::new);
    }

    Observable<ProfileUser> getSyncedProfileUser(Urn user) {
        return userRepository.syncedUserInfo(user).map(ProfileUser::new);
    }

    public Observable<PagedRemoteCollection> pagedPostItems(Urn user) {
        return profileApi
                .userPosts(user)
                .doOnNext(writeMixedRecordsCommand.toAction1())
                .map(TO_PAGED_REMOTE_COLLECTION)
                .map(mergePlayableInfo)
                .zipWith(userRepository.userInfo(user), MERGE_REPOSTER)
                .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection> postsPagingFunction(final Urn user) {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userPosts(nextPageLink)
                                 .doOnNext(writeMixedRecordsCommand.toAction1())
                                 .map(TO_PAGED_REMOTE_COLLECTION)
                                 .map(mergePlayableInfo)
                                 .zipWith(userRepository.userInfo(user), MERGE_REPOSTER)
                                 .subscribeOn(scheduler);
            }
        });
    }

    Observable<List<PropertySet>> postsForPlayback(final List<? extends PlayableItem> playableItems) {
        return Observable.fromCallable(new Callable<List<PropertySet>>() {
            @Override
            public List<PropertySet> call() throws Exception {
                final List<PropertySet> postsForPlayback = new ArrayList<>();
                for (PlayableItem playableItem : playableItems) {
                    postsForPlayback.add(createPostForPlayback(playableItem));
                }
                return postsForPlayback;
            }
        });
    }

    private PropertySet createPostForPlayback(PlayableItem playableItem) {
        final PropertySet postForPlayback = PropertySet.from(
                EntityProperty.URN.bind(playableItem.getUrn())
        );
        if (playableItem.isRepost()) {
            postForPlayback.put(PostProperty.REPOSTER_URN, playableItem.getReposterUrn());
        }
        return postForPlayback;
    }

    Observable<PagedRemoteCollection> userPlaylists(Urn user) {
        return profileApi.userPlaylists(user)
                         .doOnNext(writeMixedRecordsCommand.toAction1())
                         .map(TO_PAGED_REMOTE_COLLECTION)
                         .map(mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection> userPlaylistsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userPlaylists(nextPageLink)
                                 .doOnNext(writeMixedRecordsCommand.toAction1())
                                 .map(TO_PAGED_REMOTE_COLLECTION)
                                 .map(mergePlayableInfo)
                                 .subscribeOn(scheduler);
            }
        });
    }

    Observable<PagedRemoteCollection> pagedFollowings(Urn user) {
        return profileApi
                .userFollowings(user)
                .doOnNext(writeMixedRecordsCommand.toAction1())
                .map(TO_PAGED_REMOTE_COLLECTION)
                .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection> followingsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userFollowings(nextPageLink)
                                 .doOnNext(writeMixedRecordsCommand.toAction1())
                                 .map(TO_PAGED_REMOTE_COLLECTION)
                                 .subscribeOn(scheduler);
            }
        });
    }

    public Observable<UserProfile> userProfile(final Urn user) {
        return profileApi.userProfile(user)
                         .cast(UserProfileRecord.class)
                         .doOnNext(storeProfileCommand.toAction1())
                         .map(TO_USER_PROFILE)
                         .doOnNext(spotlightItemStatusLoader.toAction1())
                         .doOnNext(publishEntityChanged())
                         .subscribeOn(scheduler);
    }

    Observable<PagedRemoteCollection> userReposts(Urn user) {
        return profileApi.userReposts(user)
                         .doOnNext(writeMixedRecordsCommand.toAction1())
                         .map(TO_PAGED_REMOTE_COLLECTION)
                         .map(mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection> repostsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userReposts(nextPageLink)
                                 .doOnNext(writeMixedRecordsCommand.toAction1())
                                 .map(TO_PAGED_REMOTE_COLLECTION)
                                 .map(mergePlayableInfo)
                                 .subscribeOn(scheduler);
            }
        });
    }

    Observable<PagedRemoteCollection> userTracks(Urn user) {
        return profileApi.userTracks(user)
                         .doOnNext(writeMixedRecordsCommand.toAction1())
                         .map(TO_PAGED_REMOTE_COLLECTION)
                         .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection> userTracksPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userTracks(nextPageLink)
                                 .doOnNext(writeMixedRecordsCommand.toAction1())
                                 .map(TO_PAGED_REMOTE_COLLECTION)
                                 .subscribeOn(scheduler);
            }
        });
    }

    Observable<PagedRemoteCollection> userAlbums(Urn user) {
        return profileApi.userAlbums(user)
                         .doOnNext(writeMixedRecordsCommand.toAction1())
                         .map(TO_PAGED_REMOTE_COLLECTION)
                         .map(mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection> userAlbumsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userAlbums(nextPageLink)
                                 .doOnNext(writeMixedRecordsCommand.toAction1())
                                 .map(TO_PAGED_REMOTE_COLLECTION)
                                 .map(mergePlayableInfo)
                                 .subscribeOn(scheduler);
            }
        });
    }

    Observable<PagedRemoteCollection> userLikes(Urn user) {
        return profileApi.userLikes(user)
                         .doOnNext(writeMixedRecordsCommand.toAction1())
                         .map(TO_PAGED_REMOTE_COLLECTION)
                         .map(mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection> likesPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userLikes(nextPageLink)
                                 .doOnNext(writeMixedRecordsCommand.toAction1())
                                 .map(TO_PAGED_REMOTE_COLLECTION)
                                 .map(mergePlayableInfo)
                                 .subscribeOn(scheduler);
            }
        });
    }

    Observable<PagedRemoteCollection> pagedFollowers(Urn user) {
        return profileApi
                .userFollowers(user)
                .doOnNext(writeMixedRecordsCommand.toAction1())
                .map(TO_PAGED_REMOTE_COLLECTION)
                .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection> followersPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userFollowers(nextPageLink)
                                 .doOnNext(writeMixedRecordsCommand.toAction1())
                                 .map(TO_PAGED_REMOTE_COLLECTION)
                                 .subscribeOn(scheduler);
            }
        });
    }

    private PagingFunction<PagedRemoteCollection> pagingFunction(final Command<String,
            Observable<PagedRemoteCollection>> nextPage) {
        return PagedCollection.pagingFunction(nextPage, scheduler);
    }

    private Action1<UserProfile> publishEntityChanged() {
        return new Action1<UserProfile>() {
            @Override
            public void call(UserProfile userProfile) {
                eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                                 EntityStateChangedEvent.forUpdate(userProfile.getUser()));
            }
        };
    }
}
