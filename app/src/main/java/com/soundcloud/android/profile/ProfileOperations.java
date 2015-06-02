package com.soundcloud.android.profile;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.model.PagedRemoteCollection;
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

    private final Pager.PagingFunction<PagedRemoteCollection> pagingFunction =
            new Pager.PagingFunction<PagedRemoteCollection>() {
        @Override
        public Observable<PagedRemoteCollection> call(PagedRemoteCollection collection) {
            if (collection.nextPageLink().isPresent()) {
                return profileApi.userPosts(collection.nextPageLink().get()).subscribeOn(scheduler);
            } else {
                return Pager.finish();
            }
        }
    };

    private final Func1<PagedRemoteCollection, PagedRemoteCollection> mergeLocalInfo = new Func1<PagedRemoteCollection, PagedRemoteCollection>() {
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

    public Observable<ProfileUser> getUserDetails(Urn user) {
        return userRepository.localAndSyncedUserInfo(user).map(new Func1<PropertySet, ProfileUser>() {
            @Override
            public ProfileUser call(PropertySet properties) {
                return new ProfileUser(properties);
            }
        });
    }

    public Observable<ProfileUser> updatedUserDetails(Urn user) {
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
                .map(mergeLocalInfo)
                .subscribeOn(scheduler);
    }

    public Pager.PagingFunction<PagedRemoteCollection> pagingFunction() {
        return pagingFunction;
    }
}
