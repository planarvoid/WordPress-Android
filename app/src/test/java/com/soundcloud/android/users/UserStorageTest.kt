package com.soundcloud.android.users

import com.soundcloud.android.api.model.ApiUser
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.java.optional.Optional.*
import org.junit.Before
import org.junit.Test

class UserStorageTest : StorageIntegrationTest() {

    companion object {
        const val DESCRIPTION = "description"
        const val WEBSITE_URL = "websiteUrl"
        const val WEBSITE_NAME = "websiteTitle"
        const val DISCOGS_NAME = "discogsName"
        const val MYSPACE_NAME = "myspaceName"
    }

    private lateinit var storage: UserStorage

    @Before
    fun setUp() {
        storage = UserStorage(propellerRxV2())
    }

    @Test
    fun loadsUser() {
        val apiUser = testFixtures().insertUser()

        val expectedUser = getApiUserBuilder(apiUser).build()

        storage.loadUser(apiUser.urn)
                .test()
                .assertValue(expectedUser)
    }

    @Test
    fun loadsUserWithoutCountryOrCity() {
        val apiUser = ModelFixtures.create(ApiUser::class.java)
        apiUser.country = null
        apiUser.city = null
        testFixtures().insertUser(apiUser)

        val expectedUser = getBaseUserBuilder(apiUser)
                .city(absent<String>())
                .country(absent<String>())
                .build()

        storage.loadUser(apiUser.urn)
                .test()
                .assertValue(expectedUser)
    }

    @Test
    fun loadsExtendedUser() {
        val apiUser = ModelFixtures.create(ApiUser::class.java)
        testFixtures().insertExtendedUser(apiUser, DESCRIPTION, WEBSITE_URL, WEBSITE_NAME, DISCOGS_NAME, MYSPACE_NAME)

        val expectedUser = getExtendedUserBuilder(apiUser).build()

        storage.loadUser(apiUser.urn)
                .test()
                .assertValue(expectedUser)
    }

    @Test
    fun loadsFollowedUser() {
        val apiUser = testFixtures().insertUser()
        testFixtures().insertFollowing(apiUser.urn)

        val expectedUser = getApiUserBuilder(apiUser)
                .isFollowing(true)
                .build()

        storage.loadUser(apiUser.urn)
                .test()
                .assertValue(expectedUser)
    }

    @Test
    fun loadsUnfollowedUserPending() {
        val apiUser = testFixtures().insertUser()
        testFixtures().insertFollowingPendingRemoval(apiUser.urn, 123)

        val expectedUser = getApiUserBuilder(apiUser).build()

        storage.loadUser(apiUser.urn)
                .test()
                .assertValue(expectedUser)
    }

    @Test
    fun loadUserWithArtistStation() {
        val artistStation = Urn.forArtistStation(123)
        val apiUser = ModelFixtures.create(ApiUser::class.java)
        testFixtures().insertUser(apiUser, artistStation)

        val expectedUser = getApiUserBuilder(apiUser)
                .artistStation(of(artistStation))
                .build()

        storage.loadUser(apiUser.urn)
                .test()
                .assertValue(expectedUser)
    }

    @Test
    fun loadsUrnByPermalink() {
        testFixtures().insertUser()
        val user = testFixtures().insertUser()
        val permalink = user.permalink

        storage.urnForPermalink(permalink)
                .test()
                .assertValue(user.urn)
    }

    @Test
    fun loadsUrnByPermalinkNotFound() {
        testFixtures().insertUser()

        storage.urnForPermalink("testing")
                .test()
                .assertNoValues()
                .assertComplete()
    }

    @Test
    fun loadsProUser() {
        val apiUser = testFixtures().insertProUser("name")

        val expectedUser = getApiUserBuilder(apiUser).build()

        storage.loadUser(apiUser.urn)
                .test()
                .assertValue(expectedUser)
    }

    @Test
    fun loadsMultipleUsers() {
        val firstUser = ModelFixtures.create(ApiUser::class.java)
        testFixtures().insertUser(firstUser)

        val secondUser = ModelFixtures.create(ApiUser::class.java)
        testFixtures().insertUser(secondUser)

        val urns = listOf(firstUser.urn, secondUser.urn)

        val firstApiUser = getApiUserBuilder(firstUser).build()
        val secondApiUser = getApiUserBuilder(secondUser).build()

        storage.loadUsers(urns)
                .test()
                .assertValue(listOf(firstApiUser, secondApiUser))
    }

    @Test
    fun loadsMultipleUsersReturnsEmptyListsOnNoMatches() {
        val urns = listOf(Urn.forUser(42))

        storage.loadUsers(urns)
                .test()
                .assertValue(listOf())
    }

    private fun getExtendedUserBuilder(apiUser: ApiUser): User.Builder {
        return getBaseUserBuilder(apiUser)
                .country(fromNullable(apiUser.country))
                .city(fromNullable(apiUser.city))
                .description(of(DESCRIPTION))
                .websiteUrl(of(WEBSITE_URL))
                .websiteName(of(WEBSITE_NAME))
                .discogsName(of(DISCOGS_NAME))
                .mySpaceName(of(MYSPACE_NAME))
    }

    private fun getApiUserBuilder(apiUser: ApiUser): User.Builder {
        return getBaseUserBuilder(apiUser)
                .country(fromNullable(apiUser.country))
                .city(fromNullable(apiUser.city))
    }

    private fun getBaseUserBuilder(apiUser: ApiUser): User.Builder {
        return ModelFixtures.userBuilder(false)
                .urn(apiUser.urn)
                .username(apiUser.username)
                .signupDate(apiUser.createdAt)
                .firstName(apiUser.firstName)
                .lastName(apiUser.lastName)
                .followersCount(apiUser.followersCount)
                .followingsCount(apiUser.followingsCount)
                .avatarUrl(apiUser.imageUrlTemplate)
                .visualUrl(apiUser.visualUrlTemplate)
                .isPro(apiUser.isPro)
    }

}
