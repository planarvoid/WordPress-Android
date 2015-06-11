package com.soundcloud.android.profile;

import com.google.common.base.Optional;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.rx.Pager;
import com.soundcloud.android.search.LoadPlaylistLikedStatuses;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

public class ProfileOperations {

    private final ProfileApi profileApi;
    private final Scheduler scheduler;
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    private final UserRepository userRepository;

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

    @Inject
    public ProfileOperations(ProfileApi profileApi, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                             LoadPlaylistLikedStatuses loadPlaylistLikedStatuses, UserRepository userRepository) {
        this.profileApi = profileApi;
        this.scheduler = scheduler;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.userRepository = userRepository;
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
                .map(mergePlayableInfo)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<PagedRemoteCollection> postsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userPosts(nextPageLink);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedLikes(Urn user) {
        return profileApi
                .userLikes(user)
                .map(mergePlayableInfo)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<PagedRemoteCollection> likesPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userLikes(nextPageLink);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedPlaylists(Urn user) {
        return profileApi
                .userPlaylists(user)
                .map(mergePlayableInfo)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<PagedRemoteCollection> playlistsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userPlaylists(nextPageLink);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedFollowings(Urn user) {
        return profileApi
                .userFollowings(user)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<PagedRemoteCollection> followingsPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userFollowings(nextPageLink);
            }
        });
    }

    public Observable<PagedRemoteCollection> pagedFollowers(Urn user) {
        return profileApi
                .userFollowers(user)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<PagedRemoteCollection> followersPagingFunction() {
        return pagingFunction(new Command<String, Observable<PagedRemoteCollection>>() {
            @Override
            public Observable<PagedRemoteCollection> call(String nextPageLink) {
                return profileApi.userFollowers(nextPageLink);
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
