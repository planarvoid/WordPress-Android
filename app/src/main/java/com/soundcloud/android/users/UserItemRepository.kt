package com.soundcloud.android.users

import com.soundcloud.android.api.model.ApiUser
import com.soundcloud.android.model.Urn
import com.soundcloud.android.presentation.EntityItemCreator
import com.soundcloud.android.utils.OpenForTesting
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Maybes
import io.reactivex.rxkotlin.Singles
import javax.inject.Inject

@OpenForTesting
class UserItemRepository
@Inject
constructor(val userRepository: UserRepository, val followingStorage: FollowingStorage, val entityItemCreator: EntityItemCreator) {
    fun localUserItem(urn: Urn): Maybe<UserItem> {
        return Maybes.zip(userRepository.localUserInfo(urn),
                          followingStorage.isFollowing(urn).toMaybe(),
                          { user, isFollowing -> entityItemCreator.userItem(user, isFollowing) }
        )
    }

    fun userItem(urn: Urn): Maybe<UserItem> {
        return Maybes.zip(userRepository.userInfo(urn),
                          followingStorage.isFollowing(urn).toMaybe(),
                          { user, isFollowing -> entityItemCreator.userItem(user, isFollowing) }
        )
    }

    fun userItem(apiUser: ApiUser): Single<UserItem> {
        return followingStorage.isFollowing(apiUser.urn).map { entityItemCreator.userItem(apiUser, it) }
    }

    fun userItems(apiUsers: Iterable<ApiUser>): Single<List<UserItem>> {
        return Singles.zip(Single.fromCallable { apiUsers.associateBy({ it.urn }, { false }).toMutableMap() },
                            followingStorage.loadFollowings(),
                            { userMap, followings ->
                                followings.forEach { userMap.put(it.userUrn, true) }
                                userMap
                            })
                .map { followingMap -> apiUsers.map { apiUser -> entityItemCreator.userItem(apiUser, followingMap[apiUser.urn] == true) } }
    }

    fun userItemsMap(userUrns: Iterable<Urn>): Single<Map<Urn, UserItem>> {
        return Singles.zip(userRepository.usersInfo(userUrns.toList()), followingStorage.loadFollowings().map { it.associateBy { it.userUrn } },
                           { users, followingsMap ->
                                users.map { user: User -> entityItemCreator.userItem(user, followingsMap.containsKey(user.urn())) }.associateBy { it.urn }
                            })
    }
}
