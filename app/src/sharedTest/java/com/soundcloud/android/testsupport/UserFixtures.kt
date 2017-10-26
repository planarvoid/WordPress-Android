package com.soundcloud.android.testsupport
import com.soundcloud.android.api.model.ApiUser
import com.soundcloud.android.model.Urn
import com.soundcloud.android.users.User
import com.soundcloud.android.users.UserItem
import com.soundcloud.java.optional.Optional
import java.util.ArrayList
import java.util.Date

@Suppress("MemberVisibilityCanPrivate")
object UserFixtures {

    private var runningUserId = 1L

    @JvmField val VISUAL_URL_TEMPLATE = "https://i1.sndcdn.com/visuals-${runningUserId}-{size}.jpg"
    @JvmField val AVATAR_URL_TEMPLATE = "https://i1.sndcdn.com/avatars-${runningUserId}-{size}.jpg"
    @JvmField val FOLLOWERS_COUNT = 100
    @JvmField val FOLLOWINGS_COUNT = 200
    @JvmField val CREATED_AT = Date(1476342997)
    @JvmField val FIRST_NAME = "sound"
    @JvmField val LAST_NAME = "cloud"
    @JvmField val COUNTRY = "Country"
    @JvmField val COUNTRY_CODE = "CountryCode"
    @JvmField val CITY = "City"
    @JvmField val TRACK_COUNT = 3

    private val nextUserUrn: Urn get() = Urn.forUser(runningUserId++)

    @JvmStatic
    fun apiUser(): ApiUser = apiUserBuilder(nextUserUrn).build()

    @JvmStatic
    fun apiUser(urn: Urn): ApiUser = apiUserBuilder(urn).build()

    @JvmStatic
    fun apiUserBuilder(): ApiUser.Builder = apiUserBuilder(nextUserUrn)

    @JvmStatic
    fun apiUserBuilder(urn: Urn = nextUserUrn): ApiUser.Builder {
        return ApiUser.builder(urn)
                .followersCount(FOLLOWERS_COUNT)
                .followingsCount(FOLLOWINGS_COUNT)
                .imageUrlTemplate(Optional.of(AVATAR_URL_TEMPLATE))
                .visualUrlTemplate(Optional.of(VISUAL_URL_TEMPLATE))
                .firstName(Optional.of(FIRST_NAME))
                .lastName(Optional.of(LAST_NAME))
                .createdAt(Optional.of(CREATED_AT))
                .permalink(getPermalink(urn))
                .username(getUsername(urn))
                .country(COUNTRY)
                .countryCode(Optional.of(COUNTRY_CODE))
                .city(CITY)
                .trackCount(TRACK_COUNT)
                .verified(true)
                .pro(false)

    }

    @JvmStatic
    fun userBuilder(): User.Builder = User.fromApiUser(apiUser()).toBuilder()

    @JvmStatic
    fun user(urn: Urn): User = userBuilder().urn(urn).build()

    @JvmStatic
    fun userItem(): UserItem = userItem(false)

    @JvmStatic
    fun userItems(count: Int): List<UserItem> {
        val users = ArrayList<UserItem>()
        for (i in 0 until count) {
            users.add(UserFixtures.userItem())
        }
        return users
    }

    @JvmStatic
    fun userItems(apiUsers: List<ApiUser>): List<UserItem> = apiUsers.map { userItem(it) }

    @JvmStatic
    fun userItem(urn: Urn): UserItem = userItem(apiUser(urn), false)

    @JvmStatic
    fun userItem(isFollowing: Boolean): UserItem = userItem(apiUser(), isFollowing)

    @JvmStatic
    fun userItem(user: ApiUser = apiUser()): UserItem = userItem(user(
            user))

    @JvmStatic
    fun userItem(user: ApiUser, isFollowing: Boolean): UserItem = userItem(user(user), isFollowing)

    @JvmStatic
    fun user(user: ApiUser): User = User.fromApiUser(user)

    @JvmStatic
    fun userItem(user: User): UserItem = userItem(user, false)

    @JvmStatic
    fun userItem(user: User, isFollowing: Boolean = false): UserItem = UserItem.from(user, isFollowing)

    @JvmStatic
    fun userItem(urn: Urn, isFollowing: Boolean = false): UserItem = UserItem.from(user(urn), isFollowing)

    private fun getUsername(urn: Urn): String = "user" + urn.numericId

    private fun getPermalink(urn: Urn): String = "user-permalink" + urn.numericId
}
