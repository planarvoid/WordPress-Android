package com.soundcloud.android.profile;

import static com.soundcloud.android.profile.StoreProfileCommand.TO_RECORD_HOLDERS;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.model.ApiUserProfileInfo;
import com.soundcloud.android.api.model.PagedCollection;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.collection.LoadPlaylistLikedStatuses;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UserChangedEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.android.users.UserProfileInfo;
import com.soundcloud.android.users.UserRepository;
import com.soundcloud.java.collections.Lists;
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
    private final EventBus eventBus;

    private <T extends PlayableItem> PagedRemoteCollection<T> mergePlayableInfo(PagedRemoteCollection<T> input) {
        final Map<Urn, Boolean> playlistsIsLikedStatus = loadPlaylistLikedStatuses.call(Lists.transform(input.items().getCollection(), PlayableItem::getUrn));
        for (final PlayableItem resultItem : input) {
            final Urn itemUrn = resultItem.getUrn();
            if (playlistsIsLikedStatus.containsKey(itemUrn)) {
                resultItem.setLikedByCurrentUser(playlistsIsLikedStatus.get(itemUrn));
            }
        }
        return input;
    }

    private static final Func2<PagedRemoteCollection<PlayableItem>, User, PagedRemoteCollection<PlayableItem>> MERGE_REPOSTER =
            (remoteCollection, userItem) -> {
                for (PlayableItem post : remoteCollection) {
                    if (post.isRepost()) {
                        post.setReposter(userItem.username());
                        post.setReposterUrn(userItem.urn());
                    }
                }
                return remoteCollection;
            };

    @Inject
    UserProfileOperations(ProfileApi profileApi,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                          LoadPlaylistLikedStatuses loadPlaylistLikedStatuses,
                          UserRepository userRepository,
                          WriteMixedRecordsCommand writeMixedRecordsCommand,
                          StoreProfileCommand storeProfileCommand,
                          StoreUsersCommand storeUsersCommand,
                          SpotlightItemStatusLoader spotlightItemStatusLoader,
                          EventBus eventBus) {
        this.profileApi = profileApi;
        this.scheduler = scheduler;
        this.loadPlaylistLikedStatuses = loadPlaylistLikedStatuses;
        this.userRepository = userRepository;
        this.writeMixedRecordsCommand = writeMixedRecordsCommand;
        this.storeProfileCommand = storeProfileCommand;
        this.storeUsersCommand = storeUsersCommand;
        this.spotlightItemStatusLoader = spotlightItemStatusLoader;
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

    Observable<PagedRemoteCollection<PlayableItem>> pagedPostItems(Urn user) {
        return profileApi
                .userPosts(user)
                .doOnNext(posts ->  writeMixedRecordsCommand.call(TO_RECORD_HOLDERS(posts)))
                .map(posts -> posts.transform(PlayableItem::from))
                .map(PagedRemoteCollection::new)
                .map(UserProfileOperations.this::mergePlayableInfo)
                .zipWith(userRepository.userInfo(user), MERGE_REPOSTER)
                .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection<PlayableItem>> postsPagingFunction(final Urn user) {
        return pagingFunction(nextPageLink ->
                                      profileApi.userPosts(nextPageLink)
                                                .doOnNext(posts -> writeMixedRecordsCommand.call(TO_RECORD_HOLDERS(posts)))
                                                .map(posts -> posts.transform(PlayableItem::from))
                                                .map(PagedRemoteCollection::new)
                                                .map(UserProfileOperations.this::mergePlayableInfo)
                                                .zipWith(userRepository.userInfo(user), MERGE_REPOSTER)
                                                .subscribeOn(scheduler));
    }

    Observable<List<PropertySet>> postsForPlayback(final List<? extends PlayableItem> playableItems) {
        return Observable.fromCallable(() -> {
            final List<PropertySet> postsForPlayback = new ArrayList<>();
            for (PlayableItem playableItem : playableItems) {
                postsForPlayback.add(createPostForPlayback(playableItem));
            }
            return postsForPlayback;
        });
    }

    private PropertySet createPostForPlayback(PlayableItem playableItem) {
        final PropertySet postForPlayback = PropertySet.from(
                EntityProperty.URN.bind(playableItem.getUrn())
        );
        if (playableItem.getReposterUrn().isPresent()) {
            postForPlayback.put(PostProperty.REPOSTER_URN, playableItem.getReposterUrn().get());
        }
        return postForPlayback;
    }

    Observable<PagedRemoteCollection<PlaylistItem>> userPlaylists(Urn user) {
        return profileApi.userPlaylists(user)
                         .doOnNext(writeMixedRecordsCommand.toAction1())
                         .map(posts -> posts.transform(post -> PlaylistItem.from(post.getApiPlaylist(), false)))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    Observable<PagedRemoteCollection<PlaylistItem>> userPlaylists(String nextPageLink) {
        return profileApi.userPlaylists(nextPageLink)
                         .doOnNext(writeMixedRecordsCommand.toAction1())
                         .map(playlists -> playlists.transform(playlist -> PlaylistItem.from(playlist.getApiPlaylist())))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    Observable<PagedRemoteCollection<UserItem>> pagedFollowings(Urn user) {
        return profileApi
                .userFollowings(user)
                .doOnNext(writeMixedRecordsCommand.toAction1())
                .map(users -> users.transform(UserItem::from))
                .map(PagedRemoteCollection::new)
                .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection<UserItem>> followingsPagingFunction() {
        return pagingFunction(nextPageLink ->
                                      profileApi.userFollowings(nextPageLink)
                                                .doOnNext(writeMixedRecordsCommand.toAction1())
                                                .map(users -> users.transform(UserItem::from))
                                                .map(PagedRemoteCollection::new)
                                                .subscribeOn(scheduler));
    }

    public Observable<UserProfile> userProfile(final Urn user) {
        return profileApi.userProfile(user)
                         .doOnNext(storeProfileCommand.toAction1())
                         .doOnNext(publishEntityChangedFromProfile())
                         .map(UserProfile::fromUserProfileRecord)
                         .doOnNext(spotlightItemStatusLoader.toAction1())
                         .subscribeOn(scheduler);
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
                         .map(reposts -> reposts.transform(repost -> PlayableItem.from(repost, true)))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection<PlayableItem>> repostsPagingFunction() {
        return pagingFunction(nextPageLink -> profileApi.userReposts(nextPageLink)
                                                        .doOnNext(reposts -> writeMixedRecordsCommand.call(
                                                                TO_RECORD_HOLDERS(reposts)))
                                                        .map(reposts -> reposts.transform(repost -> PlayableItem.from(
                                                                repost,
                                                                true)))
                                                        .map(PagedRemoteCollection::new)
                                                        .map(UserProfileOperations.this::mergePlayableInfo)
                                                        .subscribeOn(scheduler));
    }

    Observable<PagedRemoteCollection<PlayableItem>> userTracks(Urn user) {
        return profileApi.userTracks(user)
                         .doOnNext(posts ->  writeMixedRecordsCommand.call(TO_RECORD_HOLDERS(posts)))
                         .map(posts -> posts.transform(post -> PlayableItem.from(post, false)))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection<PlayableItem>> userTracksPagingFunction() {
        return pagingFunction(nextPageLink ->
                                      profileApi.userTracks(nextPageLink)
                                                .doOnNext(posts -> writeMixedRecordsCommand.call(TO_RECORD_HOLDERS(posts)))
                                                .map(posts -> posts.transform(post -> PlayableItem.from(post, false)))
                                                .map(PagedRemoteCollection::new)
                                                .map(UserProfileOperations.this::mergePlayableInfo)
                                                .subscribeOn(scheduler));
    }

    Observable<PagedRemoteCollection<PlaylistItem>> userAlbums(Urn user) {
        return profileApi.userAlbums(user)
                         .doOnNext(writeMixedRecordsCommand.toAction1())
                         .map(playlists -> playlists.transform(playlist -> PlaylistItem.from(playlist.getApiPlaylist())))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    Observable<PagedRemoteCollection<PlaylistItem>> userAlbums(String nextPageLink) {
        return profileApi.userAlbums(nextPageLink)
                  .doOnNext(writeMixedRecordsCommand.toAction1())
                  .map(playlists -> playlists.transform(playlist -> PlaylistItem.from(playlist.getApiPlaylist())))
                  .map(PagedRemoteCollection::new)
                  .map(UserProfileOperations.this::mergePlayableInfo)
                  .subscribeOn(scheduler);
    }

    Observable<PagedRemoteCollection<PlayableItem>> userLikes(Urn user) {
        return profileApi.userLikes(user)
                         .doOnNext(posts ->  writeMixedRecordsCommand.call(TO_RECORD_HOLDERS(posts)))
                         .map(posts -> posts.transform(PlayableItem::from))
                         .map(PagedRemoteCollection::new)
                         .map(UserProfileOperations.this::mergePlayableInfo)
                         .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection<PlayableItem>> likesPagingFunction() {
        return pagingFunction(nextPageLink ->
                                      profileApi.userLikes(nextPageLink)
                                                .doOnNext(likes -> writeMixedRecordsCommand.call(TO_RECORD_HOLDERS(likes)))
                                                .map(posts -> posts.transform(PlayableItem::from))
                                                .map(PagedRemoteCollection::new)
                                                .map(UserProfileOperations.this::mergePlayableInfo)
                                                .subscribeOn(scheduler));
    }

    Observable<PagedRemoteCollection<UserItem>> pagedFollowers(Urn user) {
        return profileApi
                .userFollowers(user)
                .doOnNext(writeMixedRecordsCommand.toAction1())
                .map(users -> users.transform(UserItem::from))
                .map(PagedRemoteCollection::new)
                .subscribeOn(scheduler);
    }

    PagingFunction<PagedRemoteCollection<UserItem>> followersPagingFunction() {
        return pagingFunction(nextPageLink ->
                 profileApi.userFollowers(nextPageLink)
                                 .doOnNext(writeMixedRecordsCommand.toAction1())
                                 .map(users -> users.transform(UserItem::from))
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
