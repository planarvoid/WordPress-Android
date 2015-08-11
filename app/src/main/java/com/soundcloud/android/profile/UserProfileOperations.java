package com.soundcloud.android.profile;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.search.LoadPlaylistLikedStatuses;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.Pager;
import com.soundcloud.rx.Pager.PagingFunction;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;

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
    private final WriteMixedRecordsCommand writeMixedRecordsCommand;

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
                            post.put(PlayableProperty.REPOSTER, propertySet.get(UserProperty.USERNAME));
                            post.put(PlayableProperty.REPOSTER_URN, propertySet.get(UserProperty.URN));
                        }
                    }
                    return remoteCollection;
                }
            };

    @Inject
    UserProfileOperations(ProfileApi profileApi,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                          LoadPlaylistLikedStatuses loadPlaylistLikedStatuses,
                          UserRepository userRepository,
                          WriteMixedRecordsCommand writeMixedRecordsCommand) {
        this.profileApi = profileApi;
        this.scheduler = scheduler;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.userRepository = userRepository;
        this.writeMixedRecordsCommand = writeMixedRecordsCommand;
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
                .doOnNext(writeMixedRecordsCommand.toAction())
                .map(TO_PAGED_REMOTE_COLLECTION)
                .map(mergePlayableInfo)
                .zipWith(userRepository.userInfo(user), MERGE_REPOSTER)
                .subscribeOn(scheduler);
    }

    public PagingFunction<PagedRemoteCollection> postsPagingFunction(final Urn user) {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userPosts(nextPageLink)
                        .doOnNext(writeMixedRecordsCommand.toAction())
                        .map(TO_PAGED_REMOTE_COLLECTION)
                        .map(mergePlayableInfo)
                        .zipWith(userRepository.userInfo(user), MERGE_REPOSTER)
                        .subscribeOn(scheduler);
            }
        });
    }

    public Observable<List<PropertySet>> postsForPlayback(final List<? extends PlayableItem> playableItems) {
        return Observable.create(new Observable.OnSubscribe<List<PropertySet>>() {
            @Override
            public void call(Subscriber<? super List<PropertySet>> subscriber) {
                List<PropertySet> postsForPlayback = new ArrayList<>();
                for (PlayableItem playableItem : playableItems) {
                    if (playableItem.getEntityUrn().isTrack()) {
                        postsForPlayback.add(createPostForPlayback(playableItem));
                    }
                }
                subscriber.onNext(postsForPlayback);
                subscriber.onCompleted();
            }
        });
    }

    private PropertySet createPostForPlayback(PlayableItem playableItem) {
        final PropertySet postForPlayback = PropertySet.from(
                TrackProperty.URN.bind(playableItem.getEntityUrn())
        );
        if (playableItem.isRepost()) {
            postForPlayback.put(TrackProperty.REPOSTER_URN, playableItem.getReposterUrn());
        }
        return postForPlayback;
    }

    public Observable<PagedRemoteCollection> pagedLikes(Urn user) {
        return profileApi
                .userLikes(user)
                .doOnNext(writeMixedRecordsCommand.toAction())
                .map(TO_PAGED_REMOTE_COLLECTION)
                .map(mergePlayableInfo)
                .subscribeOn(scheduler);
    }

    public PagingFunction<PagedRemoteCollection> likesPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userLikes(nextPageLink)
                        .doOnNext(writeMixedRecordsCommand.toAction())
                        .map(TO_PAGED_REMOTE_COLLECTION)
                        .map(mergePlayableInfo)
                        .subscribeOn(scheduler);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedPlaylists(Urn user) {
        return profileApi
                .userPlaylists(user)
                .doOnNext(writeMixedRecordsCommand.toAction())
                .map(TO_PAGED_REMOTE_COLLECTION)
                .map(mergePlayableInfo)
                .subscribeOn(scheduler);
    }

    public PagingFunction<PagedRemoteCollection> playlistsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userPlaylists(nextPageLink)
                        .doOnNext(writeMixedRecordsCommand.toAction())
                        .map(TO_PAGED_REMOTE_COLLECTION)
                        .map(mergePlayableInfo)
                        .subscribeOn(scheduler);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedFollowings(Urn user) {
        return profileApi
                .userFollowings(user)
                .doOnNext(writeMixedRecordsCommand.toAction())
                .map(TO_PAGED_REMOTE_COLLECTION)
                .subscribeOn(scheduler);
    }

    public PagingFunction<PagedRemoteCollection> followingsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userFollowings(nextPageLink)
                        .doOnNext(writeMixedRecordsCommand.toAction())
                        .map(TO_PAGED_REMOTE_COLLECTION)
                        .subscribeOn(scheduler);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedFollowers(Urn user) {
        return profileApi
                .userFollowers(user)
                .doOnNext(writeMixedRecordsCommand.toAction())
                .map(TO_PAGED_REMOTE_COLLECTION)
                .subscribeOn(scheduler);
    }

    public PagingFunction<PagedRemoteCollection> followersPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userFollowers(nextPageLink)
                        .doOnNext(writeMixedRecordsCommand.toAction())
                        .map(TO_PAGED_REMOTE_COLLECTION)
                        .subscribeOn(scheduler);
            }
        });
    }

    private PagingFunction<PagedRemoteCollection>pagingFunction(final Command<String,
            Observable<PagedRemoteCollection>> nextPage) {
        return new PagingFunction<PagedRemoteCollection>() {
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
