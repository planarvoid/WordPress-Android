package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.StoreProfileCommand.TO_RECORD_HOLDERS;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.model.ApiUserProfileInfo;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PagedCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UserChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.EntityItemCreator;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserProfileInfo;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.rx.Pager.PagingFunction;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UserProfileOperations {

    private final ProfileApi profileApi;
    private final Scheduler scheduler;
    private final LoadPlaylistLikedStatuses loadPlaylistLikedStatuses;
    private final UserRepository userRepository;
    private final WriteMixedRecordsCommand writeMixedRecordsCommand;
    private final StoreProfileCommand storeProfileCommand;
    private final StoreUsersCommand storeUsersCommand;
    private final SpotlightItemStatusLoader spotlightItemStatusLoader;
    private final EntityItemCreator entityItemCreator;
    private final EventBus eventBus;

    private PagedRemoteCollection<PlayableItem> mergePlayableInfo(PagedRemoteCollection<PlayableItem> input) {
        final Map<Urn, Boolean> playlistsIsLikedStatus = loadPlaylistLikedStatuses.call(Lists.transform(input.items().getCollection(), PlayableItem::getUrn));
        final List<PlayableItem> updatedList = new ArrayList<>();
        for (final PlayableItem resultItem : input) {
            final Urn itemUrn = resultItem.getUrn();
            if (playlistsIsLikedStatus.containsKey(itemUrn)) {
                updatedList.add(resultItem.updateLikeState(playlistsIsLikedStatus.get(itemUrn)));
            } else {
                updatedList.add(resultItem);
            }
        }
        return new PagedRemoteCollection<>(input.items().copyWithItems(updatedList));
    }

    private PagedRemoteCollection<PlaylistItem> mergePlaylistInfo(PagedRemoteCollection<PlaylistItem> input) {
        final Map<Urn, Boolean> playlistsIsLikedStatus = loadPlaylistLikedStatuses.call(Lists.transform(input.items().getCollection(), PlayableItem::getUrn));
        final List<PlaylistItem> updatedList = new ArrayList<>();
        for (final PlaylistItem resultItem : input) {
            final Urn itemUrn = resultItem.getUrn();
            if (playlistsIsLikedStatus.containsKey(itemUrn)) {
                updatedList.add(resultItem.updateLikeState(playlistsIsLikedStatus.get(itemUrn)));
            } else {
                updatedList.add(resultItem);
            }
        }
        return new PagedRemoteCollection<>(input.items().copyWithItems(updatedList));
    }

    @Inject
    UserProfileOperations(ProfileApi profileApi,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                          LoadPlaylistLikedStatuses loadPlaylistLikedStatuses,
                          UserRepository userRepository,
                          WriteMixedRecordsCommand writeMixedRecordsCommand,
                          StoreProfileCommand storeProfileCommand,
                          StoreUsersCommand storeUsersCommand,
                          SpotlightItemStatusLoader spotlightItemStatusLoader,
                          EntityItemCreator entityItemCreator,
                          EventBus eventBus) {
        this.profileApi = profileApi;
        this.scheduler = scheduler;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.userRepository = userRepository;
        this.writeMixedRecordsCommand = writeMixedRecordsCommand;
        this.storeProfileCommand = storeProfileCommand;
        this.storeUsersCommand = storeUsersCommand;
        this.spotlightItemStatusLoader = spotlightItemStatusLoader;
        this.entityItemCreator = entityItemCreator;
        this.eventBus = eventBus;
    }

    Observable<User> getLocalProfileUser(Urn user) {
        return userRepository.localUserInfo(user);
    }

    Observable<User> getLocalAndSyncedProfileUser(Urn user) {
        return userRepository.localAndSyncedUserInfo(user);
    }

    Observable<User> getSyncedProfileUser(Urn user) {
        return userRepository.syncedUserInfo(user);
    }

    Observable<PagedRemoteCollection<PlaylistItem>> userPlaylists(Urn user) {
        return profileApi.userPlaylists(user)
                         .doOnNext(writeMixedRecordsCommand.toAction1())
                         .map(posts -> posts.transform(entityItemCreator::playlistItem))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlaylistInfo)
                         .subscribeOn(scheduler);
    }

    Observable<PagedRemoteCollection<PlaylistItem>> userPlaylists(String nextPageLink) {
        return profileApi.userPlaylists(nextPageLink)
                         .doOnNext(writeMixedRecordsCommand.toAction1())
                         .map(playlists -> playlists.transform(entityItemCreator::playlistItem))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlaylistInfo)
                         .subscribeOn(scheduler);
    }

    Observable<PagedRemoteCollection<UserItem>> pagedFollowings(Urn user) {
        return profileApi
                .userFollowings(user)
                .doOnNext(writeMixedRecordsCommand.toAction1())
                .map(users -> users.transform(entityItemCreator::userItem))
                .map(PagedRemoteCollection::new)
                .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection<UserItem>> followingsPagingFunction() {
        return pagingFunction(nextPageLink ->
                                      profileApi.userFollowings(nextPageLink)
                                                .doOnNext(writeMixedRecordsCommand.toAction1())
                                                .map(users -> users.transform(entityItemCreator::userItem))
                                                .map(PagedRemoteCollection::new)
                                                .subscribeOn(scheduler));
    }

    public Observable<UserProfile> userProfile(final Urn user) {
        return profileApi.userProfile(user)
                         .doOnNext(storeProfileCommand.toAction1())
                         .doOnNext(publishEntityChangedFromProfile())
                         .map(this::fromApiUserProfile)
                         .doOnNext(spotlightItemStatusLoader.toAction1())
                         .subscribeOn(scheduler);
    }

    public UserProfile fromApiUserProfile(ApiUserProfile apiUserProfile){
        UserItem user = entityItemCreator.userItem(apiUserProfile.getUser());
        ModelCollection<PlayableItem> spotlight = apiUserProfile.getSpotlight().transform(entityItemCreator::playableItem);
        ModelCollection<TrackItem> tracks = apiUserProfile.getTracks().transform(entityItemCreator::trackItem);
        ModelCollection<PlaylistItem> albums = apiUserProfile.getAlbums().transform(entityItemCreator::playlistItem);
        ModelCollection<PlaylistItem> playlists = apiUserProfile.getPlaylists().transform(entityItemCreator::playlistItem);
        ModelCollection<PlayableItem> reposts = apiUserProfile.getReposts().transform(entityItemCreator::playableItem);
        ModelCollection<PlayableItem> likes = apiUserProfile.getLikes().transform(entityItemCreator::playableItem);
        return new UserProfile(user, spotlight, tracks, albums, playlists, reposts, likes);
    }

    public Observable<UserProfileInfo> userProfileInfo(final Urn user) {
        return profileApi.userProfileInfo(user)
                         .doOnNext(profileInfo -> storeUsersCommand.call(Collections.singleton(profileInfo.getUser())))
                         .doOnNext(publishEntityChangedFromProfileInfo())
                         .map(UserProfileInfo::fromApiUserProfileInfo)
                         .subscribeOn(scheduler);
    }

    Observable<PagedRemoteCollection<PlayableItem>> userReposts(Urn user) {
        return profileApi.userReposts(user)
                         .doOnNext(reposts ->  writeMixedRecordsCommand.call(TO_RECORD_HOLDERS(reposts)))
                         .map(reposts -> reposts.transform(entityItemCreator::playableItem))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection<PlayableItem>> repostsPagingFunction() {
        return pagingFunction(nextPageLink -> profileApi.userReposts(nextPageLink)
                                                        .doOnNext(reposts -> writeMixedRecordsCommand.call(
                                                                TO_RECORD_HOLDERS(reposts)))
                                                        .map(reposts -> reposts.transform(entityItemCreator::playableItem))
                                                        .map(PagedRemoteCollection::new)
                                                        .map(UserProfileOperations.this::mergePlayableInfo)
                                                        .subscribeOn(scheduler));
    }

    Observable<PagedRemoteCollection<PlayableItem>> userTracks(Urn user) {
        return profileApi.userTracks(user)
                         .doOnNext(posts ->  writeMixedRecordsCommand.call(TO_RECORD_HOLDERS(posts)))
                         .map(posts -> posts.transform(entityItemCreator::playableItem))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection<PlayableItem>> userTracksPagingFunction() {
        return pagingFunction(nextPageLink ->
                                      profileApi.userTracks(nextPageLink)
                                                .doOnNext(posts -> writeMixedRecordsCommand.call(TO_RECORD_HOLDERS(posts)))
                                                .map(posts -> posts.transform(entityItemCreator::playableItem))
                                                .map(PagedRemoteCollection::new)
                                                .map(UserProfileOperations.this::mergePlayableInfo)
                                                .subscribeOn(scheduler));
    }

    Observable<PagedRemoteCollection<PlaylistItem>> userAlbums(Urn user) {
        return profileApi.userAlbums(user)
                         .doOnNext(writeMixedRecordsCommand.toAction1())
                         .map(playlists -> playlists.transform(entityItemCreator::playlistItem))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlaylistInfo)
                         .subscribeOn(scheduler);
    }

    Observable<PagedRemoteCollection<PlaylistItem>> userAlbums(String nextPageLink) {
        return profileApi.userAlbums(nextPageLink)
                  .doOnNext(writeMixedRecordsCommand.toAction1())
                  .map(playlists -> playlists.transform(entityItemCreator::playlistItem))
                  .map(PagedRemoteCollection::new)
                  .map(UserProfileOperations.this::mergePlaylistInfo)
                  .subscribeOn(scheduler);
    }

    Observable<PagedRemoteCollection<PlayableItem>> userLikes(Urn user) {
        return profileApi.userLikes(user)
                         .doOnNext(posts ->  writeMixedRecordsCommand.call(TO_RECORD_HOLDERS(posts)))
                         .map(posts -> posts.transform(entityItemCreator::playableItem))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection<PlayableItem>> likesPagingFunction() {
        return pagingFunction(nextPageLink ->
                                      profileApi.userLikes(nextPageLink)
                                                .doOnNext(likes -> writeMixedRecordsCommand.call(TO_RECORD_HOLDERS(likes)))
                                                .map(posts -> posts.transform(entityItemCreator::playableItem))
                                                .map(PagedRemoteCollection::new)
                                                .map(UserProfileOperations.this::mergePlayableInfo)
                                                .subscribeOn(scheduler));
    }

    Observable<PagedRemoteCollection<UserItem>> pagedFollowers(Urn user) {
        return profileApi
                .userFollowers(user)
                .doOnNext(writeMixedRecordsCommand.toAction1())
                .map(users -> users.transform(entityItemCreator::userItem))
                .map(PagedRemoteCollection::new)
                .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection<UserItem>> followersPagingFunction() {
        return pagingFunction(nextPageLink ->
                 profileApi.userFollowers(nextPageLink)
                                 .doOnNext(writeMixedRecordsCommand.toAction1())
                                 .map(users -> users.transform(entityItemCreator::userItem))
                                 .map(PagedRemoteCollection::new)
                                 .subscribeOn(scheduler));
    }

    public <T> PagingFunction<PagedRemoteCollection<T>> pagingFunction(final Func1<String,
                Observable<PagedRemoteCollection<T>>> nextPage) {
        return PagedCollection.pagingFunction(nextPage, scheduler);
    }

    private Action1<ApiUserProfile> publishEntityChangedFromProfile() {
        return userProfile -> eventBus.publish(EventQueue.USER_CHANGED, UserChangedEvent.forUpdate(User.fromApiUser(userProfile.getUser())));
    }

    private Action1<ApiUserProfileInfo> publishEntityChangedFromProfileInfo() {
        return userProfileInfo -> eventBus.publish(EventQueue.USER_CHANGED, UserChangedEvent.forUpdate(User.fromApiUser(userProfileInfo.getUser())));
    }
}
